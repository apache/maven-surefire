package org.apache.maven.plugin.surefire.booterclient;

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

import org.apache.maven.surefire.booter.SystemUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import static org.apache.maven.surefire.util.internal.DaemonThreadFactory.newDaemonThread;

/**
 * Loads platform specifics.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
 */
public final class Platform
{
    private final RunnableFuture<Long> pidJob;

    public Platform()
    {
        // the job may take 50 or 80 ms
        pidJob = new FutureTask<Long>( pidJob() );
        newDaemonThread( pidJob ).start();
    }

    public Long getPid()
    {
        try
        {
            return pidJob.get();
        }
        catch ( Exception e )
        {
            return null;
        }
    }

    private static Callable<Long> pidJob()
    {
        return new Callable<Long>()
        {
            @Override
            public Long call() throws Exception
            {
                return SystemUtils.pid();
            }
        };
    }
}
