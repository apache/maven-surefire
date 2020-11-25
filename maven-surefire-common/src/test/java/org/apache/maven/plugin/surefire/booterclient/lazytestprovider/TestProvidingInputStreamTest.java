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

import org.apache.maven.surefire.api.booter.Command;
import org.apache.maven.surefire.api.booter.MasterProcessChannelDecoder;
import org.apache.maven.surefire.booter.ForkedNodeArg;
import org.apache.maven.surefire.booter.spi.CommandChannelDecoder;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.State;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static java.nio.channels.Channels.newChannel;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.maven.surefire.api.booter.Command.TEST_SET_FINISHED;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.BYE_ACK;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.NOOP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Asserts that this stream properly reads bytes from queue.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public class TestProvidingInputStreamTest
{
    private static final int WAIT_LOOPS = 100;
    @Test
    public void closedStreamShouldReturnNullAsEndOfStream()
        throws IOException
    {
        Queue<String> commands = new ArrayDeque<>();
        TestProvidingInputStream is = new TestProvidingInputStream( commands );
        is.close();
        assertThat( is.readNextCommand(), is( nullValue() ) );
    }

    @Test
    public void emptyStreamShouldWaitUntilClosed()
        throws Exception
    {
        Queue<String> commands = new ArrayDeque<>();
        final TestProvidingInputStream is = new TestProvidingInputStream( commands );
        final Thread streamThread = Thread.currentThread();
        FutureTask<State> futureTask = new FutureTask<>( new Callable<State>()
        {
            @Override
            public State call()
            {
                sleep( 1000L );
                State state = streamThread.getState();
                is.close();
                return state;
            }
        } );
        Thread assertionThread = new Thread( futureTask );
        assertionThread.start();
        assertThat( is.readNextCommand(), is( nullValue() ) );
        State state = futureTask.get();
        assertThat( state, is( State.WAITING ) );
    }

    @Test
    public void finishedTestsetShouldNotBlock()
        throws IOException
    {
        final TestProvidingInputStream is = new TestProvidingInputStream( new ArrayDeque<String>() );
        is.testSetFinished();
        new Thread( new Runnable()
        {
            @Override
            public void run()
            {
                is.provideNewTest();
            }
        } ).start();

        for ( int i = 0; i < 2; i++ )
        {
            Command cmd = is.readNextCommand();
            assertThat( cmd.getData(), is( nullValue() ) );
            assertThat( cmd, is( TEST_SET_FINISHED ) );
        }

        boolean emptyStream = isInputStreamEmpty( is );

        is.close();
        assertTrue( emptyStream );
        assertThat( is.readNextCommand(), is( nullValue() ) );
    }

    @Test
    public void shouldReadTest()
        throws IOException
    {
        Queue<String> commands = new ArrayDeque<>();
        commands.add( "Test" );
        final TestProvidingInputStream is = new TestProvidingInputStream( commands );
        new Thread( new Runnable()
        {
            @Override
            public void run()
            {
                is.provideNewTest();
            }
        } ).start();

        Command cmd = is.readNextCommand();
        assertThat( cmd.getData(), is( "Test" ) );

        is.close();
    }

    @Test
    public void shouldDecodeTwoCommands()
            throws IOException
    {
        final TestProvidingInputStream pluginIs = new TestProvidingInputStream( new ConcurrentLinkedQueue<String>() );
        InputStream is = new InputStream()
        {
            private byte[] buffer;
            private int idx;

            @Override
            public int read() throws IOException
            {
                if ( buffer == null )
                {
                    idx = 0;
                    Command cmd = pluginIs.readNextCommand();
                    if ( cmd != null )
                    {
                        if ( cmd.getCommandType() == BYE_ACK )
                        {
                            buffer = ":maven-surefire-command:\u0007:bye-ack:".getBytes( UTF_8 );
                        }
                        else if ( cmd.getCommandType() == NOOP )
                        {
                            buffer = ":maven-surefire-command:\u0004:noop:".getBytes( UTF_8 );
                        }
                        else
                        {
                            fail();
                        }
                    }
                }

                if ( buffer != null )
                {
                    byte b = buffer[idx++];
                    if ( idx == buffer.length )
                    {
                        buffer = null;
                        idx = 0;
                    }
                    return b;
                }
                throw new IOException();
            }
        };
        MasterProcessChannelDecoder decoder =
            new CommandChannelDecoder( newChannel( is ), new ForkedNodeArg( 1, false ) );
        pluginIs.acknowledgeByeEventReceived();
        pluginIs.noop();
        Command bye = decoder.decode();
        assertThat( bye, is( notNullValue() ) );
        assertThat( bye.getCommandType(), is( BYE_ACK ) );
        Command noop = decoder.decode();
        assertThat( noop, is( notNullValue() ) );
        assertThat( noop.getCommandType(), is( NOOP ) );
    }

    private static void sleep( long millis )
    {
        try
        {
            TimeUnit.MILLISECONDS.sleep( millis );
        }
        catch ( InterruptedException e )
        {
            // do nothing
        }
    }

    /**
     * Waiting (max of 20 seconds)
     * @param is examined stream
     * @return {@code true} if the {@link InputStream#read()} is waiting for a new byte.
     */
    private static boolean isInputStreamEmpty( final TestProvidingInputStream is )
    {
        Thread t = new Thread( new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    is.readNextCommand();
                }
                catch ( IOException e )
                {
                    Throwable cause = e.getCause();
                    Throwable err = cause == null ? e : cause;
                    if ( !( err instanceof InterruptedException ) )
                    {
                        System.err.println( err.toString() );
                    }
                }
            }
        } );
        t.start();
        State state;
        int loops = 0;
        do
        {
            sleep( 100L );
            state = t.getState();
        }
        while ( state == State.NEW && loops++ < WAIT_LOOPS );
        t.interrupt();
        return state == State.WAITING || state == State.TIMED_WAITING;
    }
}
