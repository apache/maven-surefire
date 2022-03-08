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

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Abstract parallel scheduling strategy in private package.
 * The remaining abstract methods have to be implemented differently
 * depending if the thread pool is shared with other strategies or not.
 *
 * @author Tibor Digana (tibor17)
 * @see SchedulingStrategy
 * @see SharedThreadPoolStrategy
 * @see NonSharedThreadPoolStrategy
 * @since 2.16
 */
abstract class AbstractThreadPoolStrategy
    extends SchedulingStrategy
{
    private final ExecutorService threadPool;

    private final Collection<Future<?>> futureResults;

    private volatile boolean isDestroyed;

    AbstractThreadPoolStrategy( ConsoleLogger logger, ExecutorService threadPool )
    {
        this( logger, threadPool, null );
    }

    AbstractThreadPoolStrategy( ConsoleLogger logger, ExecutorService threadPool, Collection<Future<?>> futureResults )
    {
        super( logger );
        this.threadPool = threadPool;
        this.futureResults = futureResults;
    }

    protected final ExecutorService getThreadPool()
    {
        return threadPool;
    }

    protected final Collection<Future<?>> getFutureResults()
    {
        return futureResults;
    }

    @Override
    public void schedule( Runnable task )
    {
        if ( canSchedule() )
        {
            Future<?> futureResult = threadPool.submit( task );
            if ( futureResults != null )
            {
                futureResults.add( futureResult );
            }
        }
    }

    @Override
    protected boolean stop()
    {
        boolean wasRunning = disable();
        if ( threadPool.isShutdown() )
        {
            wasRunning = false;
        }
        else
        {
            threadPool.shutdown();
        }
        return wasRunning;
    }

    @Override
    protected boolean stopNow()
    {
        boolean wasRunning = disable();
        if ( threadPool.isShutdown() )
        {
            wasRunning = false;
        }
        else
        {
            threadPool.shutdownNow();
        }
        return wasRunning;
    }

    /**
     * @see Scheduler.ShutdownHandler
     */
    @Override
    protected void setDefaultShutdownHandler( Scheduler.ShutdownHandler handler )
    {
        if ( threadPool instanceof ThreadPoolExecutor )
        {
            ThreadPoolExecutor pool = (ThreadPoolExecutor) threadPool;
            handler.setRejectedExecutionHandler( pool.getRejectedExecutionHandler() );
            pool.setRejectedExecutionHandler( handler );
        }
    }

    @Override
    public boolean destroy()
    {
        try
        {
            if ( !isDestroyed )//just an optimization
            {
                disable();
                threadPool.shutdown();
                this.isDestroyed |= threadPool.awaitTermination( Long.MAX_VALUE, TimeUnit.NANOSECONDS );
            }
            return isDestroyed;
        }
        catch ( InterruptedException e )
        {
            return false;
        }
    }
}
