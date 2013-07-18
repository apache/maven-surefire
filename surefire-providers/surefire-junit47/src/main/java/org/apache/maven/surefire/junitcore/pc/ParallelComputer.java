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

import org.junit.runner.Computer;
import org.junit.runner.Description;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ParallelComputer extends JUnit {@link Computer} and has a shutdown functionality.
 *
 * @author Tibor Digana (tibor17)
 * @since 2.16
 *
 * @see ParallelComputerBuilder
 */
public abstract class ParallelComputer extends Computer
{
    private ScheduledExecutorService shutdownScheduler;

    public abstract Collection<Description> shutdown( boolean shutdownNow );

    protected final void afterRunQuietly()
    {
        if ( shutdownScheduler != null )
        {
            shutdownScheduler.shutdownNow();
        }
    }

    public Future<Collection<Description>> scheduleShutdown( int timeout, TimeUnit unit )
    {
        return getShutdownScheduler().schedule( createShutdownTask( false ), timeout, unit );
    }

    public Future<Collection<Description>> scheduleForcedShutdown( int timeout, TimeUnit unit )
    {
        return getShutdownScheduler().schedule( createShutdownTask( true ), timeout, unit );
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
            public Collection<Description> call() throws Exception
            {
                return shutdown( isForced );
            }
        };
    }
}