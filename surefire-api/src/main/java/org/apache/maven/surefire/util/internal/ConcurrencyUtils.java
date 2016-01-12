package org.apache.maven.surefire.util.internal;

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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concurrency utilities.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public final class ConcurrencyUtils
{
    private ConcurrencyUtils()
    {
        throw new IllegalStateException( "not instantiable constructor" );
    }

    /**
     * Decreases {@code counter} to zero, or does not change the counter if negative.
     * This method pretends been atomic. Only one thread can succeed setting the counter to zero.
     *
     * @param counter atomic counter
     * @return {@code true} if this Thread modified concurrent counter from any positive number down to zero.
     */
    @SuppressWarnings( "checkstyle:emptyforiteratorpad" )
    public static boolean countDownToZero( AtomicInteger counter )
    {
        for (;;)
        {
            int c = counter.get();
            if ( c > 0 )
            {
                int newCounter = c - 1;
                if ( counter.compareAndSet( c, newCounter ) )
                {
                    return newCounter == 0;
                }
            }
            else
            {
                return false;
            }
        }
    }
}
