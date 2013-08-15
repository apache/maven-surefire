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
import org.junit.runner.Computer;
import org.junit.runner.Description;

import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private final long timeoutNanos;

    private final long timeoutForcedNanos;

    private ScheduledExecutorService shutdownScheduler;

    private Future<Collection<Description>> testsBeforeShutdown;

    private Future<Collection<Description>> testsBeforeForcedShutdown;

    public ParallelComputer( long timeout, long timeoutForced, TimeUnit timeoutUnit )
    {
        this.timeoutNanos = timeoutUnit.toNanos( timeout );
        this.timeoutForcedNanos = timeoutUnit.toNanos( timeoutForced );
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

    private static Collection<String> printShutdownHook( Future<Collection<Description>> future )
        throws TestSetFailedException
    {
        if ( !future.isCancelled() && future.isDone() )
        {
            try
            {
                TreeSet<String> executedTests = new TreeSet<String>();
                for ( Description executedTest : future.get() )
                {
                    if ( executedTest != null && executedTest.getDisplayName() != null )
                    {
                        executedTests.add( executedTest.getDisplayName() );
                    }
                }
                return executedTests;
            }
            catch ( Exception e )
            {
                throw new TestSetFailedException( e );
            }
        }
        return Collections.emptySet();
    }

    public abstract Collection<Description> shutdown( boolean shutdownNow );

    protected final void beforeRunQuietly()
    {
        testsBeforeShutdown = timeoutNanos > 0 ? scheduleShutdown() : null;
        testsBeforeForcedShutdown = timeoutForcedNanos > 0 ? scheduleForcedShutdown() : null;
    }

    protected final void afterRunQuietly()
    {
        if ( shutdownScheduler != null )
        {
            shutdownScheduler.shutdownNow();
        }
    }

    public String describeElapsedTimeout()
        throws TestSetFailedException
    {
        TreeSet<String> executedTests = new TreeSet<String>();
        if ( testsBeforeShutdown != null )
        {
            executedTests.addAll( printShutdownHook( testsBeforeShutdown ) );
        }

        if ( testsBeforeForcedShutdown != null )
        {
            executedTests.addAll( printShutdownHook( testsBeforeForcedShutdown ) );
        }

        StringBuilder msg = new StringBuilder();
        if ( !executedTests.isEmpty() )
        {
            msg.append( "The test run has finished abruptly after timeout of " );
            msg.append( nanosToSeconds( minTimeout( timeoutNanos, timeoutForcedNanos ) ) );
            msg.append( " seconds.\n" );
            msg.append( "These tests were executed in prior of the shutdown operation:\n" );
            for ( String executedTest : executedTests )
            {
                msg.append( executedTest ).append( '\n' );
            }
        }
        return msg.toString();
    }

    private Future<Collection<Description>> scheduleShutdown()
    {
        return getShutdownScheduler().schedule( createShutdownTask( false ), timeoutNanos, TimeUnit.NANOSECONDS );
    }

    private Future<Collection<Description>> scheduleForcedShutdown()
    {
        return getShutdownScheduler().schedule( createShutdownTask( true ), timeoutForcedNanos, TimeUnit.NANOSECONDS );
    }

    private ScheduledExecutorService getShutdownScheduler()
    {
        if ( shutdownScheduler == null )
        {
            shutdownScheduler = Executors.newScheduledThreadPool( 2 );
        }
        return shutdownScheduler;
    }

    private Callable<Collection<Description>> createShutdownTask( final boolean isForced )
    {
        return new Callable<Collection<Description>>()
        {
            public Collection<Description> call()
                throws Exception
            {
                return ParallelComputer.this.shutdown( isForced );
            }
        };
    }

    private double nanosToSeconds( long nanos )
    {
        return (double) nanos / 1E9;
    }
}