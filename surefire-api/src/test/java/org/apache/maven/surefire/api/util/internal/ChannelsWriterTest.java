package org.apache.maven.surefire.api.util.internal;

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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ShutdownChannelGroupException;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static java.nio.file.Files.readAllBytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * The tests for {@link Channels#newChannel(OutputStream)} and {@link Channels#newBufferedChannel(OutputStream)}.
 */
public class ChannelsWriterTest
{
    @Rule
    public final ExpectedException ee = ExpectedException.none();

    @Rule
    public final TemporaryFolder tmp = TemporaryFolder.builder()
        .assureDeletion()
        .build();

    @Test
    public void wrappedBuffer() throws Exception
    {
        final boolean[] isFlush = {false};
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        {
            @Override
            public void flush() throws IOException
            {
                isFlush[0] = true;
                super.flush();
            }
        };
        WritableByteChannel channel = Channels.newBufferedChannel( out );
        ByteBuffer bb = ByteBuffer.wrap( new byte[] {1, 2, 3} );
        int countWritten = channel.write( bb );
        assertThat( countWritten )
            .isEqualTo( 3 );

        assertThat( out.toByteArray() )
            .hasSize( 3 )
            .isEqualTo( new byte[] {1, 2, 3} );

        assertThat( isFlush )
            .hasSize( 1 )
            .containsOnly( true );

        assertThat( ( (Buffer) bb ).position() )
            .isEqualTo( 3 );

        assertThat( ( (Buffer) bb ).limit() )
            .isEqualTo( 3 );

        assertThat( bb.capacity() )
            .isEqualTo( 3 );

        assertThat( channel.isOpen() )
            .isTrue();
    }

    @Test
    public void bigBuffer() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WritableByteChannel channel = Channels.newChannel( out );
        ByteBuffer bb = ByteBuffer.allocate( 4 );
        bb.put( (byte) 1 );
        bb.put( (byte) 2 );
        bb.put( (byte) 3 );
        int countWritten = channel.write( bb );
        assertThat( countWritten ).isEqualTo( 3 );
        assertThat( out.toByteArray() )
            .hasSize( 3 )
            .isEqualTo( new byte[] {1, 2, 3} );

        assertThat( ( (Buffer) bb ).position() )
            .isEqualTo( 3 );

        assertThat( ( (Buffer) bb ).limit() )
            .isEqualTo( 3 );

        assertThat( bb.capacity() )
            .isEqualTo( 4 );

        assertThat( channel.isOpen() )
            .isTrue();
    }

    @Test
    public void shouldFlushWhenEmptyBuffer() throws Exception
    {
        final boolean[] flushed = {false};
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        {
            @Override
            public void flush() throws IOException
            {
                flushed[0] = true;
                super.flush();
            }
        };
        WritableByteChannel channel = Channels.newChannel( out );
        ByteBuffer bb = ByteBuffer.allocate( 0 );
        int countWritten = channel.write( bb );
        assertThat( countWritten )
            .isEqualTo( 0 );
        assertThat( flushed[0] )
            .isTrue();
    }

    @Test
    public void shouldFlushWhenEmptyBufferOnBufferedWrites() throws Exception
    {
        final boolean[] flushed = {false};
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        {
            @Override
            public void flush() throws IOException
            {
                flushed[0] = true;
                super.flush();
            }
        };
        WritableBufferedByteChannel channel = Channels.newBufferedChannel( out );
        ByteBuffer bb = ByteBuffer.allocate( 0 );
        channel.writeBuffered( bb );
        assertThat( flushed[0] )
            .isFalse();
    }

    @Test
    public void bufferedChannel() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WritableBufferedByteChannel channel = Channels.newBufferedChannel( out );
        ByteBuffer bb = ByteBuffer.allocate( 5 );
        bb.put( (byte) 1 );
        bb.put( (byte) 2 );
        bb.put( (byte) 3 );

        channel.writeBuffered( bb );

        assertThat( out.toByteArray() )
            .isEmpty();

        channel.write( ByteBuffer.wrap( new byte[] {4} ) );

        assertThat( out.toByteArray() )
            .hasSize( 4 )
            .isEqualTo( new byte[] {1, 2, 3, 4} );

        assertThat( ( (Buffer) bb ).position() )
            .isEqualTo( 3 );

        assertThat( ( (Buffer) bb ).limit() )
            .isEqualTo( 3 );

        assertThat( bb.capacity() )
            .isEqualTo( 5 );

        assertThat( channel.isOpen() )
            .isTrue();
    }

    @Test
    public void shouldFailAfterClosed() throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WritableByteChannel channel = Channels.newChannel( out );
        channel.close();
        assertThat( channel.isOpen() ).isFalse();
        ee.expect( ClosedChannelException.class );
        channel.write( ByteBuffer.allocate( 0 ) );
    }

    @Test
    public void shouldFailIfNotReadable() throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WritableByteChannel channel = Channels.newChannel( out );
        ee.expect( NonWritableChannelException.class );
        channel.write( ByteBuffer.allocate( 0 ).asReadOnlyBuffer() );
    }

    @Test
    public void shouldFailIOnDirectBuffer() throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WritableByteChannel channel = Channels.newChannel( out );
        ee.expect( NonWritableChannelException.class );
        channel.write( ByteBuffer.allocateDirect( 0 ) );
    }

    @Test
    public void shouldUseFileChannel() throws IOException
    {
        File f = tmp.newFile();
        FileOutputStream os = new FileOutputStream( f );
        WritableByteChannel channel = Channels.newChannel( os );
        ByteBuffer bb = ByteBuffer.wrap( new byte[] {1, 2, 3} );
        channel.write( bb );

        assertThat( channel.isOpen() )
            .isTrue();

        channel.close();

        assertThat( channel.isOpen() )
            .isFalse();

        assertThat( readAllBytes( f.toPath() ) )
            .hasSize( 3 )
            .isEqualTo( new byte[] {1, 2, 3} );
    }

    @Test( expected = IndexOutOfBoundsException.class )
    public void shouldValidateInput1() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        OutputStream os = Channels.newOutputStream( channel );
        os.write( new byte[0], -1, 0 );
    }

    @Test( expected = IndexOutOfBoundsException.class )
    public void shouldValidateInput2() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        OutputStream os = Channels.newOutputStream( channel );
        os.write( new byte[0], 0, -1 );
    }

    @Test( expected = IndexOutOfBoundsException.class )
    public void shouldValidateInput3() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        OutputStream os = Channels.newOutputStream( channel );
        os.write( new byte[0], 1, 0 );
    }

    @Test( expected = IndexOutOfBoundsException.class )
    public void shouldValidateInput4() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        OutputStream os = Channels.newOutputStream( channel );
        os.write( new byte[0], 0, 1 );
    }

    @Test
    public void shouldClose() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        when( channel.isOpen() ).thenReturn( true );
        OutputStream os = Channels.newOutputStream( channel );
        os.close();
        verify( channel, times( 1 ) ).close();
    }

    @Test
    public void shouldNotClose() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        when( channel.isOpen() ).thenReturn( false );
        OutputStream os = Channels.newOutputStream( channel );
        os.close();
        verify( channel, never() ).close();
    }

    @Test
    public void shouldAlreadyClosed() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        when( channel.isOpen() ).thenReturn( true );
        doThrow( ClosedChannelException.class ).when( channel ).close();
        OutputStream os = Channels.newOutputStream( channel );
        os.close();
        verify( channel ).close();
    }

    @Test
    public void shouldWriteZeroLength() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        OutputStream os = Channels.newOutputStream( channel );
        os.write( new byte[] { 5 }, 0, 0 );
        verifyZeroInteractions( channel );
    }

    @Test
    public void shouldWriteArray() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        when( channel.write( any( ByteBuffer.class ) ) )
            .thenAnswer( new Answer<Future<Integer>>()
            {
                @Override
                public Future<Integer> answer( InvocationOnMock invocation ) throws Throwable
                {
                    ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
                    int i = 0;
                    for ( ; bb.hasRemaining(); i++ )
                    {
                        bb.get();
                    }
                    Future<Integer> future = mock( Future.class );
                    when( future.get() ).thenReturn( i );
                    return future;
                }
            } );

        OutputStream os = Channels.newOutputStream( channel );
        ArgumentCaptor<ByteBuffer> captured = ArgumentCaptor.forClass( ByteBuffer.class );
        os.write( new byte[] { 1, 2, 3, 4, 5 }, 2, 2 );

        verify( channel ).write( captured.capture() );
        verifyNoMoreInteractions( channel );

        assertThat( captured.getAllValues() )
            .hasSize( 1 );

        assertThat( captured.getAllValues().get( 0 ).array() )
            .containsOnly( new byte[] { 1, 2, 3, 4, 5 } );

        assertThat( captured.getAllValues().get( 0 ).arrayOffset() )
            .isEqualTo( 0 );

        assertThat( captured.getAllValues().get( 0 ).position() )
            .isEqualTo( 4 );

        assertThat( captured.getAllValues().get( 0 ).limit() )
            .isEqualTo( 4 );

        assertThat( captured.getAllValues().get( 0 ).capacity() )
            .isEqualTo( 5 );
    }

    @Test
    public void shouldWrite() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        when( channel.write( any( ByteBuffer.class ) ) )
            .thenAnswer( new Answer<Future<Integer>>()
            {
                @Override
                public Future<Integer> answer( InvocationOnMock invocation ) throws Throwable
                {
                    ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
                    int i = 0;
                    for ( ; bb.hasRemaining(); i++ )
                    {
                        bb.get();
                    }
                    Future<Integer> future = mock( Future.class );
                    when( future.get() ).thenReturn( i );
                    return future;
                }
            } );

        OutputStream os = Channels.newOutputStream( channel );
        ArgumentCaptor<ByteBuffer> captured = ArgumentCaptor.forClass( ByteBuffer.class );
        os.write( 3 );

        verify( channel ).write( captured.capture() );
        verifyNoMoreInteractions( channel );

        assertThat( captured.getAllValues() )
            .hasSize( 1 );

        assertThat( captured.getAllValues().get( 0 ).array() )
            .containsOnly( new byte[] { 3 } );

        assertThat( captured.getAllValues().get( 0 ).arrayOffset() )
            .isEqualTo( 0 );

        assertThat( captured.getAllValues().get( 0 ).position() )
            .isEqualTo( 1 );

        assertThat( captured.getAllValues().get( 0 ).limit() )
            .isEqualTo( 1 );

        assertThat( captured.getAllValues().get( 0 ).capacity() )
            .isEqualTo( 1 );
    }

    @Test
    public void shouldThrowExceptionOnWrite() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        when( channel.write( any( ByteBuffer.class ) ) )
            .thenThrow( ShutdownChannelGroupException.class );
        OutputStream os = Channels.newOutputStream( channel );
        ee.expect( IOException.class );
        ee.expectCause( instanceOf( ShutdownChannelGroupException.class ) );
        os.write( new byte[1], 0, 1 );
    }

    @Test
    public void shouldThrowExceptionOnFuture1() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        Future<Integer> future = mock( Future.class );
        when( future.get() )
            .thenThrow( new ExecutionException( new InterruptedIOException() ) );
        when( channel.write( any( ByteBuffer.class ) ) )
            .thenReturn( future );
        OutputStream os = Channels.newOutputStream( channel );
        ee.expect( InterruptedIOException.class );
        os.write( new byte[1], 0, 1 );
    }

    @Test
    public void shouldThrowExceptionOnFuture2() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        Future<Integer> future = mock( Future.class );
        when( future.get() )
            .thenThrow( new ExecutionException( new RuntimeException( "msg" ) ) );
        when( channel.write( any( ByteBuffer.class ) ) )
            .thenReturn( future );
        OutputStream os = Channels.newOutputStream( channel );
        ee.expect( IOException.class );
        ee.expectCause( instanceOf( RuntimeException.class ) );
        ee.expectMessage( "msg" );
        os.write( new byte[1], 0, 1 );
    }

    @Test
    public void shouldThrowExceptionOnFuture3() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        Future<Integer> future = mock( Future.class );
        when( future.get() )
            .thenThrow( new ExecutionException( "msg", null ) );
        when( channel.write( any( ByteBuffer.class ) ) )
            .thenReturn( future );
        OutputStream os = Channels.newOutputStream( channel );
        ee.expect( IOException.class );
        ee.expectMessage( "msg" );
        os.write( new byte[1], 0, 1 );
    }
}
