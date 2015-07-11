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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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
{
    private final ReentrantLock lock = new ReentrantLock();

    private final Condition lockCondition = lock.newCondition();

    private final Queue<String> testItemQueue;

    private byte[] currentBuffer;

    private int currentPos;

    private volatile FlushReceiverProvider flushReceiverProvider;

    private volatile boolean closed;

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

    @SuppressWarnings( "checkstyle:magicnumber" )
    @Override
    public int read()
        throws IOException
    {
        lock.lock();
        try
        {
            if ( closed )
            {
                throw new EOFException( "closed unexpectedly" );
            }
            else
            {
                if ( null == currentBuffer )
                {
                    if ( null != flushReceiverProvider && null != flushReceiverProvider.getFlushReceiver() )
                    {
                        flushReceiverProvider.getFlushReceiver().flush();
                    }

                    lockCondition.awaitUninterruptibly();

                    if ( closed )
                    {
                        throw new EOFException( "closed unexpectedly" );
                    }

                    String currentElement = testItemQueue.poll();
                    if ( currentElement != null )
                    {
                        currentBuffer = encodeStringForForkCommunication( currentElement );
                        currentPos = 0;
                    }
                    else
                    {
                        return -1;
                    }
                }

                if ( currentPos < currentBuffer.length )
                {
                    return currentBuffer[currentPos++] & 0xff;
                }
                else
                {
                    currentBuffer = null;
                    return '\n' & 0xff;
                }
            }
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Signal that a new test is to be provided.
     */
    public void provideNewTest()
    {
        lock.lock();
        try
        {
            lockCondition.signalAll();
        }
        finally
        {
            lock.unlock();
        }
    }

    @Override
    public void close()
    {
        closed = true;
        lock.lock();
        try
        {
            currentBuffer = null;
            lockCondition.signalAll();
        }
        finally
        {
            lock.unlock();
        }
    }
}