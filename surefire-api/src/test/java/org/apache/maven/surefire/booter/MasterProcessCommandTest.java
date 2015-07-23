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
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import static org.apache.maven.surefire.booter.MasterProcessCommand.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public class MasterProcessCommandTest
    extends TestCase
{
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    public void testEncodedStreamSequence()
    {
        byte[] streamSequence = new byte[10];
        streamSequence[8] = (byte) 'T';
        streamSequence[9] = (byte) 'e';
        setCommandAndDataLength( 256, 2, streamSequence );
        assertEquals( streamSequence[0], (byte) 0 );
        assertEquals( streamSequence[1], (byte) 0 );
        assertEquals( streamSequence[2], (byte) 1 );
        assertEquals( streamSequence[3], (byte) 0 );
        assertEquals( streamSequence[4], (byte) 0 );
        assertEquals( streamSequence[5], (byte) 0 );
        assertEquals( streamSequence[6], (byte) 0 );
        assertEquals( streamSequence[7], (byte) 2 );
        // remain unchanged
        assertEquals( streamSequence[8], (byte) 'T' );
        assertEquals( streamSequence[9], (byte) 'e' );
    }

    public void testResolved()
    {
        for ( MasterProcessCommand command : MasterProcessCommand.values() )
        {
            assertThat( command, is( resolve( command.getId() ) ) );
        }
    }

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
                case STOP_ON_NEXT_TEST:
                    assertEquals( Void.class, command.getDataType() );
                    encoded = command.fromDataType( dummyData );
                    assertThat( encoded.length, is( 0 ) );
                    decoded = command.toDataTypeAsString( encoded );
                    assertNull( decoded );
                    break;
                case SHUTDOWN:
                    assertEquals( Void.class, command.getDataType() );
                    encoded = command.fromDataType( dummyData );
                    assertThat( encoded.length, is( 0 ) );
                    decoded = command.toDataTypeAsString( encoded );
                    assertNull( decoded );
                    break;
                case NOOP:
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
        assertThat( encoded.length, is( 16 ) );
        assertThat( encoded[0], is( (byte) 0 ) );
        assertThat( encoded[1], is( (byte) 0 ) );
        assertThat( encoded[2], is( (byte) 0 ) );
        assertThat( encoded[3], is( (byte) 0 ) );
        assertThat( encoded[4], is( (byte) 0 ) );
        assertThat( encoded[5], is( (byte) 0 ) );
        assertThat( encoded[6], is( (byte) 0 ) );
        assertThat( encoded[7], is( (byte) 8 ) );
        assertThat( encoded[8], is( (byte) 'p' ) );
        assertThat( encoded[9], is( (byte) 'k' ) );
        assertThat( encoded[10], is( (byte) 'g' ) );
        assertThat( encoded[11], is( (byte) '.' ) );
        assertThat( encoded[12], is( (byte) 'T' ) );
        assertThat( encoded[13], is( (byte) 'e' ) );
        assertThat( encoded[14], is( (byte) 's' ) );
        assertThat( encoded[15], is( (byte) 't' ) );
        Command command = decode( new DataInputStream( new ByteArrayInputStream( encoded ) ) );
        assertNotNull( command );
        assertThat( command.getCommandType(), is( RUN_CLASS ) );
        assertThat( command.getData(), is( "pkg.Test" ) );
    }
}
