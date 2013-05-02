package org.apache.maven.plugin.surefire.booterclient.output;

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

import org.apache.maven.shared.utils.cli.StreamConsumer;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Knows how to reconstruct *all* the state transmitted over stdout by the forked process.
 *
 * @author Kristian Rosenvold
 */
public class ThreadedStreamConsumer
    implements StreamConsumer
{

    private final java.util.concurrent.BlockingQueue<String> items = new LinkedBlockingQueue<String>();

    private static final String poison = "Pioson";

    private final Thread thread;

    private final Pumper pumper;

    static class Pumper
        implements Runnable
    {
        private final java.util.concurrent.BlockingQueue<String> queue;

        private final StreamConsumer target;

        private volatile Throwable throwable;


        Pumper( java.util.concurrent.BlockingQueue<String> queue, StreamConsumer target )
        {
            this.queue = queue;
            this.target = target;
        }

        public void run()
        {
            try
            {
                String item = queue.take();
                //noinspection StringEquality
                while ( item != poison )
                {
                    target.consumeLine( item );
                    item = queue.take();
                }
            }
            catch ( Throwable t )
            {
                // Think about what happens if the producer overruns us and creates an OOME. Not nice.
                // Maybe limit length of blocking queue
                this.throwable = t;
            }
        }

        public Throwable getThrowable()
        {
            return throwable;
        }
    }

    public ThreadedStreamConsumer( StreamConsumer target )
    {
        pumper = new Pumper( items, target );
        thread = new Thread( pumper, "ThreadedStreamConsumer" );
        thread.start();
    }

    public void consumeLine( String s )
    {
        items.add( s );
        if ( items.size() > 10000 )
        {
            try
            {
                Thread.sleep( 100 );
            }
            catch ( InterruptedException ignore )
            {
            }
        }
    }


    public void close()
    {
        try
        {
            items.add( poison );
            thread.join();
        }
        catch ( InterruptedException e )
        {
            throw new RuntimeException( e );
        }

        //noinspection ThrowableResultOfMethodCallIgnored
        if ( pumper.getThrowable() != null )
        {
            throw new RuntimeException( pumper.getThrowable() );
        }
    }
}
