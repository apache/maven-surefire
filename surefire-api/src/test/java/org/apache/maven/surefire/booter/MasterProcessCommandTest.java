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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringBufferInputStream;
import java.io.StringReader;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.apache.maven.surefire.booter.MasterProcessCommand.BYE_ACK;
import static org.apache.maven.surefire.booter.MasterProcessCommand.NOOP;
import static org.apache.maven.surefire.booter.MasterProcessCommand.RUN_CLASS;
import static org.apache.maven.surefire.booter.MasterProcessCommand.decode;
import static org.apache.maven.surefire.booter.MasterProcessCommand.resolve;
import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public class MasterProcessCommandTest
    extends TestCase
{

    public void testDataToByteArrayAndBack()
    {
        String dummyData = "pkg.Test";
        for ( MasterProcessCommand command : MasterProcessCommand.values() )
        {
            switch ( command )
            {
                case RUN_CLASS:
                    assertEquals( String.class, command.getDataType() );
                    byte[] encoded = command.fromDataType( dummyData );
                    assertThat( encoded.length, is( 8 ) );
                    assertThat( encoded[0], is( (byte) 'p' ) );
                    assertThat( encoded[1], is( (byte) 'k' ) );
                    assertThat( encoded[2], is( (byte) 'g' ) );
                    assertThat( encoded[3], is( (byte) '.' ) );
                    assertThat( encoded[4], is( (byte) 'T' ) );
                    assertThat( encoded[5], is( (byte) 'e' ) );
                    assertThat( encoded[6], is( (byte) 's' ) );
                    assertThat( encoded[7], is( (byte) 't' ) );
                    String decoded = command.toDataTypeAsString( encoded );
                    assertThat( decoded, is( dummyData ) );
                    break;
                case TEST_SET_FINISHED:
                    assertEquals( Void.class, command.getDataType() );
                    encoded = command.fromDataType( dummyData );
                    assertThat( encoded.length, is( 0 ) );
                    decoded = command.toDataTypeAsString( encoded );
                    assertNull( decoded );
                    break;
                case SKIP_SINCE_NEXT_TEST:
                    assertEquals( Void.class, command.getDataType() );
                    encoded = command.fromDataType( dummyData );
                    assertThat( encoded.length, is( 0 ) );
                    decoded = command.toDataTypeAsString( encoded );
                    assertNull( decoded );
                    break;
                case SHUTDOWN:
                    assertEquals( String.class, command.getDataType() );
                    encoded = command.fromDataType( Shutdown.EXIT.name() );
                    assertThat( encoded.length, is( 4 ) );
                    decoded = command.toDataTypeAsString( encoded );
                    assertThat( decoded, is( Shutdown.EXIT.name() ) );
                    break;
                case NOOP:
                    assertEquals( Void.class, command.getDataType() );
                    encoded = command.fromDataType( dummyData );
                    assertThat( encoded.length, is( 0 ) );
                    decoded = command.toDataTypeAsString( encoded );
                    assertNull( decoded );
                    break;
                case  BYE_ACK:
                    assertEquals( Void.class, command.getDataType() );
                    encoded = command.fromDataType( dummyData );
                    assertThat( encoded.length, is( 0 ) );
                    decoded = command.toDataTypeAsString( encoded );
                    assertNull( decoded );
                    break;
                default:
                    fail();
            }
            assertThat( command, is( resolve( command.getId() ) ) );
        }
    }

    public void testEncodedDecodedIsSameForRunClass()
        throws IOException
    {
        byte[] encoded = RUN_CLASS.encode( "pkg.Test" );
        assertThat( new String( encoded, US_ASCII ), is( ":maven:surefire:std:out:run-testclass:pkg.Test" ) );

        BufferedReader lineReader = new BufferedReader( new InputStreamReader( new ByteArrayInputStream( encoded ) ) );
        Command command = decode( lineReader );
        assertNotNull( command );
        assertThat( command.getCommandType(), is( RUN_CLASS ) );
        assertThat( command.getData(), is( "pkg.Test" ) );
    }

    public void testShouldDecodeTwoCommands() throws IOException
    {
        String cmd = ":maven:surefire:std:out:bye-ack\n"
                + ":maven:surefire:std:out:noop\n";
        BufferedReader lineReader = new BufferedReader( new StringReader( cmd ) );

        Command bye = decode( lineReader );
        assertNotNull( bye );
        assertThat( bye.getCommandType(), is( BYE_ACK ) );
        assertThat( bye.getData(), is( nullValue() ) );

        Command noop = decode( lineReader );
        assertNotNull( noop );
        assertThat( noop.getCommandType(), is( NOOP ) );
        assertThat( noop.getData(), is( nullValue() ) );
    }

    public void testObservingShutdownDataFailed()
    {
        try
        {
            Command.SKIP_SINCE_NEXT_TEST.toShutdownData();
            fail();
        }
        catch ( IllegalStateException e )
        {
            assertThat( e )
                    .hasMessage( "expected MasterProcessCommand.SHUTDOWN" );
        }
    }

    public void test() throws IOException
    {
        MasterProcessChannelDecoder decoder = new MasterProcessChannelDecoder();

        String cmd = ":maven:surefire:std:out:bye-ack\n";
        decoder.decode( new ByteArrayInputStream( cmd.getBytes( US_ASCII ) ) );
    }
}
