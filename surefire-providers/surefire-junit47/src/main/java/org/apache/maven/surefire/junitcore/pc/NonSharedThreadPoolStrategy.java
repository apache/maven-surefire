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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Parallel strategy for non-shared thread pool in private package.
 *
 * @author Tibor Digana (tibor17)
 * @see AbstractThreadPoolStrategy
 * @since 2.16
 */
final class NonSharedThreadPoolStrategy
    extends AbstractThreadPoolStrategy
{
    NonSharedThreadPoolStrategy( ConsoleLogger logger, ExecutorService threadPool )
    {
        super( logger, threadPool );
    }

    @Override
    public boolean hasSharedThreadPool()
    {
        return false;
    }

    @Override
    public boolean finished()
        throws InterruptedException
    {
        boolean wasRunning = disable();
        getThreadPool().shutdown();
        getThreadPool().awaitTermination( Long.MAX_VALUE, TimeUnit.NANOSECONDS );
        return wasRunning;
    }
}
