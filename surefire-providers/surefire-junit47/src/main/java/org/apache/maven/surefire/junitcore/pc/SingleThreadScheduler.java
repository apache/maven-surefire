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

import org.junit.runner.Description;
import org.junit.runners.model.RunnerScheduler;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Used to execute tests annotated with {@link net.jcip.annotations.NotThreadSafe}.
 * <p/>
 *
 * @author <a href="mailto:tibor.digana@gmail.com">Tibor Digana (tibor17)</a>
 * @see ParallelComputerBuilder
 * @since 2.18
 */
final class SingleThreadScheduler
{
    private final ExecutorService pool = newPool();

    private final Scheduler master = new Scheduler( null, SchedulingStrategies.createParallelSharedStrategy( pool ) );

    RunnerScheduler newRunnerScheduler()
    {
        return new Scheduler( null, master, SchedulingStrategies.createParallelSharedStrategy( pool ) );
    }

    /**
     * @see Scheduler#shutdown(boolean)
     */
    Collection<Description> shutdown( boolean shutdownNow )
    {
        return master.shutdown( shutdownNow );
    }

    private static ExecutorService newPool()
    {
        final ThreadFactory factory = new ThreadFactory()
        {
            public Thread newThread( Runnable r )
            {
                return new Thread( r, "maven-surefire-plugin@NotThreadSafe" );
            }
        };
        return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), factory);
    }
}