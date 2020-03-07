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
import org.apache.maven.surefire.util.internal.DaemonThreadFactory;
import org.junit.runner.Description;
import org.junit.runners.model.RunnerScheduler;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Used to execute tests annotated with net.jcip.annotations.NotThreadSafe.
 * <br>
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @see ParallelComputerBuilder
 * @since 2.18
 */
final class SingleThreadScheduler
{
    private final ConsoleStream logger;

    private final ExecutorService pool = newPool();

    private final Scheduler master;

    private static ExecutorService newPool()
    {
        ThreadFactory tf = DaemonThreadFactory.newDaemonThreadFactory( "maven-surefire-plugin@NotThreadSafe" );
        return new ThreadPoolExecutor( 1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), tf );
    }

    SingleThreadScheduler( ConsoleStream logger )
    {
        this.logger = logger;
        SchedulingStrategy strategy = SchedulingStrategies.createParallelSharedStrategy( logger, pool );
        master = new Scheduler( logger, null, strategy );
    }

    RunnerScheduler newRunnerScheduler()
    {
        SchedulingStrategy strategy = SchedulingStrategies.createParallelSharedStrategy( logger, pool );
        return new Scheduler( logger, null, master, strategy );
    }

    /**
     * @see Scheduler#describeStopped(boolean)
     */
    ShutdownResult describeStopped( boolean shutdownNow )
    {
        ShutdownResult shutdownResult = master.describeStopped( shutdownNow );
        return new ShutdownResult( copyExisting( shutdownResult.getTriggeredTests() ),
                                   copyExisting( shutdownResult.getIncompleteTests() ) );
    }

    /**
     * @see Scheduler#shutdownThreadPoolsAwaitingKilled()
     */
    boolean shutdownThreadPoolsAwaitingKilled()
    {
        return master.shutdownThreadPoolsAwaitingKilled();
    }

    private Collection<Description> copyExisting( Collection<Description> descriptions )
    {
        Collection<Description> activeChildren = new ConcurrentLinkedQueue<>( descriptions );
        ParallelComputerUtil.removeUnusedDescriptions( activeChildren );
        return activeChildren;
    }
}