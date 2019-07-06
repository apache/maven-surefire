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
import org.apache.maven.surefire.booter.spi.DefaultMasterProcessChannelDecoder;
import org.apache.maven.surefire.providerapi.MasterProcessChannelDecoder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.apache.maven.plugin.surefire.booterclient.lazytestprovider.TestLessInputStream.TestLessInputStreamBuilder;
import static org.apache.maven.surefire.booter.Command.NOOP;
import static org.apache.maven.surefire.booter.Command.SKIP_SINCE_NEXT_TEST;
import static org.apache.maven.surefire.booter.MasterProcessCommand.BYE_ACK;
import static org.apache.maven.surefire.booter.MasterProcessCommand.SHUTDOWN;
import static org.apache.maven.surefire.booter.Shutdown.EXIT;
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

        builder.getCachableCommands().noop();
        assertTrue( iterator.hasNext() );
        assertThat( iterator.next(), is( NOOP ) );

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
        assertThat( is.nextCommand(), is( NOOP ) );
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
        assertThat( is.nextCommand(), is( NOOP ) );
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
        assertThat( is.nextCommand(), is( NOOP ) );
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
        TestLessInputStream pluginIs = builder.build();
        MasterProcessChannelDecoder decoder = new DefaultMasterProcessChannelDecoder( pluginIs, null );
        builder.getImmediateCommands().acknowledgeByeEventReceived();
        builder.getImmediateCommands().noop();
        Command bye = decoder.decode();
        assertThat( bye, is( notNullValue() ) );
        assertThat( bye.getCommandType(), is( BYE_ACK ) );
        Command noop = decoder.decode();
        assertThat( noop, is( notNullValue() ) );
        assertThat( noop.getCommandType(), is( MasterProcessCommand.NOOP ) );
    }
}
