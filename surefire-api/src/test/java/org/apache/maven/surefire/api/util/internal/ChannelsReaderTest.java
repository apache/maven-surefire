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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ShutdownChannelGroupException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static java.nio.file.Files.write;
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
import static org.powermock.reflect.Whitebox.invokeMethod;

/**
 * The tests for {@link Channels#newChannel(InputStream)} and {@link Channels#newBufferedChannel(InputStream)}.
 */
public class ChannelsReaderTest
{
    @Rule
    public final ExpectedException ee = ExpectedException.none();

    @Rule
    public final TemporaryFolder tmp = TemporaryFolder.builder()
        .assureDeletion()
        .build();

    @Test
    public void shouldOverflowBuffer() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        WritableBufferedByteChannel channel = invokeMethod( Channels.class, "newChannel",
            new Class[] {OutputStream.class, int.class}, new Object[] {out, 8} );

        assertThat( channel.countBufferOverflows() )
            .isEqualTo( 0 );

        channel.write( ByteBuffer.wrap( new byte[] {1, 2, 3} ) );

        assertThat( channel.countBufferOverflows() )
            .isEqualTo( 0 );

        channel.write( ByteBuffer.wrap( new byte[] {4, 5, 6, 7, 8} ) );

        assertThat( channel.countBufferOverflows() )
            .isEqualTo( 0 );

        channel.write( ByteBuffer.wrap( new byte[] {9} ) );

        assertThat( channel.countBufferOverflows() )
            .isEqualTo( 1 );

