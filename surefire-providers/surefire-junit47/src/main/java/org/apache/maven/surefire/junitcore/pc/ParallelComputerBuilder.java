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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Executing suites, classes and methods with defined concurrency. In this example the threads which completed
 * the suites and classes can be reused in parallel methods.
 * <pre>
 * ParallelComputerBuilder builder = new ParallelComputerBuilder();
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
public class ParallelComputerBuilder
{
    static final int TOTAL_POOL_SIZE_UNDEFINED = 0;

    private final Map<Type, Integer> parallelGroups = new HashMap<Type, Integer>( 3 );

    private boolean useSeparatePools;

    private int totalPoolSize;

    /**
     * Calling {@link #useSeparatePools()}.
     */
    public ParallelComputerBuilder()
    {
        useSeparatePools();
        parallelGroups.put( Type.SUITES, 0 );
        parallelGroups.put( Type.CLASSES, 0 );
        parallelGroups.put( Type.METHODS, 0 );
    }

    public ParallelComputerBuilder useSeparatePools()
    {
        totalPoolSize = TOTAL_POOL_SIZE_UNDEFINED;
        useSeparatePools = true;
        return this;
    }

    public ParallelComputerBuilder useOnePool()
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
    public ParallelComputerBuilder useOnePool( int totalPoolSize )
    {
        if ( totalPoolSize < 1 )
        {
            throw new IllegalArgumentException( "Size of common pool is less than 1." );
        }
        this.totalPoolSize = totalPoolSize;
        useSeparatePools = false;
        return this;
    }

    public ParallelComputerBuilder parallelSuites()
    {
        return parallel( Type.SUITES );
    }

    public ParallelComputerBuilder parallelSuites( int nThreads )
    {
        return parallel( nThreads, Type.SUITES );
    }

    public ParallelComputerBuilder parallelClasses()
    {
        return parallel( Type.CLASSES );
    }

    public ParallelComputerBuilder parallelClasses( int nThreads )
    {
        return parallel( nThreads, Type.CLASSES );
    }

    public ParallelComputerBuilder parallelMethods()
    {
        return parallel( Type.METHODS );
    }

    public ParallelComputerBuilder parallelMethods( int nThreads )
    {
        return parallel( nThreads, Type.METHODS );
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

    public ParallelComputer buildComputer()
    {
        return buildComputer( 0, 0, TimeUnit.NANOSECONDS );
    }

    public ParallelComputer buildComputer( long timeout, long timeoutForced, TimeUnit timeUnit )
    {
        return new PC( timeout, timeoutForced, timeUnit );
    }

    private static enum Type
    {
        SUITES,
        CLASSES,
        METHODS
    }

    final class PC
        extends ParallelComputer
    {
        final Collection<ParentRunner> suites = new LinkedHashSet<ParentRunner>();

        final Collection<ParentRunner> nestedSuites = new LinkedHashSet<ParentRunner>();

        final Collection<ParentRunner> classes = new LinkedHashSet<ParentRunner>();

        final Collection<ParentRunner> nestedClasses = new LinkedHashSet<ParentRunner>();

        final Collection<Runner> unscheduledRunners = new LinkedHashSet<Runner>();

        final int poolCapacity;

        final boolean splitPool;

        private final Map<Type, Integer> allGroups;

        private volatile Scheduler master;

        private PC( long timeout, long timeoutForced, TimeUnit timeoutUnit )
        {
            super( timeout, timeoutForced, timeoutUnit );
            allGroups = new HashMap<Type, Integer>( ParallelComputerBuilder.this.parallelGroups );
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
            super.getSuite( builder, cls );
            populateChildrenFromSuites();
            return setSchedulers();
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

        private <T extends Runner> ParentRunner wrapRunners( Collection<T> runners )
            throws InitializationError
        {
            ArrayList<Runner> runs = new ArrayList<Runner>();
            for ( T runner : runners )
            {
                if ( runner != null && hasChildren( runner ) )
                {
                    runs.add( runner );
                }
            }

            return runs.isEmpty() ? null : new Suite( null, runs )
            {
            };
        }

        private boolean hasChildren( Runner runner )
        {
            Description description = runner.getDescription();
            Collection children = description == null ? null : description.getChildren();
            return children != null && !children.isEmpty();
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
            return !suites.isEmpty() && allGroups.get( Type.SUITES ) > 0 && !classes.isEmpty()
                && allGroups.get( Type.CLASSES ) > 0;
        }

        private void populateChildrenFromSuites()
        {
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

        private Runner setSchedulers()
            throws InitializationError
        {
            int parallelSuites = allGroups.get( Type.SUITES );
            int parallelClasses = allGroups.get( Type.CLASSES );
            int parallelMethods = allGroups.get( Type.METHODS );
            int poolSize = totalPoolSize();
            ExecutorService commonPool = splitPool || poolSize == 0 ? null : createPool( poolSize );
            master = createMaster( commonPool, poolSize );

            ParentRunner suiteSuites = wrapRunners( suites );
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
            ParentRunner suiteClasses = wrapRunners( classes );
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
                    nestedClasses.add( (ParentRunner) child );
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
