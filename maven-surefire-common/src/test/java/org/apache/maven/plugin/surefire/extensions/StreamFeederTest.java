package org.apache.maven.plugin.surefire.extensions;

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

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.api.booter.Command;
import org.apache.maven.surefire.extensions.CommandReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;

import static java.util.Arrays.asList;
import static org.apache.maven.surefire.api.booter.Command.TEST_SET_FINISHED;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.RUN_CLASS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link StreamFeeder}.
 */
public class StreamFeederTest
{
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final WritableByteChannel channel = mock( WritableByteChannel.class );
    private final CommandReader commandReader = mock( CommandReader.class );
    private StreamFeeder streamFeeder;

    @Before
    public void setup() throws IOException
    {
        final Iterator<Command> it = asList( new Command( RUN_CLASS, "pkg.ATest" ), TEST_SET_FINISHED ).iterator();
        when( commandReader.readNextCommand() )
            .thenAnswer( new Answer<Command>()
            {
                @Override
                public Command answer( InvocationOnMock invocation )
                {
                    return it.hasNext() ? it.next() : null;
                }
            } );
    }

    @After
    public void close() throws IOException
    {
        if ( streamFeeder != null )
        {
            streamFeeder.disable();
            streamFeeder.close();
        }
    }

    @Test
    public void shouldEncodeCommandToStream() throws Exception
    {
        when( channel.write( any( ByteBuffer.class ) ) )
            .thenAnswer( new Answer<Object>()
            {
                @Override
                public Object answer( InvocationOnMock invocation ) throws IOException
                {
                    ByteBuffer bb = invocation.getArgument( 0 );
                    ( (Buffer) bb ).flip();
                    out.write( bb.array(), 0, ( (Buffer) bb ).limit() );
                    return ( (Buffer) bb ).limit();
                }
            } );

        ConsoleLogger logger = mock( ConsoleLogger.class );
        streamFeeder = new StreamFeeder( "t", channel, commandReader, logger );
        streamFeeder.start();

        streamFeeder.join();
        String commands = out.toString();

        String expected = new StringBuilder()
            .append( ":maven-surefire-command:" )
            .append( (char) 13 )
            .append( ":run-testclass:" )
            .append( (char) 5 )
            .append( ":UTF-8:" )
            .append( (char) 0 )
            .append( (char) 0 )
            .append( (char) 0 )
            .append( (char) 9 )
            .append( ":" )
            .append( "pkg.ATest" )
            .append( ":" )
            .append( ":maven-surefire-command:" )
            .append( (char) 16 )
            .append( ":testset-finished:" )
            .toString();

        assertThat( commands )
            .isEqualTo( expected );

        verify( channel, times( 1 ) )
            .close();

        assertThat( streamFeeder.getException() )
            .isNull();

        verifyZeroInteractions( logger );
    }

    @Test
    public void shouldFailThread() throws Exception
    {
        when( channel.write( any( ByteBuffer.class ) ) )
            .thenAnswer( new Answer<Object>()
            {
                @Override
                public Object answer( InvocationOnMock invocation ) throws IOException
                {
                    throw new IOException();
                }
            } );

        ConsoleLogger logger = mock( ConsoleLogger.class );
        streamFeeder = new StreamFeeder( "t", channel, commandReader, logger );
        streamFeeder.start();

        streamFeeder.join();

        assertThat( out.size() )
            .isZero();

        verify( channel, times( 1 ) )
            .close();

        assertThat( streamFeeder.getException() )
            .isNotNull()
            .isInstanceOf( IOException.class );

        verifyZeroInteractions( logger );
    }
}
