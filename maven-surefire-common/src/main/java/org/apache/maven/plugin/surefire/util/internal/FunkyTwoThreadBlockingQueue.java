package org.apache.maven.plugin.surefire.util.internal;

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
 * <p/>
 * TwoThreadBlockingQueue insert 5000000 elements in  = 52
 * FunkyTwoThreadBlockingQueue insert 5000000 elements in  = 42
 * TwoThreadBlockingQueue produced and taken 5000000 elements in  = 104
 * LinkedBlockingQueue insert 5000000 elements in  = 1815
 * LinkedBlockingDeque insert 5000000 elements in  = 113
 * ArrayList insert 5000000 elements in  = 18
 * LinkedList insert 5000000 elements in  = 334
 * <p/>
 * Todo: Determine if this design actually works ;)
 *
 * @author Kristian Rosenvold
 */
public class FunkyTwoThreadBlockingQueue
    implements BlockingQueue
{
    final int chunkSize = 100;

    private Chunk takeChunk = new Chunk();

    private int takePos = 0;

    private Chunk insertChunk = takeChunk;

    private int insertPos = 0;

    private volatile boolean memoryModelGuard;


    public void put( String object )
    {
        insertChunk.elements[insertPos] = object;
        if ( ++insertPos == chunkSize )
        {
            Chunk newChunk = new Chunk();
            insertChunk.next = newChunk;
            insertChunk = newChunk;
            insertPos = 0;
        }
        memoryModelGuard = true;
    }

    public void add( String object )
    {
        put( object );
    }


    public String take()
        throws InterruptedException
    {
        if ( takePos >= chunkSize )
        {
            takeChunk = takeChunk.next;
            takePos = 0;
        }

        boolean fud = memoryModelGuard;
        String next = takeChunk.elements[takePos];
        while ( next == null )
        {
            Thread.sleep( 1 );
            fud = memoryModelGuard;
            next = takeChunk.elements[takePos];
        }
        takePos++;
        return next;
    }

    final class Chunk
    {
        final String[] elements = new String[chunkSize];

        volatile Chunk next;
    }
}
