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

/**
 * A producer/consumer queue that is optimized for *one* producer thread
 * and *one* consumer thread, and solely optimized for efficient inserts
 * by the producer, minimizing producer locking for hand-off to
 * a second consumer.
 *
 * The producer can actually come in on different threads
 * (because lastInserted is volatile), but can/will lose
 * items if they arrive concurrently. Take only supports a single
 * client.
 *
 * This runs like crazy, but is not the most garbage friendly around.
 *
 * TwoThreadBlockingQueue insert 5000000 elements in  = 52ms
 * LinkedBlockingQueue insert 5000000 elements in  = 179ms
 * LikedBlockingDeque insert 5000000 elements in  = 114ms
 * ArrayList insert 5000000 elements in  = 18ms (sized at correct size from start)
 *
 * @author Kristian Rosenvold
 */
public class TwoThreadBlockingQueue
    implements BlockingQueue
{
    private volatile Element lastInserted;
    private volatile Element lastTaken;
    private volatile Element first;

    public static final String poison = "poison";

    public void add( String object )
    {
        Element next = new Element( object);
        if (lastInserted == null){
            first = lastInserted = next;
        } else {
            lastInserted.next = next;
            lastInserted = next;
        }
    }

    public String take()
        throws InterruptedException
    {
        if (lastTaken == null){
            while (first == null){
                Thread.sleep(1);
            }
            lastTaken = first;
            first = null;
        } else {
            Element next = lastTaken.next;
            while (next == null){
                Thread.sleep(1);
                next = lastTaken.next;
            }
            lastTaken = next;
        }
        return lastTaken.object;
    }

    private static class Element
    {
        private final String object;

        private volatile Element next;

        Element( String object )
        {
            this.object = object;
        }
    }

}
