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
import org.apache.maven.surefire.util.internal.DaemonThreadFactory;

import java.util.concurrent.BlockingQueue;
import java.io.Closeable;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Knows how to reconstruct *all* the state transmitted over stdout by the forked process.
 *
 * @author Kristian Rosenvold
 */
public final class ThreadedStreamConsumer
    implements StreamConsumer, Closeable
{
    private static final String POISON = "Pioson";

    private static final int ITEM_LIMIT_BEFORE_SLEEP = 10000;

    private final BlockingQueue<String> items = new LinkedBlockingQueue<String>();

    private final Thread thread;

    private final Pumper pumper;

    static class Pumper
        implements Runnable
    {
        private final BlockingQueue<String> queue;

        private final StreamConsumer target;

        private volatile Throwable throwable;


        Pumper( BlockingQueue<String> queue, StreamConsumer target )
        {
            this.queue = queue;
            this.target = target;
        }

        /**
         * Calls {@link ForkClient#consumeLine(String)} throwing {@link RuntimeException}. Even if {@link ForkClient}
         * is not fault-tolerant, this method MUST be fault-tolerant except for {@link InterruptedException}.<p/>
         * This Thread is interrupted by {@link #close() closing the consumer}.<p/>
         * If {@link org.apache.maven.plugin.surefire.report.ConsoleOutputFileReporter#writeTestOutput} throws
         * {@link java.io.IOException} this method MUST NOT interrupt reading the events from forked JVM; otherwise
         * we can simply loose events like acquire-next-test which means that {@link ForkClient} hangs on waiting
         * for old test to complete and therefore the plugin permanently in progress.
         */
        @SuppressWarnings( "checkstyle:stringliteralequalitycheck" )
        public void run()
        {
            String item = null;
            do
            {
                try
                {
                    item = queue.take();
                    target.consumeLine( item );
                }
                catch ( InterruptedException e )
                {
                    break;
                }
                catch ( Throwable t )
                {
                    throwable = t;
                }
            } while ( item != POISON );
        }

        public Throwable getThrowable()
        {
            return throwable;
        }
    }

    public ThreadedStreamConsumer( StreamConsumer target )
    {
        pumper = new Pumper( items, target );
        thread = DaemonThreadFactory.newDaemonThread( pumper, "ThreadedStreamConsumer" );
        thread.start();
    }

    @SuppressWarnings( "checkstyle:emptyblock" )
    public void consumeLine( String s )
    {
        items.add( s );
        if ( items.size() > ITEM_LIMIT_BEFORE_SLEEP )
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
            items.add( POISON );
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
