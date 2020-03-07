package org.apache.maven.surefire.junitcore.pc;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.maven.surefire.junitcore.JUnitCoreParameters;
import org.apache.maven.surefire.report.ConsoleStream;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.internal.DaemonThreadFactory;
import org.junit.internal.runners.ErrorReportingRunner;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

import static org.apache.maven.surefire.junitcore.pc.ParallelComputerUtil.resolveConcurrency;
import static org.apache.maven.surefire.junitcore.pc.SchedulingStrategies.createParallelStrategy;
import static org.apache.maven.surefire.junitcore.pc.SchedulingStrategies.createParallelStrategyUnbounded;
import static org.apache.maven.surefire.junitcore.pc.Type.CLASSES;
import static org.apache.maven.surefire.junitcore.pc.Type.METHODS;
import static org.apache.maven.surefire.junitcore.pc.Type.SUITES;

@SuppressWarnings( { "javadoc", "checkstyle:javadoctype" } )
/*
 * Executing suites, classes and methods with defined concurrency. In this example the threads which completed
 * the suites and classes can be reused in parallel methods.
 * <pre>
 * JUnitCoreParameters parameters = ...;
 * ParallelComputerBuilder builder = new ParallelComputerBuilder(parameters);
 * builder.useOnePool(8).parallelSuites(2).parallelClasses(4).parallelMethods();
 * ParallelComputerBuilder.ParallelComputer computer = builder.buildComputer();
 * Class<?>[] tests = {...};
 * new JUnitCore().run(computer, tests);
 * </pre>
 * Note that the type has always at least one thread even if unspecified. The capacity in
 * {@link ParallelComputerBuilder#useOnePool(int)} must be greater than the number of concurrent suites and classes
 * altogether.
 * <br>
 * The Computer can be stopped in a separate thread. Pending tests will be interrupted if the argument is
 * {@code true}.
 * <pre>
 * computer.describeStopped(true);
 * </pre>
 *
 * @author Tibor Digana (tibor17)
 * @since 2.16
 */
public final class ParallelComputerBuilder
{
    private static final ThreadFactory DAEMON_THREAD_FACTORY = DaemonThreadFactory.newDaemonThreadFactory();

    private static final Class<? extends Annotation> JCIP_NOT_THREAD_SAFE = loadNotThreadSafeAnnotations();

    private static final Set<Runner> NULL_SINGLETON = Collections.singleton( null );

    static final int TOTAL_POOL_SIZE_UNDEFINED = 0;

    private final Map<Type, Integer> parallelGroups = new EnumMap<>( Type.class );

    private final ConsoleStream logger;

    private boolean useSeparatePools;

    private int totalPoolSize;

    private JUnitCoreParameters parameters;

    private boolean optimize;

    private boolean runningInTests;

    /**
     * Calling {@link #useSeparatePools()}.
     * Can be used only in unit tests.
     * Do NOT call this constructor in production.
     */
    ParallelComputerBuilder( ConsoleStream logger )
    {
        this.logger = logger;
        runningInTests = true;
        useSeparatePools();
        parallelGroups.put( SUITES, 0 );
        parallelGroups.put( CLASSES, 0 );
        parallelGroups.put( METHODS, 0 );
    }

    public ParallelComputerBuilder( ConsoleStream logger, JUnitCoreParameters parameters )
    {
        this( logger );
        runningInTests = false;
        this.parameters = parameters;
    }

    public ParallelComputer buildComputer()
    {
        return new PC();
    }

    ParallelComputerBuilder useSeparatePools()
    {
        totalPoolSize = TOTAL_POOL_SIZE_UNDEFINED;
        useSeparatePools = true;
        return this;
    }

    ParallelComputerBuilder useOnePool()
    {
        totalPoolSize = TOTAL_POOL_SIZE_UNDEFINED;
        useSeparatePools = false;
        return this;
    }

    /**
     * @param totalPoolSize Pool size where suites, classes and methods are executed in parallel.
     *                      If the <tt>totalPoolSize</tt> is {@link Integer#MAX_VALUE}, the pool capacity is not
     *                      limited.
     * @throws IllegalArgumentException If <tt>totalPoolSize</tt> is &lt; 1.
     */
    ParallelComputerBuilder useOnePool( int totalPoolSize )
    {
        if ( totalPoolSize < 1 )
        {
            throw new IllegalArgumentException( "Size of common pool is less than 1." );
        }
        this.totalPoolSize = totalPoolSize;
        useSeparatePools = false;
        return this;
    }

