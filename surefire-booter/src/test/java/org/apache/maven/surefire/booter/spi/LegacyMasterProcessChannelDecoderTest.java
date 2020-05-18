package org.apache.maven.surefire.booter.spi;

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
import org.apache.maven.surefire.api.booter.Shutdown;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import static java.nio.channels.Channels.newChannel;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.BYE_ACK;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.NOOP;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.RUN_CLASS;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.SHUTDOWN;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.SKIP_SINCE_NEXT_TEST;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.TEST_SET_FINISHED;
import static org.apache.maven.surefire.api.booter.Shutdown.DEFAULT;
import static org.apache.maven.surefire.api.booter.Shutdown.EXIT;
import static org.apache.maven.surefire.api.booter.Shutdown.KILL;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Tests for {@link LegacyMasterProcessChannelDecoder}.
 */
public class LegacyMasterProcessChannelDecoderTest
{
    @Test
    public void testDecoderRunClass() throws IOException
    {
        assertEquals( String.class, RUN_CLASS.getDataType() );
        byte[] encoded = ":maven-surefire-command:run-testclass:pkg.Test:\n".getBytes();
        InputStream is = new ByteArrayInputStream( encoded );
        LegacyMasterProcessChannelDecoder decoder = new LegacyMasterProcessChannelDecoder( newChannel( is ) );
        Command command = decoder.decode();
        assertThat( command.getCommandType() ).isSameAs( RUN_CLASS );
        assertThat( command.getData() ).isEqualTo( "pkg.Test" );
    }

    @Test
    public void testDecoderTestsetFinished() throws IOException
    {
        Command command = Command.TEST_SET_FINISHED;
        assertThat( command.getCommandType() ).isSameAs( TEST_SET_FINISHED );
        assertEquals( Void.class, TEST_SET_FINISHED.getDataType() );
        byte[] encoded = ":maven-surefire-command:testset-finished:".getBytes();
        ByteArrayInputStream is = new ByteArrayInputStream( encoded );
        LegacyMasterProcessChannelDecoder decoder = new LegacyMasterProcessChannelDecoder( newChannel( is ) );
        command = decoder.decode();
        assertThat( command.getCommandType() ).isSameAs( TEST_SET_FINISHED );
        assertNull( command.getData() );
    }

    @Test
    public void testDecoderSkipSinceNextTest() throws IOException
    {
        Command command = Command.SKIP_SINCE_NEXT_TEST;
        assertThat( command.getCommandType() ).isSameAs( SKIP_SINCE_NEXT_TEST );
        assertEquals( Void.class, SKIP_SINCE_NEXT_TEST.getDataType() );
        byte[] encoded = ":maven-surefire-command:skip-since-next-test:".getBytes();
        ByteArrayInputStream is = new ByteArrayInputStream( encoded );
        LegacyMasterProcessChannelDecoder decoder = new LegacyMasterProcessChannelDecoder( newChannel( is ) );
        command = decoder.decode();
        assertThat( command.getCommandType() ).isSameAs( SKIP_SINCE_NEXT_TEST );
        assertNull( command.getData() );
    }

    @Test
    public void testDecoderShutdownWithExit() throws IOException
    {
        Shutdown shutdownType = EXIT;
        assertEquals( String.class, SHUTDOWN.getDataType() );
        byte[] encoded = ( ":maven-surefire-command:shutdown:" + shutdownType + ":" ).getBytes();
        ByteArrayInputStream is = new ByteArrayInputStream( encoded );
        LegacyMasterProcessChannelDecoder decoder = new LegacyMasterProcessChannelDecoder( newChannel( is ) );
        Command command = decoder.decode();
        assertThat( command.getCommandType() ).isSameAs( SHUTDOWN );
        assertThat( command.getData() ).isEqualTo( shutdownType.name() );
    }

    @Test
    public void testDecoderShutdownWithKill() throws IOException
    {
        Shutdown shutdownType = KILL;
        assertEquals( String.class, SHUTDOWN.getDataType() );
        byte[] encoded = ( ":maven-surefire-command:shutdown:" + shutdownType + ":" ).getBytes();
        ByteArrayInputStream is = new ByteArrayInputStream( encoded );
        LegacyMasterProcessChannelDecoder decoder = new LegacyMasterProcessChannelDecoder( newChannel( is ) );
        Command command = decoder.decode();
        assertThat( command.getCommandType() ).isSameAs( SHUTDOWN );
        assertThat( command.getData() ).isEqualTo( shutdownType.name() );
    }

    @Test
    public void testDecoderShutdownWithDefault() throws IOException
    {
        Shutdown shutdownType = DEFAULT;
        assertEquals( String.class, SHUTDOWN.getDataType() );
        byte[] encoded = ( ":maven-surefire-command:shutdown:" + shutdownType + ":" ).getBytes();
        ByteArrayInputStream is = new ByteArrayInputStream( encoded );
        LegacyMasterProcessChannelDecoder decoder = new LegacyMasterProcessChannelDecoder( newChannel( is ) );
        Command command = decoder.decode();
        assertThat( command.getCommandType() ).isSameAs( SHUTDOWN );
        assertThat( command.getData() ).isEqualTo( shutdownType.name() );
    }

