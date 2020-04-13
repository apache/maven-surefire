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
import org.apache.maven.surefire.booter.spi.LegacyMasterProcessChannelDecoder;
import org.apache.maven.surefire.booter.MasterProcessChannelDecoder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static java.nio.channels.Channels.newChannel;
import static org.apache.maven.plugin.surefire.booterclient.lazytestprovider.TestLessInputStream.TestLessInputStreamBuilder;
import static org.apache.maven.surefire.booter.Command.SKIP_SINCE_NEXT_TEST;
import static org.apache.maven.surefire.booter.MasterProcessCommand.SHUTDOWN;
import static org.apache.maven.surefire.booter.Shutdown.EXIT;
import static org.apache.maven.surefire.booter.Shutdown.KILL;
import static org.apache.maven.plugin.surefire.extensions.StreamFeeder.encode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Testing cached and immediate commands in {@link TestLessInputStream}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public class TestLessInputStreamBuilderTest
{
    @Rule
    public final ExpectedException e = ExpectedException.none();

    @Test
    public void cachableCommandsShouldBeIterableWithStillOpenIterator()
    {
        TestLessInputStreamBuilder builder = new TestLessInputStreamBuilder();
        TestLessInputStream is = builder.build();
        Iterator<Command> iterator = builder.getIterableCachable().iterator();

        assertFalse( iterator.hasNext() );

        builder.getCachableCommands().skipSinceNextTest();
        assertTrue( iterator.hasNext() );
        assertThat( iterator.next(), is( SKIP_SINCE_NEXT_TEST ) );

        assertFalse( iterator.hasNext() );

        builder.getCachableCommands().shutdown( KILL );
        assertTrue( iterator.hasNext() );
        assertThat( iterator.next(), is( new Command( SHUTDOWN, "KILL" ) ) );

        builder.removeStream( is );
    }

    @Test
    public void immediateCommands()
        throws IOException
    {
        TestLessInputStreamBuilder builder = new TestLessInputStreamBuilder();
        TestLessInputStream is = builder.build();
        assertThat( is.availablePermits(), is( 0 ) );
        is.noop();
        assertThat( is.availablePermits(), is( 1 ) );
        is.beforeNextCommand();
        assertThat( is.availablePermits(), is( 0 ) );
        assertThat( is.nextCommand(), is( Command.NOOP ) );
        assertThat( is.availablePermits(), is( 0 ) );
        e.expect( NoSuchElementException.class );
        is.nextCommand();
    }

    @Test
    public void combinedCommands()
        throws IOException
    {
        TestLessInputStreamBuilder builder = new TestLessInputStreamBuilder();
        TestLessInputStream is = builder.build();
        assertThat( is.availablePermits(), is( 0 ) );
        builder.getCachableCommands().skipSinceNextTest();
        is.noop();
        assertThat( is.availablePermits(), is( 2 ) );
        is.beforeNextCommand();
        assertThat( is.availablePermits(), is( 1 ) );
        assertThat( is.nextCommand(), is( Command.NOOP ) );
        assertThat( is.availablePermits(), is( 1 ) );
        builder.getCachableCommands().skipSinceNextTest();
        assertThat( is.availablePermits(), is( 1 ) );
        builder.getImmediateCommands().shutdown( EXIT );
        assertThat( is.availablePermits(), is( 2 ) );
        is.beforeNextCommand();
        assertThat( is.nextCommand().getCommandType(), is( SHUTDOWN ) );
        assertThat( is.availablePermits(), is( 1 ) );
        is.beforeNextCommand();
        assertThat( is.nextCommand(), is( SKIP_SINCE_NEXT_TEST ) );
        assertThat( is.availablePermits(), is( 0 ) );
        builder.getImmediateCommands().noop();
        assertThat( is.availablePermits(), is( 1 ) );
        builder.getCachableCommands().shutdown( EXIT );
        builder.getCachableCommands().shutdown( EXIT );
        assertThat( is.availablePermits(), is( 2 ) );
        is.beforeNextCommand();
        assertThat( is.nextCommand(), is( Command.NOOP ) );
        assertThat( is.availablePermits(), is( 1 ) );
        is.beforeNextCommand();
        assertThat( is.nextCommand().getCommandType(), is( SHUTDOWN ) );
        assertThat( is.availablePermits(), is( 0 ) );
        e.expect( NoSuchElementException.class );
        is.nextCommand();
    }

    @Test
    public void shouldDecodeTwoCommands()
            throws IOException
    {
        TestLessInputStreamBuilder builder = new TestLessInputStreamBuilder();
        final TestLessInputStream pluginIs = builder.build();
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
                        MasterProcessCommand cmdType = cmd.getCommandType();
                        buffer = cmdType.hasDataType() ? encode( cmdType, cmd.getData() ) : encode( cmdType );
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
        MasterProcessChannelDecoder decoder = new LegacyMasterProcessChannelDecoder( newChannel( is ) );
        builder.getImmediateCommands().shutdown( KILL );
        builder.getImmediateCommands().noop();
        Command bye = decoder.decode();
        assertThat( bye, is( notNullValue() ) );
        assertThat( bye.getCommandType(), is( SHUTDOWN ) );
        assertThat( bye.getData(), is( KILL.name() ) );
        Command noop = decoder.decode();
        assertThat( noop, is( notNullValue() ) );
        assertThat( noop.getCommandType(), is( MasterProcessCommand.NOOP ) );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldThrowUnsupportedException1()
    {
        TestLessInputStreamBuilder builder = new TestLessInputStreamBuilder();
        builder.getImmediateCommands().provideNewTest();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldThrowUnsupportedException2()
    {
        TestLessInputStreamBuilder builder = new TestLessInputStreamBuilder();
        builder.getImmediateCommands().skipSinceNextTest();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldThrowUnsupportedException3()
    {
        TestLessInputStreamBuilder builder = new TestLessInputStreamBuilder();
        builder.getImmediateCommands().acknowledgeByeEventReceived();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldThrowUnsupportedException4()
    {
        TestLessInputStreamBuilder builder = new TestLessInputStreamBuilder();
        builder.getCachableCommands().acknowledgeByeEventReceived();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldThrowUnsupportedException5()
    {
        TestLessInputStreamBuilder builder = new TestLessInputStreamBuilder();
        builder.getCachableCommands().provideNewTest();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldThrowUnsupportedException6()
    {
        TestLessInputStreamBuilder builder = new TestLessInputStreamBuilder();
        builder.getCachableCommands().noop();
    }
}