    boolean isOptimized()
    {
        return optimize;
    }

    ParallelComputerBuilder optimize( boolean optimize )
    {
        this.optimize = optimize;
        return this;
    }

    ParallelComputerBuilder parallelSuites()
    {
        return parallel( SUITES );
    }

    ParallelComputerBuilder parallelSuites( int nThreads )
    {
        return parallel( nThreads, SUITES );
    }

    ParallelComputerBuilder parallelClasses()
    {
        return parallel( CLASSES );
    }

    ParallelComputerBuilder parallelClasses( int nThreads )
    {
        return parallel( nThreads, CLASSES );
    }

    ParallelComputerBuilder parallelMethods()
    {
        return parallel( METHODS );
    }

    ParallelComputerBuilder parallelMethods( int nThreads )
    {
        return parallel( nThreads, METHODS );
    }

    private ParallelComputerBuilder parallel( int nThreads, Type parallelType )
    {
        if ( nThreads < 0 )
        {
            throw new IllegalArgumentException( "negative nThreads " + nThreads );
        }

        if ( parallelType == null )
        {
            throw new IllegalArgumentException( "null parallelType" );
        }

        parallelGroups.put( parallelType, nThreads );
        return this;
    }

    private ParallelComputerBuilder parallel( Type parallelType )
    {
        return parallel( Integer.MAX_VALUE, parallelType );
    }

    private double parallelTestsTimeoutInSeconds()
    {
        return parameters == null ? 0d : parameters.getParallelTestsTimeoutInSeconds();
    }

    private double parallelTestsTimeoutForcedInSeconds()
    {
        return parameters == null ? 0d : parameters.getParallelTestsTimeoutForcedInSeconds();
    }

    @SuppressWarnings( "unchecked" )
    private static Class<? extends Annotation> loadNotThreadSafeAnnotations()
    {
        try
        {
            Class c = Class.forName( "net.jcip.annotations.NotThreadSafe" );
            return c.isAnnotation() ? (Class<? extends Annotation>) c : null;
        }
        catch ( ClassNotFoundException e )
        {
            return null;
        }
    }

