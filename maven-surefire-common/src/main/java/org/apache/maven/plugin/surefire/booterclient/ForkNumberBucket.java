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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A bucket from which fork numbers can be drawn. Any drawn number needs to be returned to the bucket, in order to keep
 * the range of provided values delivered as small as possible.
 *
 * @author Andreas Gudian
 */
public final class ForkNumberBucket
{
    private static final ForkNumberBucket INSTANCE = new ForkNumberBucket();

    private final Queue<Integer> qFree = new ConcurrentLinkedQueue<Integer>();

    private final AtomicInteger highWaterMark = new AtomicInteger( 1 );

    /**
     * Non-public constructor
     */
    private ForkNumberBucket()
    {
    }

    /**
     * @return a fork number that is not currently in use. The value must be returned to the bucket using
     *         {@link #returnNumber(int)}.
     */
    public static int drawNumber()
    {
        return getInstance().drawNumberInternal();
    }

    /**
     * @param number the number to return to the bucket so that it can be reused.
     */
    public static void returnNumber( int number )
    {
        getInstance().returnNumberInternal( number );
    }

    /**
     * @return a singleton instance
     */
    private static ForkNumberBucket getInstance()
    {
        return INSTANCE;
    }

    /**
     * @return a fork number that is not currently in use. The value must be returned to the bucket using
     *         {@link #returnNumber(int)}.
     */
    private int drawNumberInternal()
    {
        Integer nextFree = qFree.poll();
        return nextFree == null ? highWaterMark.getAndIncrement() : nextFree;
    }

    /**
     * @return the highest number that has been drawn
     */
    private int getHighestDrawnNumber()
    {
        return highWaterMark.get() - 1;
    }

    /**
     * @param number the number to return to the bucket so that it can be reused.
     */
    private void returnNumberInternal( int number )
    {
        qFree.add( number );
    }
}
