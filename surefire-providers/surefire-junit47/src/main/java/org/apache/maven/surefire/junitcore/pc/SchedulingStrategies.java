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
import org.apache.maven.surefire.api.util.internal.DaemonThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * The factory of {@link SchedulingStrategy}.
 *
 * @author Tibor Digana (tibor17)
 * @since 2.16
 */
public class SchedulingStrategies
{
    private static final ThreadFactory DAEMON_THREAD_FACTORY = DaemonThreadFactory.newDaemonThreadFactory();

    /**
     * @param logger current error logger
     * @return sequentially executing strategy
     */
    public static SchedulingStrategy createInvokerStrategy( ConsoleLogger logger )
    {
        return new InvokerStrategy( logger );
    }

    /**
     * @param logger current error logger
     * @param nThreads fixed pool capacity
     * @return parallel scheduling strategy
     */
    public static SchedulingStrategy createParallelStrategy( ConsoleLogger logger, int nThreads )
    {
        return new NonSharedThreadPoolStrategy( logger,
                                                Executors.newFixedThreadPool( nThreads, DAEMON_THREAD_FACTORY ) );
    }

    /**
     * @param logger current error logger
     * @return parallel scheduling strategy with unbounded capacity
     */
    public static SchedulingStrategy createParallelStrategyUnbounded( ConsoleLogger logger )
    {
        return new NonSharedThreadPoolStrategy( logger, Executors.newCachedThreadPool( DAEMON_THREAD_FACTORY ) );
    }

    /**
     * The <code>threadPool</code> passed to this strategy can be shared in other strategies.
     * <br>
     * The call {@link SchedulingStrategy#finished()} is waiting until own tasks have finished.
     * New tasks will not be scheduled by this call in this strategy. This strategy is not
     * waiting for other strategies to finish. The {@link org.junit.runners.model.RunnerScheduler#finished()} may
     * freely use {@link SchedulingStrategy#finished()}.
     *
     * @param logger current error logger
     * @param threadPool thread pool possibly shared with other strategies
     * @return parallel strategy with shared thread pool
     * @throws NullPointerException if <code>threadPool</code> is null
     */
    public static SchedulingStrategy createParallelSharedStrategy( ConsoleLogger logger, ExecutorService threadPool )
    {
        if ( threadPool == null )
        {
            throw new NullPointerException( "null threadPool in #createParallelSharedStrategy" );
        }
        return new SharedThreadPoolStrategy( logger, threadPool );
    }
}