    final class PC
        extends ParallelComputer
    {
        private final SingleThreadScheduler notThreadSafeTests =
            new SingleThreadScheduler( ParallelComputerBuilder.this.logger );

        private final Collection<ParentRunner> suites = new LinkedHashSet<>();

        private final Collection<ParentRunner> nestedSuites = new LinkedHashSet<>();

        private final Collection<ParentRunner> classes = new LinkedHashSet<>();

        private final Collection<ParentRunner> nestedClasses = new LinkedHashSet<>();

        private final Collection<Runner> notParallelRunners = new LinkedHashSet<>();

        private int poolCapacity;

        private boolean splitPool;

        private final Map<Type, Integer> allGroups;

        private long nestedClassesChildren;

        private volatile Scheduler master;

        private PC()
        {
            super( parallelTestsTimeoutInSeconds(), parallelTestsTimeoutForcedInSeconds() );
            allGroups = new EnumMap<>( ParallelComputerBuilder.this.parallelGroups );
            poolCapacity = ParallelComputerBuilder.this.totalPoolSize;
            splitPool = ParallelComputerBuilder.this.useSeparatePools;
        }

        Collection<ParentRunner> getSuites()
        {
            return suites;
        }

        Collection<ParentRunner> getNestedSuites()
        {
            return nestedSuites;
        }

        Collection<ParentRunner> getClasses()
        {
            return classes;
        }

        Collection<ParentRunner> getNestedClasses()
        {
            return nestedClasses;
        }

        Collection<Runner> getNotParallelRunners()
        {
            return notParallelRunners;
        }

        int getPoolCapacity()
        {
            return poolCapacity;
        }

        boolean isSplitPool()
        {
            return splitPool;
        }

        @Override
        protected ShutdownResult describeStopped( boolean shutdownNow )
        {
            ShutdownResult shutdownResult = notThreadSafeTests.describeStopped( shutdownNow );
            final Scheduler m = master;
            if ( m != null )
            {
                ShutdownResult shutdownResultOfMaster = m.describeStopped( shutdownNow );
                shutdownResult.getTriggeredTests().addAll( shutdownResultOfMaster.getTriggeredTests() );
                shutdownResult.getIncompleteTests().addAll( shutdownResultOfMaster.getIncompleteTests() );
            }
            return shutdownResult;
        }

        @Override
        protected boolean shutdownThreadPoolsAwaitingKilled()
        {
            boolean notInterrupted = notThreadSafeTests.shutdownThreadPoolsAwaitingKilled();
            final Scheduler m = master;
            if ( m != null )
            {
                notInterrupted &= m.shutdownThreadPoolsAwaitingKilled();
            }
            return notInterrupted;
        }

        @Override
        public Runner getSuite( RunnerBuilder builder, Class<?>[] cls )
            throws InitializationError
        {
            try
            {
                super.getSuite( builder, cls );
                populateChildrenFromSuites();

                WrappedRunners suiteSuites = wrapRunners( suites );
                WrappedRunners suiteClasses = wrapRunners( classes );

                long suitesCount = suites.size();
                long classesCount = classes.size() + nestedClasses.size();
                long methodsCount = suiteClasses.embeddedChildrenCount + nestedClassesChildren;
                if ( !ParallelComputerBuilder.this.runningInTests )
                {
                    determineThreadCounts( suitesCount, classesCount, methodsCount );
                }

                return setSchedulers( suiteSuites.wrappingSuite, suiteClasses.wrappingSuite );
            }
            catch ( TestSetFailedException e )
            {
                throw new InitializationError( Collections.<Throwable>singletonList( e ) );
            }
        }

        @Override
        protected Runner getRunner( RunnerBuilder builder, Class<?> testClass )
            throws Throwable
        {
            Runner runner = super.getRunner( builder, testClass );
            if ( canSchedule( runner ) )
            {
                if ( !isThreadSafe( runner ) )
                {
                    ( ( ParentRunner ) runner ).setScheduler( notThreadSafeTests.newRunnerScheduler() );
                    notParallelRunners.add( runner );
                }
                else if ( runner instanceof Suite )
                {
                    suites.add( (Suite) runner );
                }
                else
                {
                    classes.add( (ParentRunner) runner );
                }
            }
            else
            {
                notParallelRunners.add( runner );
            }
            return runner;
        }

        private void determineThreadCounts( long suites, long classes, long methods )
            throws TestSetFailedException
        {
            RunnerCounter counts =
                    ParallelComputerBuilder.this.optimize ? new RunnerCounter( suites, classes, methods ) : null;
            Concurrency concurrency =
                    resolveConcurrency( ParallelComputerBuilder.this.parameters, counts );
            allGroups.put( SUITES, concurrency.suites );
            allGroups.put( CLASSES, concurrency.classes );
            allGroups.put( METHODS, concurrency.methods );
            poolCapacity = concurrency.capacity;
            splitPool &= concurrency.capacity <= 0; // fault if negative; should not happen
        }

        private <T extends Runner> WrappedRunners wrapRunners( Collection<T> runners )
            throws InitializationError
        {
            // Do NOT use allGroups here.
            long childrenCounter = 0;
            ArrayList<Runner> runs = new ArrayList<>();
            for ( T runner : runners )
            {
                if ( runner != null )
                {
                    int children = countChildren( runner );
                    childrenCounter += children;
                    runs.add( runner );
                }
            }
            return runs.isEmpty() ? new WrappedRunners() : new WrappedRunners( createSuite( runs ), childrenCounter );
        }

        private int countChildren( Runner runner )
        {
            Description description = runner.getDescription();
            Collection children = description == null ? null : description.getChildren();
            return children == null ? 0 : children.size();
        }

        private ExecutorService createPool( int poolSize )
        {
            return poolSize < Integer.MAX_VALUE
                ? Executors.newFixedThreadPool( poolSize, DAEMON_THREAD_FACTORY )
                : Executors.newCachedThreadPool( DAEMON_THREAD_FACTORY );
        }

        private Scheduler createMaster( ExecutorService pool, int poolSize )
        {
            // can be 0, 1, 2 or 3
            final int finalRunnersCounter = countFinalRunners();

            final SchedulingStrategy strategy;
            if ( finalRunnersCounter <= 1 || poolSize <= 1 )
            {
                strategy = new InvokerStrategy( ParallelComputerBuilder.this.logger );
            }
            else if ( pool != null && poolSize == Integer.MAX_VALUE )
            {
                strategy = new SharedThreadPoolStrategy( ParallelComputerBuilder.this.logger, pool );
            }
            else
            {
                strategy = createParallelStrategy( ParallelComputerBuilder.this.logger, finalRunnersCounter );
            }
            return new Scheduler( ParallelComputerBuilder.this.logger, null, strategy );
        }

        private int countFinalRunners()
        {
            int counter = notParallelRunners.isEmpty() ? 0 : 1;

            if ( !suites.isEmpty() && allGroups.get( SUITES ) > 0 )
            {
                ++counter;
            }

            if ( !classes.isEmpty() && allGroups.get( CLASSES ) > 0 )
            {
                ++counter;
            }

            return counter;
        }

        private void populateChildrenFromSuites()
        {
            // Do NOT use allGroups here.
            Filter filter = new SuiteFilter();
            for ( Iterator<ParentRunner> it = suites.iterator(); it.hasNext(); )
            {
                ParentRunner suite = it.next();
                try
                {
                    suite.filter( filter );
                }
                catch ( NoTestsRemainException e )
                {
                    it.remove();
                }
            }
        }

        private int totalPoolSize()
        {
            if ( poolCapacity == TOTAL_POOL_SIZE_UNDEFINED )
            {
                int total = 0;
                for ( int nThreads : allGroups.values() )
                {
                    total += nThreads;
                    if ( total < 0 )
                    {
                        total = Integer.MAX_VALUE;
                        break;
                    }
                }
                return total;
            }
            else
            {
                return poolCapacity;
            }
        }

        private Runner setSchedulers( ParentRunner suiteSuites, ParentRunner suiteClasses )
            throws InitializationError
        {
            int parallelSuites = allGroups.get( SUITES );
            int parallelClasses = allGroups.get( CLASSES );
            int parallelMethods = allGroups.get( METHODS );
            int poolSize = totalPoolSize();
            ExecutorService commonPool = splitPool || poolSize == 0 ? null : createPool( poolSize );
            master = createMaster( commonPool, poolSize );

            if ( suiteSuites != null )
            {
                // a scheduler for parallel suites
                if ( commonPool != null && parallelSuites > 0 )
                {
                    Balancer balancer = BalancerFactory.createBalancerWithFairness( parallelSuites );
                    suiteSuites.setScheduler( createScheduler( null, commonPool, true, balancer ) );
                }
                else
                {
                    suiteSuites.setScheduler( createScheduler( parallelSuites ) );
                }
            }

            // schedulers for parallel classes
            ArrayList<ParentRunner> allSuites = new ArrayList<>( suites );
            allSuites.addAll( nestedSuites );
            if ( suiteClasses != null )
            {
                allSuites.add( suiteClasses );
            }
            if ( !allSuites.isEmpty() )
            {
                setSchedulers( allSuites, parallelClasses, commonPool );
            }

            // schedulers for parallel methods
            ArrayList<ParentRunner> allClasses = new ArrayList<>( classes );
            allClasses.addAll( nestedClasses );
            if ( !allClasses.isEmpty() )
            {
                setSchedulers( allClasses, parallelMethods, commonPool );
            }

            // resulting runner for Computer#getSuite() scheduled by master scheduler
            ParentRunner all = createFinalRunner( removeNullRunners(
                Arrays.<Runner>asList( suiteSuites, suiteClasses, createSuite( notParallelRunners ) )
            ) );
            all.setScheduler( master );
            return all;
        }

        private ParentRunner createFinalRunner( List<Runner> runners )
            throws InitializationError
        {
            return new Suite( null, runners )
            {
                @Override
                public void run( RunNotifier notifier )
                {
                    try
                    {
                        beforeRunQuietly();
                        super.run( notifier );
                    }
                    finally
                    {
                        afterRunQuietly();
                    }
                }
            };
        }

        private void setSchedulers( Iterable<? extends ParentRunner> runners, int poolSize, ExecutorService commonPool )
        {
            if ( commonPool != null )
            {
                Balancer concurrencyLimit = BalancerFactory.createBalancerWithFairness( poolSize );
                boolean doParallel = poolSize > 0;
                for ( ParentRunner runner : runners )
                {
                    runner.setScheduler(
                        createScheduler( runner.getDescription(), commonPool, doParallel, concurrencyLimit ) );
                }
            }
            else
            {
                ExecutorService pool = null;
                if ( poolSize == Integer.MAX_VALUE )
                {
                    pool = Executors.newCachedThreadPool( DAEMON_THREAD_FACTORY );
                }
                else if ( poolSize > 0 )
                {
                    pool = Executors.newFixedThreadPool( poolSize, DAEMON_THREAD_FACTORY );
                }
                boolean doParallel = pool != null;
                for ( ParentRunner runner : runners )
                {
                    runner.setScheduler( createScheduler( runner.getDescription(), pool, doParallel,
                                                          BalancerFactory.createInfinitePermitsBalancer() ) );
                }
            }
        }

        private Scheduler createScheduler( Description desc, ExecutorService pool, boolean doParallel,
                                           Balancer concurrency )
        {
            SchedulingStrategy strategy =
                    doParallel & pool != null
                    ? new SharedThreadPoolStrategy( ParallelComputerBuilder.this.logger, pool )
                    : new InvokerStrategy( ParallelComputerBuilder.this.logger );
            return new Scheduler( ParallelComputerBuilder.this.logger, desc, master, strategy, concurrency );
        }

        private Scheduler createScheduler( int poolSize )
        {
            final SchedulingStrategy strategy;
            if ( poolSize == Integer.MAX_VALUE )
            {
                strategy = createParallelStrategyUnbounded( ParallelComputerBuilder.this.logger );
            }
            else if ( poolSize == 0 )
            {
                strategy = new InvokerStrategy( ParallelComputerBuilder.this.logger );
            }
            else
            {
                strategy = createParallelStrategy( ParallelComputerBuilder.this.logger, poolSize );
            }
            return new Scheduler( ParallelComputerBuilder.this.logger, null, master, strategy );
        }

        private boolean canSchedule( Runner runner )
        {
            return !( runner instanceof ErrorReportingRunner ) && runner instanceof ParentRunner;
        }

        private boolean isThreadSafe( Runner runner )
        {
            return runner.getDescription().getAnnotation( JCIP_NOT_THREAD_SAFE ) == null;
        }

        private class SuiteFilter
            extends Filter
        {
            // Do NOT use allGroups in SuiteFilter.

            @Override
            public boolean shouldRun( Description description )
            {
                return true;
            }

            @Override
            public void apply( Object child )
                throws NoTestsRemainException
            {
                super.apply( child );
                if ( child instanceof ParentRunner )
                {
                    ParentRunner runner = ( ParentRunner ) child;
                    if ( !isThreadSafe( runner ) )
                    {
                        runner.setScheduler( notThreadSafeTests.newRunnerScheduler() );
                    }
                    else if ( child instanceof Suite )
                    {
                        nestedSuites.add( (Suite) child );
                    }
                    else
                    {
                        ParentRunner parentRunner = (ParentRunner) child;
                        nestedClasses.add( parentRunner );
                        nestedClassesChildren += parentRunner.getDescription().getChildren().size();
                    }
                }
            }

            @Override
            public String describe()
            {
                return "";
            }
        }
    }

    private static Suite createSuite( Collection<Runner> runners )
        throws InitializationError
    {
        final List<Runner> onlyRunners = removeNullRunners( runners );
        return onlyRunners.isEmpty() ? null : new Suite( null, onlyRunners )
        {
        };
    }

    private static List<Runner> removeNullRunners( Collection<Runner> runners )
    {
        final List<Runner> onlyRunners = new ArrayList<>( runners );
        onlyRunners.removeAll( NULL_SINGLETON );
        return onlyRunners;
    }
}
