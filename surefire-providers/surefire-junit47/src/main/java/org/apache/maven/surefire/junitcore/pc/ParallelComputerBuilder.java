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

import org.apache.maven.surefire.junitcore.JUnitCoreParameters;
import org.apache.maven.surefire.testset.TestSetFailedException;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.apache.maven.surefire.junitcore.pc.ParallelComputerUtil.*;
import static org.apache.maven.surefire.junitcore.pc.Type.*;

/**
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
 * <p/>
 * The Computer can be shutdown in a separate thread. Pending tests will be interrupted if the argument is
 * <tt>true</tt>.
 * <pre>
 * computer.shutdown(true);
 * </pre>
 *
 * @author Tibor Digana (tibor17)
 * @since 2.16
 */
public final class ParallelComputerBuilder
{
    static final int TOTAL_POOL_SIZE_UNDEFINED = 0;

    private final Map<Type, Integer> parallelGroups = new EnumMap<Type, Integer>( Type.class );

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
    ParallelComputerBuilder()
    {
        runningInTests = true;
        useSeparatePools();
        parallelGroups.put( SUITES, 0 );
        parallelGroups.put( CLASSES, 0 );
        parallelGroups.put( METHODS, 0 );
    }

    public ParallelComputerBuilder( JUnitCoreParameters parameters )
    {
        this();
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
            throw new NullPointerException( "null parallelType" );
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

    final class PC
        extends ParallelComputer
    {
        final Collection<ParentRunner> suites = new LinkedHashSet<ParentRunner>();

        final Collection<ParentRunner> nestedSuites = new LinkedHashSet<ParentRunner>();

        final Collection<ParentRunner> classes = new LinkedHashSet<ParentRunner>();

        final Collection<ParentRunner> nestedClasses = new LinkedHashSet<ParentRunner>();

        final Collection<Runner> unscheduledRunners = new LinkedHashSet<Runner>();

        int poolCapacity;

        boolean splitPool;

        private final Map<Type, Integer> allGroups;

        private long nestedClassesChildren;

        private volatile Scheduler master;

        private PC()
        {
            super( parallelTestsTimeoutInSeconds(), parallelTestsTimeoutForcedInSeconds() );
            allGroups = new EnumMap<Type, Integer>( ParallelComputerBuilder.this.parallelGroups );
            poolCapacity = ParallelComputerBuilder.this.totalPoolSize;
            splitPool = ParallelComputerBuilder.this.useSeparatePools;
        }

        @Override
        public Collection<Description> shutdown( boolean shutdownNow )
        {
            final Scheduler master = this.master;
            return master == null ? Collections.<Description>emptyList() : master.shutdown( shutdownNow );
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
                if (!ParallelComputerBuilder.this.runningInTests)
                {
                    determineThreadCounts( suitesCount, classesCount, methodsCount );
                }

                return setSchedulers( suiteSuites.wrappingSuite, suiteClasses.wrappingSuite );
            }
            catch ( TestSetFailedException e )
            {
                throw new InitializationError( e );
            }
        }

        @Override
        protected Runner getRunner( RunnerBuilder builder, Class<?> testClass )
            throws Throwable
        {
            Runner runner = super.getRunner( builder, testClass );
            if ( canSchedule( runner ) )
            {
                if ( runner instanceof Suite )
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
                unscheduledRunners.add( runner );
            }
            return runner;
        }

        private void determineThreadCounts( long suites, long classes, long methods )
            throws TestSetFailedException
        {
            final JUnitCoreParameters parameters = ParallelComputerBuilder.this.parameters;
            final boolean optimize = ParallelComputerBuilder.this.optimize;
            RunnerCounter counts = new RunnerCounter( suites, classes, methods );
            Concurrency concurrency = resolveConcurrency( parameters, optimize ? counts : null );
            allGroups.put( SUITES, concurrency.suites );
            allGroups.put( CLASSES, concurrency.classes );
            allGroups.put( METHODS, concurrency.methods );
            poolCapacity = concurrency.capacity;
            splitPool &= concurrency.capacity <= 0;//fault if negative; should not happen
        }

        private <T extends Runner> WrappedRunners wrapRunners( Collection<T> runners )
            throws InitializationError
        {
            // Do NOT use allGroups here.
            long childrenCounter = 0;
            ArrayList<Runner> runs = new ArrayList<Runner>();
            for ( T runner : runners )
            {
                if ( runner != null )
                {
                    int children = countChildren( runner );
                    childrenCounter += children;
                    if ( children != 0 )
                    {
                        runs.add( runner );
                    }
                }
            }

            Suite wrapper = runs.isEmpty() ? null : new Suite( null, runs )
            {
            };
            return new WrappedRunners( wrapper, childrenCounter );
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
                ? Executors.newFixedThreadPool( poolSize )
                : Executors.newCachedThreadPool();
        }

        private Scheduler createMaster( ExecutorService pool, int poolSize )
        {
            if ( !areSuitesAndClassesParallel() || poolSize <= 1 )
            {
                return new Scheduler( null, new InvokerStrategy() );
            }
            else if ( pool != null && poolSize == Integer.MAX_VALUE )
            {
                return new Scheduler( null, new SharedThreadPoolStrategy( pool ) );
            }
            else
            {
                return new Scheduler( null, SchedulingStrategies.createParallelStrategy( 2 ) );
            }
        }

        private boolean areSuitesAndClassesParallel()
        {
            return !suites.isEmpty() && allGroups.get( SUITES ) > 0 && !classes.isEmpty()
                && allGroups.get( CLASSES ) > 0;
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
            ArrayList<ParentRunner> allSuites = new ArrayList<ParentRunner>( suites );
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
            ArrayList<ParentRunner> allClasses = new ArrayList<ParentRunner>( classes );
            allClasses.addAll( nestedClasses );
            if ( !allClasses.isEmpty() )
            {
                setSchedulers( allClasses, parallelMethods, commonPool );
            }

            // resulting runner for Computer#getSuite() scheduled by master scheduler
            ParentRunner all = createFinalRunner( suiteSuites, suiteClasses );
            all.setScheduler( master );
            return all;
        }

        private ParentRunner createFinalRunner( Runner... runners )
            throws InitializationError
        {
            ArrayList<Runner> all = new ArrayList<Runner>( unscheduledRunners );
            for ( Runner runner : runners )
            {
                if ( runner != null )
                {
                    all.add( runner );
                }
            }

            return new Suite( null, all )
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
                    pool = Executors.newCachedThreadPool();
                }
                else if ( poolSize > 0 )
                {
                    pool = Executors.newFixedThreadPool( poolSize );
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
            doParallel &= pool != null;
            SchedulingStrategy strategy = doParallel ? new SharedThreadPoolStrategy( pool ) : new InvokerStrategy();
            return new Scheduler( desc, master, strategy, concurrency );
        }

        private Scheduler createScheduler( int poolSize )
        {
            if ( poolSize == Integer.MAX_VALUE )
            {
                return new Scheduler( null, master, SchedulingStrategies.createParallelStrategyUnbounded() );
            }
            else if ( poolSize == 0 )
            {
                return new Scheduler( null, master, new InvokerStrategy() );
            }
            else
            {
                return new Scheduler( null, master, SchedulingStrategies.createParallelStrategy( poolSize ) );
            }
        }

        private boolean canSchedule( Runner runner )
        {
            return !( runner instanceof ErrorReportingRunner ) && runner instanceof ParentRunner;
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
                if ( child instanceof Suite )
                {
                    nestedSuites.add( (Suite) child );
                }
                else if ( child instanceof ParentRunner )
                {
                    ParentRunner parentRunner = (ParentRunner) child;
                    nestedClasses.add( parentRunner );
                    nestedClassesChildren += parentRunner.getDescription().getChildren().size();
                }
            }

            @Override
            public String describe()
            {
                return "";
            }
        }
    }
}
