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

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Parallel strategy for shared thread pool in private package.
 *
 * @author Tibor Digana (tibor17)
 * @see AbstractThreadPoolStrategy
 * @since 2.16
 */
final class SharedThreadPoolStrategy
    extends AbstractThreadPoolStrategy
{
    SharedThreadPoolStrategy( ConsoleLogger logger, ExecutorService threadPool )
    {
        super( logger, threadPool, new ConcurrentLinkedQueue<Future<?>>() );
    }

    @Override
    public boolean hasSharedThreadPool()
    {
        return true;
    }

    @Override
    public boolean finished()
        throws InterruptedException
    {
        boolean wasRunningAll = disable();
        for ( Future<?> futureResult : getFutureResults() )
        {
            try
            {
                futureResult.get();
            }
            catch ( InterruptedException e )
            {
                // after called external ExecutorService#shutdownNow()
                wasRunningAll = false;
            }
            catch ( ExecutionException e )
            {
                // JUnit core throws exception.
                if ( e.getCause() != null )
                {
                    logQuietly( e.getCause() );
                }
            }
            catch ( CancellationException e )
            {
                /**
                 * Cancelled by {@link Future#cancel(boolean)} in {@link stop()} and {@link stopNow()}.
                 */
            }
        }
        return wasRunningAll;
    }

    @Override
    protected boolean stop()
    {
        return stop( false );
    }

    @Override
    protected boolean stopNow()
    {
        return stop( true );
    }

    private boolean stop( boolean interrupt )
    {
        final boolean wasRunning = disable();
        for ( Future<?> futureResult : getFutureResults() )
        {
            futureResult.cancel( interrupt );
        }
        return wasRunning;
    }
}