        assertThat( out.toByteArray() )
            .isEqualTo( new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9} );
    }

    @Test
    public void exactBufferSize() throws Exception
    {
        ByteArrayInputStream is = new ByteArrayInputStream( new byte[] {1, 2, 3} );
        ReadableByteChannel channel = Channels.newChannel( is );
        ByteBuffer bb = ByteBuffer.allocate( 3 );

        int countWritten = channel.read( bb );

        assertThat( countWritten )
            .isEqualTo( 3 );

        assertThat( bb.arrayOffset() )
            .isEqualTo( 0 );

        assertThat( ( (Buffer) bb ).position() )
            .isEqualTo( 3 );

        assertThat( bb.remaining() )
            .isEqualTo( 0 );

        assertThat( ( (Buffer) bb ).limit() )
            .isEqualTo( 3 );

        assertThat( bb.capacity() )
            .isEqualTo( 3 );

        ( (Buffer) bb ).flip();

        assertThat( bb.arrayOffset() )
            .isEqualTo( 0 );

        assertThat( ( (Buffer) bb ).position() )
            .isEqualTo( 0 );

        assertThat( bb.remaining() )
            .isEqualTo( 3 );

        assertThat( ( (Buffer) bb ).limit() )
            .isEqualTo( 3 );

        assertThat( bb.capacity() )
            .isEqualTo( 3 );

        assertThat( bb.array() )
            .isEqualTo( new byte[] {1, 2, 3} );

        assertThat( channel.isOpen() )
            .isTrue();

        channel.close();

        assertThat( channel.isOpen() )
            .isFalse();
    }

    @Test
    public void bufferedChannel() throws Exception
    {
        ByteArrayInputStream is = new ByteArrayInputStream( new byte[] {1, 2, 3} );
        ReadableByteChannel channel = Channels.newBufferedChannel( is );
        ByteBuffer bb = ByteBuffer.allocate( 4 );

        int countWritten = channel.read( bb );

        assertThat( countWritten )
            .isEqualTo( 3 );

        assertThat( bb.arrayOffset() )
            .isEqualTo( 0 );

        assertThat( ( (Buffer) bb ).position() )
            .isEqualTo( 3 );

        assertThat( bb.remaining() )
            .isEqualTo( 1 );

        assertThat( ( (Buffer) bb ).limit() )
            .isEqualTo( 4 );

        assertThat( bb.capacity() )
            .isEqualTo( 4 );

        ( (Buffer) bb ).flip();

        assertThat( bb.arrayOffset() )
            .isEqualTo( 0 );

        assertThat( ( (Buffer) bb ).position() )
            .isEqualTo( 0 );

        assertThat( bb.remaining() )
            .isEqualTo( 3 );

        assertThat( ( (Buffer) bb ).limit() )
            .isEqualTo( 3 );

        assertThat( bb.capacity() )
            .isEqualTo( 4 );

        assertThat( bb.array() )
            .isEqualTo( new byte[] {1, 2, 3, 0} );

        assertThat( channel.isOpen() )
            .isTrue();

        channel.close();

        assertThat( channel.isOpen() )
            .isFalse();
    }

    @Test
    public void biggerBuffer() throws Exception
    {
        ByteArrayInputStream is = new ByteArrayInputStream( new byte[] {1, 2, 3} );
        ReadableByteChannel channel = Channels.newChannel( is );
        ByteBuffer bb = ByteBuffer.allocate( 4 );

        int countWritten = channel.read( bb );

        assertThat( countWritten )
            .isEqualTo( 3 );

        assertThat( bb.arrayOffset() )
            .isEqualTo( 0 );

        assertThat( ( (Buffer) bb ).position() )
            .isEqualTo( 3 );

        assertThat( bb.remaining() )
            .isEqualTo( 1 );

        assertThat( ( (Buffer) bb ).limit() )
            .isEqualTo( 4 );

        assertThat( bb.capacity() )
            .isEqualTo( 4 );

        ( (Buffer) bb ).flip();

        assertThat( bb.arrayOffset() )
            .isEqualTo( 0 );

        assertThat( ( (Buffer) bb ).position() )
            .isEqualTo( 0 );

        assertThat( bb.remaining() )
            .isEqualTo( 3 );

        assertThat( ( (Buffer) bb ).limit() )
            .isEqualTo( 3 );

        assertThat( bb.capacity() )
            .isEqualTo( 4 );

        assertThat( bb.array() )
            .isEqualTo( new byte[] {1, 2, 3, 0} );

        assertThat( channel.isOpen() )
            .isTrue();

        channel.close();

        assertThat( channel.isOpen() )
            .isFalse();
    }

    @Test
    public void shouldFailAfterClosed() throws IOException
    {
        ByteArrayInputStream is = new ByteArrayInputStream( new byte[] {1, 2, 3} );
        ReadableByteChannel channel = Channels.newChannel( is );
        channel.close();
        assertThat( channel.isOpen() ).isFalse();
        ee.expect( ClosedChannelException.class );
        channel.read( ByteBuffer.allocate( 0 ) );
    }

    @Test
    public void shouldFailIfNotReadable() throws IOException
    {
        ByteArrayInputStream is = new ByteArrayInputStream( new byte[] {1, 2, 3} );
        ReadableByteChannel channel = Channels.newChannel( is );
        ee.expect( NonReadableChannelException.class );
        channel.read( ByteBuffer.allocate( 0 ).asReadOnlyBuffer() );
    }

    @Test
    public void shouldFailIOnDirectBuffer() throws IOException
    {
        ByteArrayInputStream is = new ByteArrayInputStream( new byte[] {1, 2, 3} );
        ReadableByteChannel channel = Channels.newChannel( is );
        ee.expect( NonReadableChannelException.class );
        channel.read( ByteBuffer.allocateDirect( 0 ) );
    }

    @Test
    public void shouldUseFileChannel() throws IOException
    {
        File f = tmp.newFile();
        write( f.toPath(), new byte[] {1, 2, 3} );
        FileInputStream is = new FileInputStream( f );
        ReadableByteChannel channel = Channels.newChannel( is );
        ByteBuffer bb = ByteBuffer.allocate( 4 );
        int countWritten = channel.read( bb );

        assertThat( channel.isOpen() )
            .isTrue();

        channel.close();

        assertThat( channel.isOpen() )
            .isFalse();

        assertThat( countWritten )
            .isEqualTo( 3 );

        assertThat( bb.arrayOffset() )
            .isEqualTo( 0 );

        assertThat( ( (Buffer) bb ).position() )
            .isEqualTo( 3 );

        assertThat( bb.remaining() )
            .isEqualTo( 1 );

        assertThat( ( (Buffer) bb ).limit() )
            .isEqualTo( 4 );

        assertThat( bb.capacity() )
            .isEqualTo( 4 );

        ( (Buffer) bb ).flip();

        assertThat( bb.arrayOffset() )
            .isEqualTo( 0 );

        assertThat( ( (Buffer) bb ).position() )
            .isEqualTo( 0 );

        assertThat( bb.remaining() )
            .isEqualTo( 3 );

        assertThat( ( (Buffer) bb ).limit() )
            .isEqualTo( 3 );

        assertThat( bb.capacity() )
            .isEqualTo( 4 );

        assertThat( bb.array() )
            .isEqualTo( new byte[] {1, 2, 3, 0} );
    }

    @Test( expected = IndexOutOfBoundsException.class )
    public void shouldValidateInput1() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        InputStream is = Channels.newInputStream( channel );
        is.read( new byte[0], -1, 0 );
    }

    @Test( expected = IndexOutOfBoundsException.class )
    public void shouldValidateInput2() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        InputStream is = Channels.newInputStream( channel );
        is.read( new byte[0], 0, -1 );
    }

    @Test( expected = IndexOutOfBoundsException.class )
    public void shouldValidateInput3() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        InputStream is = Channels.newInputStream( channel );
        is.read( new byte[0], 1, 0 );
    }

    @Test( expected = IndexOutOfBoundsException.class )
    public void shouldValidateInput4() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        InputStream is = Channels.newInputStream( channel );
        is.read( new byte[0], 0, 1 );
    }

    @Test
    public void shouldClose() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        when( channel.isOpen() ).thenReturn( true );
        InputStream is = Channels.newInputStream( channel );
        is.close();
        verify( channel, times( 1 ) ).close();
    }

    @Test
    public void shouldNotClose() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        when( channel.isOpen() ).thenReturn( false );
        InputStream is = Channels.newInputStream( channel );
        is.close();
        verify( channel, never() ).close();
    }

    @Test
    public void shouldAlreadyClosed() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        when( channel.isOpen() ).thenReturn( true );
        doThrow( ClosedChannelException.class ).when( channel ).close();
        InputStream is = Channels.newInputStream( channel );
        is.close();
        verify( channel ).close();
    }

    @Test
    public void shouldReadZeroLength() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        InputStream is = Channels.newInputStream( channel );
        is.read( new byte[] { 5 }, 0, 0 );
        verifyZeroInteractions( channel );
    }

    @Test
    public void shouldReadArray() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        when( channel.read( any( ByteBuffer.class ) ) )
            .thenAnswer( new Answer<Future<Integer>>()
            {
                @Override
                public Future<Integer> answer( InvocationOnMock invocation ) throws Throwable
                {
                    ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
                    bb.put( (byte) 3 );
                    bb.put( (byte) 4 );
                    Future<Integer> future = mock( Future.class );
                    when( future.get() ).thenReturn( 2 );
                    return future;
                }
            } );

        InputStream is = Channels.newInputStream( channel );
        ArgumentCaptor<ByteBuffer> captured = ArgumentCaptor.forClass( ByteBuffer.class );
        byte[] b = new byte[] { 1, 2, 0, 0, 5 };
        is.read( b, 2, 2 );

        verify( channel ).read( captured.capture() );
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
    public void shouldRead() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        when( channel.read( any( ByteBuffer.class ) ) )
            .thenAnswer( new Answer<Future<Integer>>()
            {
                @Override
                public Future<Integer> answer( InvocationOnMock invocation ) throws Throwable
                {
                    ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
                    bb.put( (byte) 3 );
                    Future<Integer> future = mock( Future.class );
                    when( future.get() ).thenReturn( 1 );
                    return future;
                }
            } );

        InputStream is = Channels.newInputStream( channel );
        ArgumentCaptor<ByteBuffer> captured = ArgumentCaptor.forClass( ByteBuffer.class );
        int b = is.read();
        assertThat( b )
            .isEqualTo( 3 );

        verify( channel ).read( captured.capture() );
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
    public void shouldThrowExceptionOnRead() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        when( channel.read( any( ByteBuffer.class ) ) )
            .thenThrow( ShutdownChannelGroupException.class );
        InputStream is = Channels.newInputStream( channel );
        ee.expect( IOException.class );
        ee.expectCause( instanceOf( ShutdownChannelGroupException.class ) );
        is.read( new byte[1], 0, 1 );
    }

    @Test
    public void shouldThrowExceptionOnFuture1() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        Future<Integer> future = mock( Future.class );
        when( future.get() )
            .thenThrow( new ExecutionException( new InterruptedIOException() ) );
        when( channel.read( any( ByteBuffer.class ) ) )
            .thenReturn( future );
        InputStream is = Channels.newInputStream( channel );
        ee.expect( InterruptedIOException.class );
        is.read( new byte[1], 0, 1 );
    }

    @Test
    public void shouldThrowExceptionOnFuture2() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        Future<Integer> future = mock( Future.class );
        when( future.get() )
            .thenThrow( new ExecutionException( new RuntimeException( "msg" ) ) );
        when( channel.read( any( ByteBuffer.class ) ) )
            .thenReturn( future );
        InputStream is = Channels.newInputStream( channel );
        ee.expect( IOException.class );
        ee.expectCause( instanceOf( RuntimeException.class ) );
        ee.expectMessage( "msg" );
        is.read( new byte[1], 0, 1 );
    }

    @Test
    public void shouldThrowExceptionOnFuture3() throws Exception
    {
        AsynchronousByteChannel channel = mock( AsynchronousByteChannel.class );
        Future<Integer> future = mock( Future.class );
        when( future.get() )
            .thenThrow( new ExecutionException( "msg", null ) );
        when( channel.read( any( ByteBuffer.class ) ) )
            .thenReturn( future );
        InputStream is = Channels.newInputStream( channel );
        ee.expect( IOException.class );
        ee.expectMessage( "msg" );
        is.read( new byte[1], 0, 1 );
    }
}
