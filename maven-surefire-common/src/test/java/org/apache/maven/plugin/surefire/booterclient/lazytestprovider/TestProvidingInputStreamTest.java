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
import org.junit.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.apache.maven.surefire.booter.MasterProcessCommand.BYE_ACK;
import static org.apache.maven.surefire.booter.MasterProcessCommand.decode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Asserts that this stream properly reads bytes from queue.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public class TestProvidingInputStreamTest
{
    @Test
    public void closedStreamShouldReturnEndOfStream()
        throws IOException
    {
        Queue<String> commands = new ArrayDeque<>();
        TestProvidingInputStream is = new TestProvidingInputStream( commands );
        is.close();
        assertThat( is.read(), is( -1 ) );
    }

    @Test
    public void emptyStreamShouldWaitUntilClosed()
        throws Exception
    {
        Queue<String> commands = new ArrayDeque<>();
        final TestProvidingInputStream is = new TestProvidingInputStream( commands );
        final Thread streamThread = Thread.currentThread();
        FutureTask<Thread.State> futureTask = new FutureTask<>( new Callable<Thread.State>()
        {
            @Override
            public Thread.State call()
            {
                sleep( 1000 );
                Thread.State state = streamThread.getState();
                is.close();
                return state;
            }
        } );
        Thread assertionThread = new Thread( futureTask );
        assertionThread.start();
        assertThat( is.read(), is( -1 ) );
        Thread.State state = futureTask.get();
        assertThat( state, is( Thread.State.WAITING ) );
    }

    @Test
    public void finishedTestsetShouldNotBlock()
        throws IOException
    {
        Queue<String> commands = new ArrayDeque<>();
        final TestProvidingInputStream is = new TestProvidingInputStream( commands );
        is.testSetFinished();
        new Thread( new Runnable()
        {
            @Override
            public void run()
            {
                is.provideNewTest();
            }
        } ).start();
        assertThat( is.read(), is( 0 ) );
        assertThat( is.read(), is( 0 ) );
        assertThat( is.read(), is( 0 ) );
        assertThat( is.read(), is( 1 ) );
        assertThat( is.read(), is( 0 ) );
        assertThat( is.read(), is( 0 ) );
        assertThat( is.read(), is( 0 ) );
        assertThat( is.read(), is( 0 ) );
        is.close();
        assertThat( is.read(), is( -1 ) );
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
        assertThat( is.read(), is( 0 ) );
        assertThat( is.read(), is( 0 ) );
        assertThat( is.read(), is( 0 ) );
        assertThat( is.read(), is( 0 ) );
        assertThat( is.read(), is( 0 ) );
        assertThat( is.read(), is( 0 ) );
        assertThat( is.read(), is( 0 ) );
        assertThat( is.read(), is( 4 ) );
        assertThat( is.read(), is( (int) 'T' ) );
        assertThat( is.read(), is( (int) 'e' ) );
        assertThat( is.read(), is( (int) 's' ) );
        assertThat( is.read(), is( (int) 't' ) );
    }

    @Test
    public void shouldDecodeTwoCommands()
            throws IOException
    {
        TestProvidingInputStream pluginIs = new TestProvidingInputStream( new ConcurrentLinkedQueue<String>() );
        pluginIs.acknowledgeByeEventReceived();
        pluginIs.noop();
        DataInputStream is = new DataInputStream( pluginIs );
        Command bye = decode( is );
        assertThat( bye, is( notNullValue() ) );
        assertThat( bye.getCommandType(), is( BYE_ACK ) );
        Command noop = decode( is );
        assertThat( noop, is( notNullValue() ) );
        assertThat( noop.getCommandType(), is( MasterProcessCommand.NOOP ) );
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
}