    @Test
    public void testDecoderNoop() throws IOException
    {
        assertThat( NOOP ).isSameAs( Command.NOOP.getCommandType() );
        assertEquals( Void.class, NOOP.getDataType() );
        byte[] encoded = ":maven-surefire-command:noop:".getBytes();
        ByteArrayInputStream is = new ByteArrayInputStream( encoded );
        LegacyMasterProcessChannelDecoder decoder = new LegacyMasterProcessChannelDecoder( newChannel( is ) );
        Command command = decoder.decode();
        assertThat( command.getCommandType() ).isSameAs( NOOP );
        assertNull( command.getData() );
    }

    @Test
    public void shouldIgnoreDamagedStream() throws IOException
    {
        assertThat( BYE_ACK ).isSameAs( Command.BYE_ACK.getCommandType() );
        assertEquals( Void.class, BYE_ACK.getDataType() );
        byte[] encoded = ":maven-surefire-command:bye-ack:".getBytes();
        byte[] streamContent = ( "<something>" + new String( encoded ) + "<damaged>" ).getBytes();
        ByteArrayInputStream is = new ByteArrayInputStream( streamContent );
        LegacyMasterProcessChannelDecoder decoder = new LegacyMasterProcessChannelDecoder( newChannel( is ) );
        Command command = decoder.decode();
        assertThat( command.getCommandType() ).isSameAs( BYE_ACK );
        assertNull( command.getData() );
    }

    @Test
    public void shouldIgnoreDamagedHeader() throws IOException
    {
        assertThat( BYE_ACK ).isSameAs( Command.BYE_ACK.getCommandType() );
        assertEquals( Void.class, BYE_ACK.getDataType() );
        byte[] encoded = ":maven-surefire-command:bye-ack:".getBytes();
        byte[] streamContent = ( ":<damaged>:" + new String( encoded ) ).getBytes();
        ByteArrayInputStream is = new ByteArrayInputStream( streamContent );
        LegacyMasterProcessChannelDecoder decoder = new LegacyMasterProcessChannelDecoder( newChannel( is ) );
        Command command = decoder.decode();
        assertThat( command.getCommandType() ).isSameAs( BYE_ACK );
        assertNull( command.getData() );
    }

    @Test
    public void testDecoderByeAck() throws IOException
    {
        assertThat( BYE_ACK ).isSameAs( Command.BYE_ACK.getCommandType() );
        assertEquals( Void.class, BYE_ACK.getDataType() );
        byte[] encoded = ":maven-surefire-command:bye-ack:".getBytes();
        ByteArrayInputStream is = new ByteArrayInputStream( encoded );
        LegacyMasterProcessChannelDecoder decoder = new LegacyMasterProcessChannelDecoder( newChannel( is ) );
        Command command = decoder.decode();
        assertThat( command.getCommandType() ).isSameAs( BYE_ACK );
        assertNull( command.getData() );
    }

    @Test
    public void shouldDecodeTwoCommands() throws IOException
    {
        String cmd = ":maven-surefire-command:bye-ack:\r\n:maven-surefire-command:bye-ack:";
        InputStream is = new ByteArrayInputStream( cmd.getBytes() );
        LegacyMasterProcessChannelDecoder decoder = new LegacyMasterProcessChannelDecoder( newChannel( is ) );

        Command command = decoder.decode();
        assertThat( command.getCommandType() ).isEqualTo( BYE_ACK );
        assertThat( command.getData() ).isNull();

        command = decoder.decode();
        assertThat( command.getCommandType() ).isEqualTo( BYE_ACK );
        assertThat( command.getData() ).isNull();

        decoder.close();
    }

    @Test( expected = EOFException.class )
    public void testIncompleteCommand() throws IOException
    {

        ByteArrayInputStream is = new ByteArrayInputStream( ":maven-surefire-command:".getBytes() );
        LegacyMasterProcessChannelDecoder decoder = new LegacyMasterProcessChannelDecoder( newChannel( is ) );
        decoder.decode();
        fail();
    }

    @Test( expected = EOFException.class )
    public void testIncompleteCommandStart() throws IOException
    {

        ByteArrayInputStream is = new ByteArrayInputStream( new byte[] {':', '\r'} );
        LegacyMasterProcessChannelDecoder decoder = new LegacyMasterProcessChannelDecoder( newChannel( is ) );
        decoder.decode();
        fail();
    }

    @Test( expected = EOFException.class )
    public void shouldNotDecodeCorruptedCommand() throws IOException
    {
        String cmd = ":maven-surefire-command:bye-ack ::maven-surefire-command:";
        InputStream is = new ByteArrayInputStream( cmd.getBytes() );
        LegacyMasterProcessChannelDecoder decoder = new LegacyMasterProcessChannelDecoder( newChannel( is ) );

        decoder.decode();
    }

    @Test
    public void shouldSkipCorruptedCommand() throws IOException
    {
        String cmd = ":maven-surefire-command:bye-ack\r\n::maven-surefire-command:noop:";
        InputStream is = new ByteArrayInputStream( cmd.getBytes() );
        LegacyMasterProcessChannelDecoder decoder = new LegacyMasterProcessChannelDecoder( newChannel( is ) );

        Command command = decoder.decode();
        assertThat( command.getCommandType() ).isSameAs( NOOP );
        assertNull( command.getData() );
    }
}
