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

import org.apache.maven.surefire.booter.Command;
import org.apache.maven.surefire.booter.MasterProcessCommand;

import java.io.IOException;
import java.io.InputStream;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.maven.surefire.booter.MasterProcessCommand.TEST_SET_FINISHED;
import static org.apache.maven.surefire.util.internal.StringUtils.requireNonNull;

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

    private final Queue<Command> commands;

    private final AtomicBoolean closed = new AtomicBoolean();

    private byte[] currentBuffer;

    private int currentPos;

    private MasterProcessCommand lastCommand;

    private volatile FlushReceiverProvider flushReceiverProvider;

    /**
     * C'tor
     *
     * @param commands source of the tests to be read from this stream
     */
    public TestProvidingInputStream( Queue<Command> commands )
    {
        this.commands = commands;
    }

    /**
     * @param flushReceiverProvider the provider for a flush receiver.
     */
    public void setFlushReceiverProvider( FlushReceiverProvider flushReceiverProvider )
    {
        this.flushReceiverProvider = requireNonNull( flushReceiverProvider );
    }

    public void testSetFinished()
    {
        commands.add( new Command( TEST_SET_FINISHED ) );
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
        byte[] buffer = currentBuffer;
        if ( buffer == null )
        {
            if ( flushReceiverProvider != null )
            {
                FlushReceiver flushReceiver = flushReceiverProvider.getFlushReceiver();
                if ( flushReceiver != null )
                {
                    flushReceiver.flush();
                }
            }

            if ( lastCommand == TEST_SET_FINISHED || closed.get() )
            {
                close();
                return -1;
            }

            awaitNextTest();

            if ( closed.get() )
            {
                return -1;
            }

            Command command = commands.poll();
            lastCommand = command.getCommandType();
            String test = command.getData();
            buffer = lastCommand == TEST_SET_FINISHED ? lastCommand.encode() : lastCommand.encode( test );
        }

        int b =  buffer[currentPos++] & 0xff;
        if ( currentPos == buffer.length )
        {
            buffer = null;
            currentPos = 0;
        }
        currentBuffer = buffer;
        return b;
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
            semaphore.drainPermits();
            semaphore.release();
        }
    }
}