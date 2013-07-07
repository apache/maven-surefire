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

import java.util.concurrent.Semaphore;

/**
 * The Balancer controls the maximum of concurrent threads in the current Scheduler(s) and prevents
 * from own thread resources exhaustion if other group of schedulers share the same pool of threads.
 * <p>
 * If a permit is available, {@link #acquirePermit()} simply returns and a new test is scheduled
 * by {@link Scheduler#schedule(Runnable)} in the current runner. Otherwise waiting for a release.
 * One permit is released as soon as the child thread has finished.
 *
 * @author Tibor Digana (tibor17)
 * @since 2.16
 */
public class Balancer {
    private final Semaphore balancer;
    private final int maxPermits;

    /**
     * Infinite permits.
     */
    protected Balancer() {
        this(0);
    }

    /**
     * <tt>fair</tt> set to false.
     * @see #Balancer(int, boolean)
     */
    public Balancer(int numPermits) {
        this(numPermits, false);
    }

    /**
     * @param numPermits number of permits to acquire when maintaining concurrency on tests
     * @param fair <tt>true</tt> guarantees the waiting schedulers to wake up in order they acquired a permit
     */
    public Balancer(int numPermits, boolean fair) {
        boolean shouldBalance = numPermits > 0 && numPermits < Integer.MAX_VALUE;
        balancer = shouldBalance ? new Semaphore(numPermits, fair) : null;
        maxPermits = numPermits;
    }

    /**
     * Acquires a permit from this balancer, blocking until one is available.
     *
     * @return <code>true</code> if current thread is <em>NOT</em> interrupted
     *         while waiting for a permit.
     */
    public boolean acquirePermit() {
        if (balancer != null) {
            try {
                balancer.acquire();
            } catch (InterruptedException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Releases a permit, returning it to the balancer.
     */
    public void releasePermit() {
        if (balancer != null) {
            balancer.release();
        }
    }

    public void releaseAllPermits() {
        if (balancer != null) {
            balancer.release(maxPermits);
        }
    }
}
