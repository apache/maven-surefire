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

import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.internal.DaemonThreadFactory;
import org.junit.runner.Computer;
import org.junit.runner.Description;

import java.util.Collection;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * ParallelComputer extends JUnit {@link Computer} and has a shutdown functionality.
 *
 * @author Tibor Digana (tibor17)
 * @see ParallelComputerBuilder
 * @since 2.16
 */
public abstract class ParallelComputer
    extends Computer
{
    private static final ThreadFactory DAEMON_THREAD_FACTORY = DaemonThreadFactory.newDaemonThreadFactory();

    private static final double NANOS_IN_A_SECOND = 1E9;

    private final ShutdownStatus shutdownStatus = new ShutdownStatus();

    private final ShutdownStatus forcedShutdownStatus = new ShutdownStatus();

    private final long timeoutNanos;

    private final long timeoutForcedNanos;

    private ScheduledExecutorService shutdownScheduler;

    public ParallelComputer( double timeoutInSeconds, double timeoutForcedInSeconds )
    {
        this.timeoutNanos = secondsToNanos( timeoutInSeconds );
        this.timeoutForcedNanos = secondsToNanos( timeoutForcedInSeconds );
    }

    protected abstract ShutdownResult describeStopped( boolean shutdownNow );

    protected abstract boolean shutdownThreadPoolsAwaitingKilled();

    protected final void beforeRunQuietly()
    {
        shutdownStatus.setDescriptionsBeforeShutdown( hasTimeout() ? scheduleShutdown() : null );
        forcedShutdownStatus.setDescriptionsBeforeShutdown( hasTimeoutForced() ? scheduleForcedShutdown() : null );
    }

    protected final boolean afterRunQuietly()
    {
        shutdownStatus.tryFinish();
        forcedShutdownStatus.tryFinish();
        boolean notInterrupted = true;
        if ( shutdownScheduler != null )
        {
            shutdownScheduler.shutdownNow();
            /**
             * Clear <i>interrupted status</i> of the (main) Thread.
             * Could be previously interrupted by {@link InvokerStrategy} after triggering immediate shutdown.
             */
            Thread.interrupted();
            try
            {
                shutdownScheduler.awaitTermination( Long.MAX_VALUE, NANOSECONDS );
            }
            catch ( InterruptedException e )
            {
                notInterrupted = false;
            }
        }
        notInterrupted &= shutdownThreadPoolsAwaitingKilled();
        return notInterrupted;
    }

    public String describeElapsedTimeout()
        throws TestSetFailedException
    {
        final StringBuilder msg = new StringBuilder();
        final boolean isShutdownTimeout = shutdownStatus.isTimeoutElapsed();
        final boolean isForcedShutdownTimeout = forcedShutdownStatus.isTimeoutElapsed();
        if ( isShutdownTimeout || isForcedShutdownTimeout )
        {
            msg.append( "The test run has finished abruptly after timeout of " );
            msg.append( nanosToSeconds( minTimeout( timeoutNanos, timeoutForcedNanos ) ) );
            msg.append( " seconds.\n" );

            try
            {
                final TreeSet<String> executedTests = new TreeSet<>();
                final TreeSet<String> incompleteTests = new TreeSet<>();

                if ( isShutdownTimeout )
                {
                    printShutdownHook( executedTests, incompleteTests, shutdownStatus.getDescriptionsBeforeShutdown() );
                }

                if ( isForcedShutdownTimeout )
                {
                    printShutdownHook( executedTests, incompleteTests,
                                       forcedShutdownStatus.getDescriptionsBeforeShutdown() );
                }

                if ( !executedTests.isEmpty() )
                {
                    msg.append( "These tests were executed in prior to the shutdown operation:\n" );
                    for ( String executedTest : executedTests )
                    {
                        msg.append( executedTest ).append( '\n' );
                    }
                }

                if ( !incompleteTests.isEmpty() )
                {
                    msg.append( "These tests are incomplete:\n" );
                    for ( String incompleteTest : incompleteTests )
                    {
                        msg.append( incompleteTest ).append( '\n' );
                    }
                }
            }
            catch ( InterruptedException e )
            {
                throw new TestSetFailedException( "Timed termination was interrupted.", e );
            }
            catch ( ExecutionException e )
            {
                throw new TestSetFailedException( e.getLocalizedMessage(), e.getCause() );
            }
        }
        return msg.toString();
    }

    private Future<ShutdownResult> scheduleShutdown()
    {
        return getShutdownScheduler().schedule( createShutdownTask(), timeoutNanos, NANOSECONDS );
    }

    private Future<ShutdownResult> scheduleForcedShutdown()
    {
        return getShutdownScheduler().schedule( createForcedShutdownTask(), timeoutForcedNanos, NANOSECONDS );
    }

    private ScheduledExecutorService getShutdownScheduler()
    {
        if ( shutdownScheduler == null )
        {
            shutdownScheduler = Executors.newScheduledThreadPool( 2, DAEMON_THREAD_FACTORY );
        }
        return shutdownScheduler;
    }

    private Callable<ShutdownResult> createShutdownTask()
    {
        return new Callable<ShutdownResult>()
        {
            @Override
            public ShutdownResult call()
                throws Exception
            {
                boolean stampedStatusWithTimeout = ParallelComputer.this.shutdownStatus.tryTimeout();
                return stampedStatusWithTimeout ? ParallelComputer.this.describeStopped( false ) : null;
            }
        };
    }

    private Callable<ShutdownResult> createForcedShutdownTask()
    {
        return new Callable<ShutdownResult>()
        {
            @Override
            public ShutdownResult call()
                throws Exception
            {
                boolean stampedStatusWithTimeout = ParallelComputer.this.forcedShutdownStatus.tryTimeout();
                return stampedStatusWithTimeout ? ParallelComputer.this.describeStopped( true ) : null;
            }
        };
    }

    private double nanosToSeconds( long nanos )
    {
        return (double) nanos / NANOS_IN_A_SECOND;
    }

    private boolean hasTimeout()
    {
        return timeoutNanos > 0;
    }

    private boolean hasTimeoutForced()
    {
        return timeoutForcedNanos > 0;
    }

    private static long secondsToNanos( double seconds )
    {
        double nanos = seconds > 0 ? seconds * NANOS_IN_A_SECOND : 0;
        return Double.isInfinite( nanos ) || nanos >= Long.MAX_VALUE ? 0 : (long) nanos;
    }

    private static long minTimeout( long timeout1, long timeout2 )
    {
        if ( timeout1 == 0 )
        {
            return timeout2;
        }
        else if ( timeout2 == 0 )
        {
            return timeout1;
        }
        else
        {
            return Math.min( timeout1, timeout2 );
        }
    }

    private static void printShutdownHook( Collection<String> executedTests, Collection<String> incompleteTests,
                                           Future<ShutdownResult> testsBeforeShutdown )
        throws ExecutionException, InterruptedException
    {
        if ( testsBeforeShutdown != null )
        {
            final ShutdownResult shutdownResult = testsBeforeShutdown.get();
            if ( shutdownResult != null )
            {
                for ( final Description test : shutdownResult.getTriggeredTests() )
                {
                    if ( test != null && test.getDisplayName() != null )
                    {
                        executedTests.add( test.getDisplayName() );
                    }
                }

                for ( final Description test : shutdownResult.getIncompleteTests() )
                {
                    if ( test != null && test.getDisplayName() != null )
                    {
                        incompleteTests.add( test.getDisplayName() );
                    }
                }
            }
        }
    }
}