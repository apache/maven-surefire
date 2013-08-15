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

import org.junit.runners.model.RunnerScheduler;

import java.util.concurrent.RejectedExecutionException;

/**
 * Specifies the strategy of scheduling whether sequential, or parallel.
 * The strategy may use a thread pool <em>shared</em> with other strategies.
 * <p/>
 * One instance of strategy can be used just by one {@link Scheduler}.
 * <p/>
 * The strategy is scheduling tasks in {@link #schedule(Runnable)} and awaiting them
 * completed in {@link #finished()}. Both methods should be used in one thread.
 *
 * @author Tibor Digana (tibor17)
 * @since 2.16
 */
public abstract class SchedulingStrategy
{

    /**
     * Schedules tasks if {@link #canSchedule()}.
     *
     * @param task runnable to schedule in a thread pool or invoke
     * @throws RejectedExecutionException if <tt>task</tt>
     *                                    cannot be scheduled for execution
     * @throws NullPointerException       if <tt>task</tt> is <tt>null</tt>
     * @see RunnerScheduler#schedule(Runnable)
     * @see java.util.concurrent.Executor#execute(Runnable)
     */
    protected abstract void schedule( Runnable task );

    /**
     * Waiting for scheduled tasks to finish.
     * New tasks will not be scheduled by calling this method.
     *
     * @return <tt>true</tt> if successfully stopped the scheduler, else
     *         <tt>false</tt> if already stopped (a <em>shared</em> thread
     *         pool was shutdown externally).
     * @throws InterruptedException if interrupted while waiting
     *                              for scheduled tasks to finish
     * @see RunnerScheduler#finished()
     */
    protected abstract boolean finished()
        throws InterruptedException;

    /**
     * Stops scheduling new tasks (e.g. by {@link java.util.concurrent.ExecutorService#shutdown()}
     * on a private thread pool which cannot be <em>shared</em> with other strategy).
     *
     * @return <tt>true</tt> if successfully stopped the scheduler, else
     *         <tt>false</tt> if already stopped (a <em>shared</em> thread
     *         pool was shutdown externally).
     * @see java.util.concurrent.ExecutorService#shutdown()
     */
    protected abstract boolean stop();

    /**
     * Stops scheduling new tasks and <em>interrupts</em> running tasks
     * (e.g. by {@link java.util.concurrent.ExecutorService#shutdownNow()} on a private thread pool
     * which cannot be <em>shared</em> with other strategy).
     * <p/>
     * This method calls {@link #stop()} by default.
     *
     * @return <tt>true</tt> if successfully stopped the scheduler, else
     *         <tt>false</tt> if already stopped (a <em>shared</em> thread
     *         pool was shutdown externally).
     * @see java.util.concurrent.ExecutorService#shutdownNow()
     */
    protected boolean stopNow()
    {
        return stop();
    }

    protected void setDefaultShutdownHandler( Scheduler.ShutdownHandler handler )
    {
    }

    /**
     * @return <tt>true</tt> if a thread pool associated with this strategy
     *         can be shared with other strategies.
     */
    protected abstract boolean hasSharedThreadPool();

    /**
     * @return <tt>true</tt> unless stopped or finished.
     */
    protected abstract boolean canSchedule();
}
