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

import org.apache.maven.surefire.junitcore.Logger;
import org.apache.maven.surefire.util.internal.DaemonThreadFactory;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the factories in SchedulingStrategy.
 * <p/>
 * Th point of these tests is to check {@link Task#result} if changed
 * from <code>false</code> to <code>true</code> after all scheduled tasks
 * have finished.
 * The call {@link SchedulingStrategy#finished()} is waiting until the
 * strategy has finished.
 * Then {@link Task#result} should be asserted that is <code>true</code>.
 *
 * @author Tibor Digana (tibor17)
 * @see SchedulingStrategy
 * @since 2.16
 */
public class SchedulingStrategiesTest
{
    private static final ThreadFactory DAEMON_THREAD_FACTORY = DaemonThreadFactory.newDaemonThreadFactory();

    @Test
    public void invokerStrategy()
        throws InterruptedException
    {
        SchedulingStrategy strategy = SchedulingStrategies.createInvokerStrategy( new Logger() );
        assertFalse( strategy.hasSharedThreadPool() );
        assertTrue( strategy.canSchedule() );

        Task task = new Task();

        strategy.schedule( task );

        assertTrue( strategy.canSchedule() );

        assertTrue( task.result );

        assertTrue( strategy.finished() );
        assertFalse( strategy.canSchedule() );
    }

    @Test
    public void nonSharedPoolStrategy()
        throws InterruptedException
    {
        SchedulingStrategy strategy = SchedulingStrategies.createParallelStrategy( new Logger(),  2 );
        assertFalse( strategy.hasSharedThreadPool() );
        assertTrue( strategy.canSchedule() );

        Task task1 = new Task();
        Task task2 = new Task();

        strategy.schedule( task1 );
        strategy.schedule( task2 );

        assertTrue( strategy.canSchedule() );

        assertTrue( strategy.finished() );
        assertFalse( strategy.canSchedule() );

        assertTrue( task1.result );
        assertTrue( task2.result );
    }

    @Test(expected = NullPointerException.class)
    public void sharedPoolStrategyNullPool()
    {
        SchedulingStrategies.createParallelSharedStrategy( new Logger(), null );
    }

    @Test
    public void sharedPoolStrategy()
        throws InterruptedException
    {
        ExecutorService sharedPool = Executors.newCachedThreadPool( DAEMON_THREAD_FACTORY );

        SchedulingStrategy strategy1 = SchedulingStrategies.createParallelSharedStrategy( new Logger(), sharedPool );
        assertTrue( strategy1.hasSharedThreadPool() );
        assertTrue( strategy1.canSchedule() );

        SchedulingStrategy strategy2 = SchedulingStrategies.createParallelSharedStrategy( new Logger(), sharedPool );
        assertTrue( strategy2.hasSharedThreadPool() );
        assertTrue( strategy2.canSchedule() );

        Task task1 = new Task();
        Task task2 = new Task();
        Task task3 = new Task();
        Task task4 = new Task();

        strategy1.schedule( task1 );
        strategy2.schedule( task2 );
        strategy1.schedule( task3 );
        strategy2.schedule( task4 );

        assertTrue( strategy1.canSchedule() );
        assertTrue( strategy2.canSchedule() );

        assertTrue( strategy1.finished() );
        assertFalse( strategy1.canSchedule() );

        assertTrue( strategy2.finished() );
        assertFalse( strategy2.canSchedule() );

        assertTrue( task1.result );
        assertTrue( task2.result );
        assertTrue( task3.result );
        assertTrue( task4.result );
    }

    @Test
    public void infinitePoolStrategy()
        throws InterruptedException
    {
        SchedulingStrategy strategy = SchedulingStrategies.createParallelStrategyUnbounded( new Logger() );
        assertFalse( strategy.hasSharedThreadPool() );
        assertTrue( strategy.canSchedule() );

        Task task1 = new Task();
        Task task2 = new Task();

        strategy.schedule( task1 );
        strategy.schedule( task2 );

        assertTrue( strategy.canSchedule() );

        assertTrue( strategy.finished() );
        assertFalse( strategy.canSchedule() );

        assertTrue( task1.result );
        assertTrue( task2.result );
    }

    static class Task
        implements Runnable
    {
        volatile boolean result = false;

        public void run()
        {
            result = true;
        }
    }
}
