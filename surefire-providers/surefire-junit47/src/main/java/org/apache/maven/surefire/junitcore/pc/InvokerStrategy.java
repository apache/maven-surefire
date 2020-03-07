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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The sequentially executing strategy in private package.
 *
 * @author Tibor Digana (tibor17)
 * @see SchedulingStrategy
 * @since 2.16
 */
final class InvokerStrategy
    extends SchedulingStrategy
{

    private final Queue<Thread> activeThreads = new ConcurrentLinkedQueue<>();

    protected InvokerStrategy( ConsoleStream logger )
    {
        super( logger );
    }

    @Override
    public void schedule( Runnable task )
    {
        if ( canSchedule() )
        {
            final Thread currentThread = Thread.currentThread();
            try
            {
                activeThreads.add( currentThread );
                task.run();
            }
            finally
            {
                activeThreads.remove( currentThread );
            }
        }
    }

    @Override
    protected boolean stop()
    {
        return disable();
    }

    @Override
    protected boolean stopNow()
    {
        final boolean stopped = disable();

        for ( Thread activeThread = activeThreads.poll(); activeThread != null; activeThread = activeThreads.poll() )
        {
            activeThread.interrupt();
        }
        return stopped;
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
        return disable();
    }

    @Override
    public boolean destroy()
    {
        return stop();
    }
}
