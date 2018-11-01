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

import org.apache.maven.surefire.report.ConsoleStream;
import org.junit.runner.Description;
import org.junit.runners.model.RunnerScheduler;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Schedules tests, controls thread resources, awaiting tests and other schedulers finished, and
 * a master scheduler can shutdown slaves.
 * <br>
 * The scheduler objects should be first created (and wired) and set in runners
 * {@link org.junit.runners.ParentRunner#setScheduler(org.junit.runners.model.RunnerScheduler)}.
 * <br>
 * A new instance of scheduling strategy should be passed to the constructor of this scheduler.
 *
 * @author Tibor Digana (tibor17)
 * @since 2.16
 */
public class Scheduler
    implements RunnerScheduler
{
    private final Balancer balancer;

    private final SchedulingStrategy strategy;

    private final Set<Controller> slaves = new CopyOnWriteArraySet<>();

    private final Description description;

    private final ConsoleStream logger;

    private volatile boolean shutdown = false;

    private volatile boolean started = false;

    private volatile boolean finished = false;

    private volatile Controller masterController;

    /**
     * Use e.g. parallel classes have own non-shared thread pool, and methods another pool.
     * <br>
     * You can use it with one infinite thread pool shared in strategies across all
     * suites, class runners, etc.
     *
     * @param logger          console logger
     * @param description     JUnit description of class
     * @param strategy        scheduling strategy
     */
    public Scheduler( ConsoleStream logger, Description description, SchedulingStrategy strategy )
    {
        this( logger, description, strategy, -1 );
    }

    /**
     * Should be used if schedulers in parallel children and parent use one instance of bounded thread pool.
     * <br>
     * Set this scheduler in a e.g. one suite of classes, then every individual class runner should reference
     * {@link #Scheduler(ConsoleStream, org.junit.runner.Description, Scheduler, SchedulingStrategy)}
     * or {@link #Scheduler(ConsoleStream, org.junit.runner.Description, Scheduler, SchedulingStrategy, int)}.
     *
     * @param logger current logger implementation
     * @param description description of current runner
     * @param strategy    scheduling strategy with a shared thread pool
     * @param concurrency determines maximum concurrent children scheduled a time via {@link #schedule(Runnable)}
     * @throws NullPointerException if null <tt>strategy</tt>
     */
    public Scheduler( ConsoleStream logger, Description description, SchedulingStrategy strategy, int concurrency )
    {
        this( logger, description, strategy, BalancerFactory.createBalancer( concurrency ) );
    }

    /**
     * New instances should be used by schedulers with limited concurrency by <tt>balancer</tt>
     * against other groups of schedulers. The schedulers share one pool.
     * <br>
     * Unlike in {@link #Scheduler(ConsoleStream, org.junit.runner.Description, SchedulingStrategy, int)} which was
     * limiting the <tt>concurrency</tt> of children of a runner where this scheduler was set, {@code this}
     * <tt>balancer</tt> is limiting the concurrency of all children in runners having schedulers created by this
     * constructor.
     *
     * @param logger current logger implementation
     * @param description description of current runner
     * @param strategy    scheduling strategy which may share threads with other strategy
     * @param balancer    determines maximum concurrent children scheduled a time via {@link #schedule(Runnable)}
     * @throws NullPointerException if null <tt>strategy</tt> or <tt>balancer</tt>
     */
    public Scheduler( ConsoleStream logger, Description description, SchedulingStrategy strategy, Balancer balancer )
    {
        strategy.setDefaultShutdownHandler( newShutdownHandler() );
        this.logger = logger;
        this.description = description;
        this.strategy = strategy;
        this.balancer = balancer;
        masterController = null;
    }

    /**
     * Can be used by e.g. a runner having parallel classes in use case with parallel
     * suites, classes and methods sharing the same thread pool.
     *
     * @param logger current logger implementation
     * @param description     description of current runner
     * @param masterScheduler scheduler sharing own threads with this slave
     * @param strategy        scheduling strategy for this scheduler
     * @param balancer        determines maximum concurrent children scheduled a time via {@link #schedule(Runnable)}
     * @throws NullPointerException if null <tt>masterScheduler</tt>, <tt>strategy</tt> or <tt>balancer</tt>
     */
    public Scheduler( ConsoleStream logger, Description description, Scheduler masterScheduler,
                      SchedulingStrategy strategy, Balancer balancer )
    {
        this( logger, description, strategy, balancer );
        strategy.setDefaultShutdownHandler( newShutdownHandler() );
        masterScheduler.register( this );
    }

    /**
     * @param logger          console logger
     * @param description     JUnit description of class
     * @param masterScheduler a reference to
     * {@link #Scheduler(ConsoleStream, org.junit.runner.Description, SchedulingStrategy, int)}
     *                        or {@link #Scheduler(ConsoleStream, org.junit.runner.Description, SchedulingStrategy)}
     * @param strategy        scheduling strategy
     * @param concurrency     determines maximum concurrent children scheduled a time via {@link #schedule(Runnable)}
     *
     * @see #Scheduler(ConsoleStream, org.junit.runner.Description, SchedulingStrategy)
     * @see #Scheduler(ConsoleStream, org.junit.runner.Description, SchedulingStrategy, int)
     */
    public Scheduler( ConsoleStream logger, Description description, Scheduler masterScheduler,
                      SchedulingStrategy strategy, int concurrency )
    {
        this( logger, description, strategy, concurrency );
        strategy.setDefaultShutdownHandler( newShutdownHandler() );
        masterScheduler.register( this );
    }

    /**
     * Should be used with individual pools on suites, classes and methods, see
     * {@link org.apache.maven.surefire.junitcore.pc.ParallelComputerBuilder#useSeparatePools()}.
     * <br>
     * Cached thread pool is infinite and can be always shared.
     *
     * @param logger          console logger
     * @param description     JUnit description of class
     * @param masterScheduler parent scheduler
     * @param strategy        scheduling strategy
     */
    public Scheduler( ConsoleStream logger, Description description, Scheduler masterScheduler,
                      SchedulingStrategy strategy )
    {
        this( logger, description, masterScheduler, strategy, 0 );
    }

    private void setController( Controller masterController )
    {
        if ( masterController == null )
        {
            throw new NullPointerException( "null ExecutionController" );
        }
        this.masterController = masterController;
    }

    /**
     * @param slave a slave scheduler to register
     * @return {@code true} if successfully registered the <tt>slave</tt>.
     */
    private boolean register( Scheduler slave )
    {
        boolean canRegister = slave != null && slave != this;
        if ( canRegister )
        {
            Controller controller = new Controller( slave );
            canRegister = !slaves.contains( controller );
            if ( canRegister )
            {
                slaves.add( controller );
                slave.setController( controller );
            }
        }
        return canRegister;
    }

    /**
     * @return {@code true} if new tasks can be scheduled.
     */
    private boolean canSchedule()
    {
        return !shutdown && ( masterController == null || masterController.canSchedule() );
    }

    protected void logQuietly( Throwable t )
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try ( PrintStream stream = new PrintStream( out ) )
        {
            t.printStackTrace( stream );
        }
        logger.println( out.toString() );
    }

    protected void logQuietly( String msg )
    {
        logger.println( msg );
    }

    /**
     * Attempts to stop all actively executing tasks and immediately returns a collection
     * of descriptions of those tasks which have started prior to this call.
     * <br>
     * This scheduler and other registered schedulers will stop, see {@link #register(Scheduler)}.
     * If <tt>shutdownNow</tt> is set, waiting methods will be interrupted via {@link Thread#interrupt}.
     *
     * @param stopNow if {@code true} interrupts waiting test methods
     * @return collection of descriptions started before shutting down
     */
    protected ShutdownResult describeStopped( boolean stopNow )
    {
        Collection<Description> executedTests = new ConcurrentLinkedQueue<>();
        Collection<Description> incompleteTests = new ConcurrentLinkedQueue<>();
        stop( executedTests, incompleteTests, false, stopNow );
        return new ShutdownResult( executedTests, incompleteTests );
    }

    /**
     * Stop/Shutdown/Interrupt scheduler and its children (if any).
     *
     * @param executedTests       Started tests which have finished normally or abruptly till called this method.
     * @param incompleteTests     Started tests which have finished incomplete due to shutdown.
     * @param tryCancelFutures    Useful to set to {@code false} if a timeout is specified in plugin config.
     *                            When the runner of
     *                            {@link ParallelComputer#getSuite(org.junit.runners.model.RunnerBuilder, Class[])}
     *                            is finished in
     *                            {@link org.junit.runners.Suite#run(org.junit.runner.notification.RunNotifier)}
     *                            all the thread-pools created by {@link ParallelComputerBuilder.PC} are already dead.
     *                            See the unit test {@code ParallelComputerBuilder#timeoutAndForcedShutdown()}.
     * @param stopNow             Interrupting tests by {@link java.util.concurrent.ExecutorService#shutdownNow()} or
     *                            {@link java.util.concurrent.Future#cancel(boolean) Future#cancel(true)} or
     *                            {@link Thread#interrupt()}.
     */
    private void stop( Collection<Description> executedTests, Collection<Description> incompleteTests,
                       boolean tryCancelFutures, boolean stopNow )
    {
        shutdown = true;
        try
        {
            if ( started && !ParallelComputerUtil.isUnusedDescription( description ) )
            {
                if ( executedTests != null )
                {
                    executedTests.add( description );
                }

                if ( incompleteTests != null && !finished )
                {
                    incompleteTests.add( description );
                }
            }

            for ( Controller slave : slaves )
            {
                slave.stop( executedTests, incompleteTests, tryCancelFutures, stopNow );
            }
        }
        finally
        {
            try
            {
                balancer.releaseAllPermits();
            }
            finally
            {
                if ( stopNow )
                {
                    strategy.stopNow();
                }
                else if ( tryCancelFutures )
                {
                    strategy.stop();
                }
                else
                {
                    strategy.disable();
                }
            }
        }
    }

    protected boolean shutdownThreadPoolsAwaitingKilled()
    {
        if ( masterController == null )
        {
            stop( null, null, true, false );
            boolean isNotInterrupted = true;
            if ( strategy != null )
            {
                isNotInterrupted = strategy.destroy();
            }
            for ( Controller slave : slaves )
            {
                isNotInterrupted &= slave.destroy();
            }
            return isNotInterrupted;
        }
        else
        {
            throw new UnsupportedOperationException( "cannot call this method if this is not a master scheduler" );
        }
    }

    protected void beforeExecute()
    {
    }

    protected void afterExecute()
    {
    }

    @Override
    public void schedule( Runnable childStatement )
    {
        if ( childStatement == null )
        {
            logQuietly( "cannot schedule null" );
        }
        else if ( canSchedule() && strategy.canSchedule() )
        {
            try
            {
                boolean isNotInterrupted = balancer.acquirePermit();
                if ( isNotInterrupted && !shutdown )
                {
                    Runnable task = wrapTask( childStatement );
                    strategy.schedule( task );
                    started = true;
                }
            }
            catch ( RejectedExecutionException e )
            {
                stop( null, null, true, false );
            }
            catch ( Throwable t )
            {
                balancer.releasePermit();
                logQuietly( t );
            }
        }
    }

    @Override
    public void finished()
    {
        try
        {
            strategy.finished();
        }
        catch ( InterruptedException e )
        {
            logQuietly( e );
        }
        finally
        {
            finished = true;
        }
    }

    private Runnable wrapTask( final Runnable task )
    {
        return new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    beforeExecute();
                    task.run();
                }
                finally
                {
                    try
                    {
                        afterExecute();
                    }
                    finally
                    {
                        balancer.releasePermit();
                    }
                }
            }
        };
    }

    protected ShutdownHandler newShutdownHandler()
    {
        return new ShutdownHandler();
    }

    /**
     * If this is a master scheduler, the slaves can stop scheduling by the master through the controller.
     */
    private final class Controller
    {
        private final Scheduler slave;

        private Controller( Scheduler slave )
        {
            this.slave = slave;
        }

        /**
         * @return {@code true} if new children can be scheduled.
         */
        boolean canSchedule()
        {
            return Scheduler.this.canSchedule();
        }

        void stop( Collection<Description> executedTests, Collection<Description> incompleteTests,
                   boolean tryCancelFutures, boolean shutdownNow )
        {
            slave.stop( executedTests, incompleteTests, tryCancelFutures, shutdownNow );
        }

        /**
         * @see org.apache.maven.surefire.junitcore.pc.Destroyable#destroy()
         */
        boolean destroy()
        {
            return slave.strategy.destroy();
        }

        @Override
        public int hashCode()
        {
            return slave.hashCode();
        }

        @Override
        public boolean equals( Object o )
        {
            return o == this || ( o instanceof Controller ) && slave.equals( ( (Controller) o ).slave );
        }
    }

    /**
     * There is a way to shutdown the hierarchy of schedulers. You can do it in master scheduler via
     * {@link #shutdownThreadPoolsAwaitingKilled()} which kills the current master and children recursively.
     * If alternatively a shared {@link java.util.concurrent.ExecutorService} used by the master and children
     * schedulers is shutdown from outside, then the {@link ShutdownHandler} is a hook calling current
     * {@link #describeStopped(boolean)}. The method {@link #describeStopped(boolean)} is again shutting down children
     * schedulers recursively as well.
     */
    public class ShutdownHandler
        implements RejectedExecutionHandler
    {
        private volatile RejectedExecutionHandler poolHandler;

        protected ShutdownHandler()
        {
            poolHandler = null;
        }

        public void setRejectedExecutionHandler( RejectedExecutionHandler poolHandler )
        {
            this.poolHandler = poolHandler;
        }

        @Override
        public void rejectedExecution( Runnable r, ThreadPoolExecutor executor )
        {
            if ( executor.isShutdown() )
            {
                Scheduler.this.stop( null, null, true, false );
            }
            final RejectedExecutionHandler poolHandler = this.poolHandler;
            if ( poolHandler != null )
            {
                poolHandler.rejectedExecution( r, executor );
            }
        }
    }
}
