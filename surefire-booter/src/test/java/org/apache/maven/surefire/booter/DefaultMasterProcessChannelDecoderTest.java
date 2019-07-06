package org.apache.maven.surefire.booter;
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

import junit.framework.TestCase;
import org.apache.maven.surefire.booter.spi.DefaultMasterProcessChannelDecoder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.apache.maven.surefire.booter.MasterProcessCommand.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.fest.assertions.Assertions.assertThat;

/**
 * Tests for {@link DefaultMasterProcessChannelDecoder}.
 */
public class DefaultMasterProcessChannelDecoderTest
    extends TestCase
{
    public void testDataToByteArrayAndBack() throws IOException
    {
        for ( MasterProcessCommand commandType : MasterProcessCommand.values() )
        {
            switch ( commandType )
            {
                case RUN_CLASS:
                    assertEquals( String.class, commandType.getDataType() );
                    byte[] encoded = commandType.encode( "pkg.Test" );
                    assertThat( new String( encoded ) )
                            .isEqualTo( ":maven-surefire-std-out:run-testclass:pkg.Test:" );
                    byte[] line = addNL( encoded, '\n' );
                    InputStream is = new ByteArrayInputStream( line );
                    DefaultMasterProcessChannelDecoder decoder = new DefaultMasterProcessChannelDecoder( is, null );
                    Command command = decoder.decode();
                    assertThat( command.getCommandType(), is( commandType ) );
                    assertThat( command.getData(), is( "pkg.Test" ) );
                    break;
                case TEST_SET_FINISHED:
                    assertThat( commandType ).isSameAs( Command.TEST_SET_FINISHED.getCommandType() );
                    assertEquals( Void.class, commandType.getDataType() );
                    encoded = commandType.encode();
                    assertThat( new String( encoded ) )
                            .isEqualTo( ":maven-surefire-std-out:testset-finished:" );
                    is = new ByteArrayInputStream( encoded );
                    decoder = new DefaultMasterProcessChannelDecoder( is, null );
                    command = decoder.decode();
                    assertThat( command.getCommandType(), is( commandType ) );
                    assertNull( command.getData() );
                    break;
                case SKIP_SINCE_NEXT_TEST:
                    assertThat( commandType ).isSameAs( Command.SKIP_SINCE_NEXT_TEST.getCommandType() );
                    assertEquals( Void.class, commandType.getDataType() );
                    encoded = commandType.encode();
                    assertThat( new String( encoded ) )
                            .isEqualTo( ":maven-surefire-std-out:skip-since-next-test:" );
                    is = new ByteArrayInputStream( encoded );
                    decoder = new DefaultMasterProcessChannelDecoder( is, null );
                    command = decoder.decode();
                    assertThat( command.getCommandType(), is( commandType ) );
                    assertNull( command.getData() );
                    break;
                case SHUTDOWN:
                    assertEquals( String.class, commandType.getDataType() );
                    encoded = commandType.encode( Shutdown.EXIT.name() );
                    assertThat( new String( encoded ) )
                            .isEqualTo( ":maven-surefire-std-out:shutdown:EXIT:" );
                    is = new ByteArrayInputStream( encoded );
                    decoder = new DefaultMasterProcessChannelDecoder( is, null );
                    command = decoder.decode();
                    assertThat( command.getCommandType(), is( commandType ) );
                    assertThat( command.getData(), is( "EXIT" ) );
                    break;
                case NOOP:
                    assertThat( commandType ).isSameAs( Command.NOOP.getCommandType() );
                    assertEquals( Void.class, commandType.getDataType() );
                    encoded = commandType.encode();
                    assertThat( new String( encoded ) )
                            .isEqualTo( ":maven-surefire-std-out:noop:" );
                    is = new ByteArrayInputStream( encoded );
                    decoder = new DefaultMasterProcessChannelDecoder( is, null );
                    command = decoder.decode();
                    assertThat( command.getCommandType(), is( commandType ) );
                    assertNull( command.getData() );
                    break;
                case BYE_ACK:
                    assertThat( commandType ).isSameAs( Command.BYE_ACK.getCommandType() );
                    assertEquals( Void.class, commandType.getDataType() );
                    encoded = commandType.encode();
                    assertThat( new String( encoded ) )
                            .isEqualTo( ":maven-surefire-std-out:bye-ack:" );
                    is = new ByteArrayInputStream( encoded );
                    decoder = new DefaultMasterProcessChannelDecoder( is, null );
                    command = decoder.decode();
                    assertThat( command.getCommandType(), is( commandType ) );
                    assertNull( command.getData() );
                    break;
                default:
                    fail();
            }
        }
    }

    public void testShouldDecodeTwoCommands() throws IOException
    {
        String cmd = ":maven-surefire-std-out:bye-ack\n:maven-surefire-std-out:bye-ack:";
        InputStream is = new ByteArrayInputStream( cmd.getBytes() );
        DefaultMasterProcessChannelDecoder decoder = new DefaultMasterProcessChannelDecoder( is, null );

        Command command = decoder.decode();
        assertThat( command.getCommandType() ).isEqualTo( BYE_ACK );
        assertThat( command.getData() ).isNull();

        command = decoder.decode();
        assertThat( command.getCommandType() ).isEqualTo( BYE_ACK );
        assertThat( command.getData() ).isNull();
    }

    private static byte[] addNL( byte[] encoded, char... newLines )
    {
        byte[] line = new byte[encoded.length + newLines.length];
        System.arraycopy( encoded, 0, line, 0, encoded.length );
        for ( int i = encoded.length, j = 0; i < line.length; i++, j++ )
        {
            line[i] = (byte) newLines[j];
        }
        return line;
    }
}
