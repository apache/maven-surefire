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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.maven.plugin.surefire.util.internal.BlockingQueue;
import org.apache.maven.plugin.surefire.util.internal.FunkyTwoThreadBlockingQueue;
import org.apache.maven.plugin.surefire.util.internal.TwoThreadBlockingQueue;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author Kristian Rosenvold
 */
public class TwoThreadBlockingQueueTest
    extends TestCase
{
    final int num = 100000;

    public void testPut()
        throws Exception
    {
        BlockingQueue twoThreadBlockingQueue = new TwoThreadBlockingQueue();

        String[] items = generate( num );
        //long start = System.currentTimeMillis();
        for ( String item : items )
        {
            twoThreadBlockingQueue.add( item );
        }
        //long elapsed = System.currentTimeMillis() - start;
        //System.out.println( "TwoThreadBlockingQueue insert " + num + " elements in  = " + elapsed );
        System.gc();
    }

    public void testFunkyPut()
        throws Exception
    {
        FunkyTwoThreadBlockingQueue twoThreadBlockingQueue = new FunkyTwoThreadBlockingQueue();

        String[] items = generate( num );
        //long start = System.currentTimeMillis();
        for ( String item : items )
        {
            twoThreadBlockingQueue.put( item );
        }
        //long elapsed = System.currentTimeMillis() - start;
        //System.out.println( "FunkyTwoThreadBlockingQueue insert " + num + " elements in  = " + elapsed );
        System.gc();
    }


    public void testPutAndTake()
        throws Exception
    {
        final FunkyTwoThreadBlockingQueue twoThreadBlockingQueue = new FunkyTwoThreadBlockingQueue();

        Callable<String> consumer = new Callable<String>()
        {
            public String call()
                throws Exception
            {
                int num = 0;
                String taken;
                do
                {
                    taken = twoThreadBlockingQueue.take();
                    if ( taken != TwoThreadBlockingQueue.poison )
                    {
                        Assert.assertEquals( "item" + num++, taken );
                    }
                }
                while ( taken != TwoThreadBlockingQueue.poison );
                return taken;
            }
        };

        FutureTask<String> futureTask = new FutureTask<String>( consumer );
        Thread thread = new Thread( futureTask );
        thread.start();

        String[] items = generate( num );
        //long start = System.currentTimeMillis();
        for ( String item : items )
        {
            twoThreadBlockingQueue.put( item );
        }
        twoThreadBlockingQueue.put( TwoThreadBlockingQueue.poison );
        //long elapsed = System.currentTimeMillis() - start;

        futureTask.get();

        // System.out.println( "TwoThreadBlockingQueue produced and taken " + num + " elements in  = " + elapsed );
        System.gc();
    }

    public void testLBQPut()
        throws Exception
    {
        LinkedBlockingQueue<String> twoThreadBlockingQueue = new LinkedBlockingQueue<String>();

        String[] items = generate( num );
        //long start = System.currentTimeMillis();
        for ( String item : items )
        {
            twoThreadBlockingQueue.put( item );
        }
        //long elapsed = System.currentTimeMillis() - start;
        //System.out.println( "LinkedBlockingQueue insert " + num + " elements in  = " + elapsed );
        System.gc();
    }

    public void testArrayList()
        throws Exception
    {
        List<String> twoThreadBlockingQueue = new ArrayList<String>( num );

        String[] items = generate( num );
        //long start = System.currentTimeMillis();
        for ( String item : items )
        {
            twoThreadBlockingQueue.add( item );
        }
        //long elapsed = System.currentTimeMillis() - start;
        //System.out.println( "ArrayList insert " + num + " elements in  = " + elapsed );
        System.gc();
    }

    public void testLinkedList()
        throws Exception
    {
        LinkedList<String> twoThreadBlockingQueue = new LinkedList<String>();

        String[] items = generate( num );
        //long start = System.currentTimeMillis();
        for ( String item : items )
        {
            twoThreadBlockingQueue.add( item );
        }
        //long elapsed = System.currentTimeMillis() - start;
        //System.out.println( "LinkedList insert " + num + " elements in  = " + elapsed );
        System.gc();
    }

    public void testTake()
        throws Exception
    {
    }

    String[] generate( int num )
    {
        String[] result = new String[num];
        for ( int i = 0; i < num; i++ )
        {
            result[i] = "item" + i;
        }
        return result;
    }
}
