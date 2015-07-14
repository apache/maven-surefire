package org.apache.maven.plugin.surefire.booterclient.lazytestprovider;

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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.maven.surefire.util.internal.StringUtils.encodeStringForForkCommunication;

/**
 * An {@link InputStream} that, when read, provides test class names out of a queue.
 * <p/>
 * The Stream provides only one test at a time, but only after {@link #provideNewTest()} has been invoked.
 * <p/>
 * After providing each test class name, followed by a newline character, a flush is performed on the
 * {@link FlushReceiver} provided by the {@link FlushReceiverProvider} that can be set using
 * {@link #setFlushReceiverProvider(FlushReceiverProvider)}.
 *
 * @author Andreas Gudian
 */
public class TestProvidingInputStream
    extends InputStream
    implements NotifiableTestStream
{
    private final Semaphore semaphore = new Semaphore( 0 );

    private final Queue<String> testItemQueue;

    private final AtomicBoolean closed = new AtomicBoolean();

    private byte[] currentBuffer;

    private int currentPos;

    private volatile FlushReceiverProvider flushReceiverProvider;

    /**
     * C'tor
     *
     * @param testItemQueue source of the tests to be read from this stream
     */
    public TestProvidingInputStream( Queue<String> testItemQueue )
    {
        this.testItemQueue = testItemQueue;
    }

    /**
     * @param flushReceiverProvider the provider for a flush receiver.
     */
    public void setFlushReceiverProvider( FlushReceiverProvider flushReceiverProvider )
    {
        this.flushReceiverProvider = flushReceiverProvider;
    }

    /**
     * Used by single thread in StreamFeeder.
     *
     * @return {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    @SuppressWarnings( "checkstyle:magicnumber" )
    @Override
    public int read()
        throws IOException
    {
        if ( closed.get() )
        {
            // help GC to free this object because StreamFeeder Thread cannot read it after IOE
            currentBuffer = null;
            throw new EOFException( "closed unexpectedly" );
        }
        else
        {
            // isolation of instance variable in Thread stack
            byte[] buffer = currentBuffer;

            if ( buffer == null )
            {
                if ( flushReceiverProvider != null )
                {
                    FlushReceiver flushing = flushReceiverProvider.getFlushReceiver();
                    if ( flushing != null )
                    {
                        flushing.flush();
                    }
                }

                awaitNextTest();

                if ( closed.get() )
                {
                    // help GC to free this object because StreamFeeder Thread cannot read it after IOE
                    currentBuffer = null;
                    throw new EOFException( "closed unexpectedly" );
                }

                String currentElement = testItemQueue.poll();
                if ( currentElement != null )
                {
                    buffer = encodeStringForForkCommunication( currentElement );
                    // may override NULL from close(), therefore setting explicitly to NULL if IOE elsewhere
                    currentBuffer = buffer;
                    currentPos = 0;
                }
                else
                {
                    // help GC to free this object since it's not needed as there's no new test
                    currentBuffer = null;
                    return -1;
                }
            }

            if ( currentPos < buffer.length )
            {
                return buffer[currentPos++] & 0xff;
            }
            else
            {
                currentBuffer = null;
                return '\n' & 0xff;
            }
        }
    }

    private void awaitNextTest()
        throws IOException
    {
        try
        {
            semaphore.acquire();
        }
        catch ( InterruptedException e )
        {
            // help GC to free this object because StreamFeeder Thread cannot read it after IOE
            currentBuffer = null;
            throw new IOException( e.getLocalizedMessage() );
        }
    }

    /**
     * Signal that a new test is to be provided.
     */
    public void provideNewTest()
    {
        if ( !closed.get() )
        {
            semaphore.release();
        }
    }

    @Override
    public void close()
    {
        if ( closed.compareAndSet( false, true ) )
        {
            currentBuffer = null;
            int permits = semaphore.drainPermits();
            if ( permits == 0 )
            {
                semaphore.release();
            }
        }
    }
}