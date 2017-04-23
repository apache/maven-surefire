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

/**
 * @author Tibor Digana (tibor17)
 * @see Balancer
 * @since 2.16
 */
public class BalancerFactory
{
    private BalancerFactory()
    {
    }

    /**
     * Infinite permits.
     * @return Balancer wih infinite permits
     */
    public static Balancer createInfinitePermitsBalancer()
    {
        return balancer( 0, false );
    }

    /**
     * Balancer without fairness.
     * Fairness guarantees the waiting schedulers to wake up in order they acquired a permit.
     *
     * @param concurrency number of permits to acquire when maintaining concurrency on tests
     * @return Balancer with given number of permits
     */
    public static Balancer createBalancer( int concurrency )
    {
        return balancer( concurrency, false );
    }

    /**
     * Balancer with fairness.
     * Fairness guarantees the waiting schedulers to wake up in order they acquired a permit.
     *
     * @param concurrency number of permits to acquire when maintaining concurrency on tests
     * @return Balancer with given number of permits
     */
    public static Balancer createBalancerWithFairness( int concurrency )
    {
        return balancer( concurrency, true );
    }

    private static Balancer balancer( int concurrency, boolean fairness )
    {
        boolean shouldBalance = concurrency > 0 && concurrency < Integer.MAX_VALUE;
        return shouldBalance ? new ThreadResourcesBalancer( concurrency, fairness ) : new NullBalancer();
    }
}