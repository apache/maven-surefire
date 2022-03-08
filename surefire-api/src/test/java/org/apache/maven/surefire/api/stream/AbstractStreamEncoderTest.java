package org.apache.maven.surefire.api.stream;

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

import org.apache.maven.surefire.api.booter.ForkedProcessEventType;
import org.apache.maven.surefire.api.util.internal.WritableBufferedByteChannel;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.maven.surefire.api.booter.Constants.MAGIC_NUMBER_FOR_EVENTS_BYTES;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_BYE;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_CONSOLE_DEBUG;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_CONSOLE_ERROR;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_CONSOLE_INFO;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_CONSOLE_WARNING;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_JVM_EXIT_ERROR;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_NEXT_TEST;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_STDERR;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_STDERR_NEW_LINE;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_STDOUT;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_STDOUT_NEW_LINE;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_STOP_ON_NEXT_TEST;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_SYSPROPS;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_TESTSET_COMPLETED;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_TESTSET_STARTING;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_TEST_ASSUMPTIONFAILURE;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_TEST_ERROR;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_TEST_FAILED;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_TEST_SKIPPED;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_TEST_STARTING;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_TEST_SUCCEEDED;
import static org.apache.maven.surefire.api.report.RunMode.NORMAL_RUN;
import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
@SuppressWarnings( { "checkstyle:linelength", "checkstyle:magicnumber" } )
public class AbstractStreamEncoderTest
{
    @Test
    public void shouldComputeStreamPreemptiveLength()
    {
        Encoder streamEncoder = new Encoder( new DummyChannel() );
        CharsetEncoder encoder = streamEncoder.newCharsetEncoder();

        // :maven-surefire-event:8:sys-prop:10:normal-run:1:5:UTF-8:0003:kkk:0003:vvv:
        assertThat( streamEncoder.estimateBufferLength( BOOTERCODE_SYSPROPS.getOpcodeBinary().length, NORMAL_RUN,
            encoder, 0, 1, "k", "v" ) )
            .isEqualTo( 82 );

        // :maven-surefire-event:16:testset-starting:10:normal-run:1:5:UTF-8:0003:sss:0003:sss:0003:sss:0003:sss:0003:sss:0003:sss:X0003:0003:sss:0003:sss:0003:sss:
        assertThat( streamEncoder.estimateBufferLength( BOOTERCODE_TESTSET_STARTING.getOpcodeBinary().length,
            NORMAL_RUN, encoder, 1, 1, "s", "s", "s", "s", "s", "s", "s", "s", "s" ) )
            .isEqualTo( 159 );

        // :maven-surefire-event:17:testset-completed:10:normal-run:1:5:UTF-8:0003:sss:0003:sss:0003:sss:0003:sss:0003:sss:0003:sss:X0003:0003:sss:0003:sss:0003:sss:
        assertThat( streamEncoder.estimateBufferLength( BOOTERCODE_TESTSET_COMPLETED.getOpcodeBinary().length,
            NORMAL_RUN, encoder, 1, 1, "s", "s", "s", "s", "s", "s", "s", "s", "s" ) )
            .isEqualTo( 160 );

        // :maven-surefire-event:13:test-starting:10:normal-run:1:5:UTF-8:0003:sss:0003:sss:0003:sss:0003:sss:0003:sss:0003:sss:X0003:0003:sss:0003:sss:0003:sss:
        assertThat( streamEncoder.estimateBufferLength( BOOTERCODE_TEST_STARTING.getOpcodeBinary().length,
            NORMAL_RUN, encoder, 1, 1, "s", "s", "s", "s", "s", "s", "s", "s", "s" ) )
            .isEqualTo( 156 );

        // :maven-surefire-event:14:test-succeeded:10:normal-run:1:5:UTF-8:0003:sss:0003:sss:0003:sss:0003:sss:0003:sss:0003:sss:X0003:0003:sss:0003:sss:0003:sss:
        assertThat( streamEncoder.estimateBufferLength( BOOTERCODE_TEST_SUCCEEDED.getOpcodeBinary().length,
            NORMAL_RUN, encoder, 1, 1, "s", "s", "s", "s", "s", "s", "s", "s", "s" ) )
            .isEqualTo( 157 );

        // :maven-surefire-event:11:test-failed:10:normal-run:1:5:UTF-8:0003:sss:0003:sss:0003:sss:0003:sss:0003:sss:0003:sss:X0003:0003:sss:0003:sss:0003:sss:
        assertThat( streamEncoder.estimateBufferLength( BOOTERCODE_TEST_FAILED.getOpcodeBinary().length,
            NORMAL_RUN, encoder, 1, 1, "s", "s", "s", "s", "s", "s", "s", "s", "s" ) )
            .isEqualTo( 154 );

        // :maven-surefire-event:12:test-skipped:10:normal-run:1:5:UTF-8:0003:sss:0003:sss:0003:sss:0003:sss:0003:sss:0003:sss:X0003:0003:sss:0003:sss:0003:sss:
        assertThat( streamEncoder.estimateBufferLength( BOOTERCODE_TEST_SKIPPED.getOpcodeBinary().length,
            NORMAL_RUN, encoder, 1, 1, "s", "s", "s", "s", "s", "s", "s", "s", "s" ) )
            .isEqualTo( 155 );

        // :maven-surefire-event:10:test-error:10:normal-run:1:5:UTF-8:0003:sss:0003:sss:0003:sss:0003:sss:0003:sss:0003:sss:X0003:0003:sss:0003:sss:0003:sss:
        assertThat( streamEncoder.estimateBufferLength( BOOTERCODE_TEST_ERROR.getOpcodeBinary().length,
            NORMAL_RUN, encoder, 1, 1, "s", "s", "s", "s", "s", "s", "s", "s", "s" ) )
            .isEqualTo( 153 );

        // :maven-surefire-event:23:test-assumption-failure:10:normal-run:1:5:UTF-8:0003:sss:0003:sss:0003:sss:0003:sss:0003:sss:0003:sss:X0003:0003:sss:0003:sss:0003:sss:
        assertThat( streamEncoder.estimateBufferLength( BOOTERCODE_TEST_ASSUMPTIONFAILURE.getOpcodeBinary().length,
            NORMAL_RUN, encoder, 1, 1, "s", "s", "s", "s", "s", "s", "s", "s", "s" ) )
            .isEqualTo( 166 );

        // :maven-surefire-event:14:std-out-stream:10:normal-run:1:5:UTF-8:0003:sss:
        assertThat( streamEncoder.estimateBufferLength( BOOTERCODE_STDOUT.getOpcodeBinary().length,
            NORMAL_RUN, encoder, 0, 1, "s" ) )
            .isEqualTo( 79 );

        // :maven-surefire-event:23:std-out-stream-new-line:10:normal-run:1:5:UTF-8:0003:sss:
        assertThat( streamEncoder.estimateBufferLength( BOOTERCODE_STDOUT_NEW_LINE.getOpcodeBinary().length,
            NORMAL_RUN, encoder, 0, 1, "s" ) )
            .isEqualTo( 88 );

        // :maven-surefire-event:14:std-err-stream:10:normal-run:1:5:UTF-8:0003:sss:
        assertThat( streamEncoder.estimateBufferLength( BOOTERCODE_STDERR.getOpcodeBinary().length,
            NORMAL_RUN, encoder, 0, 1, "s" ) )
            .isEqualTo( 79 );

        // :maven-surefire-event:23:std-err-stream-new-line:10:normal-run:1:5:UTF-8:0003:sss:
        assertThat( streamEncoder.estimateBufferLength( BOOTERCODE_STDERR_NEW_LINE.getOpcodeBinary().length,
            NORMAL_RUN, encoder, 0, 1, "s" ) )
            .isEqualTo( 88 );

        // :maven-surefire-event:16:console-info-log:5:UTF-8:0003:sss:
        assertThat( streamEncoder.estimateBufferLength( BOOTERCODE_CONSOLE_INFO.getOpcodeBinary().length,
            null, encoder, 0, 0, "s" ) )
            .isEqualTo( 58 );

        // :maven-surefire-event:17:console-debug-log:5:UTF-8:0003:sss:
        assertThat( streamEncoder.estimateBufferLength( BOOTERCODE_CONSOLE_DEBUG.getOpcodeBinary().length,
            null, encoder, 0, 0, "s" ) )
            .isEqualTo( 59 );

        // :maven-surefire-event:19:console-warning-log:5:UTF-8:0003:sss:
        assertThat( streamEncoder.estimateBufferLength( BOOTERCODE_CONSOLE_WARNING.getOpcodeBinary().length,
            null, encoder, 0, 0, "s" ) )
            .isEqualTo( 61 );

        // :maven-surefire-event:17:console-error-log:5:UTF-8:0003:sss:
        assertThat( streamEncoder.estimateBufferLength( BOOTERCODE_CONSOLE_ERROR.getOpcodeBinary().length,
            null, encoder, 0, 0, "s" ) )
            .isEqualTo( 59 );

        // :maven-surefire-event:3:bye:
        assertThat( streamEncoder.estimateBufferLength( BOOTERCODE_BYE.getOpcodeBinary().length,
            null, null, 0, 0 ) )
            .isEqualTo( 28 );

        // :maven-surefire-event:17:stop-on-next-test:
        assertThat( streamEncoder.estimateBufferLength( BOOTERCODE_STOP_ON_NEXT_TEST.getOpcodeBinary().length,
            null, null, 0, 0 ) )
            .isEqualTo( 42 );

        // :maven-surefire-event:9:next-test:
        assertThat( streamEncoder.estimateBufferLength( BOOTERCODE_NEXT_TEST.getOpcodeBinary().length,
            null, null, 0, 0 ) )
            .isEqualTo( 34 );

        // :maven-surefire-event:14:jvm-exit-error:
        assertThat( streamEncoder.estimateBufferLength( BOOTERCODE_JVM_EXIT_ERROR.getOpcodeBinary().length,
            null, null, 0, 0 ) )
            .isEqualTo( 39 );
    }

    @Test
    public void testSendOpcode()
    {
        Encoder streamEncoder = new Encoder( new DummyChannel() );
        ByteBuffer result = ByteBuffer.allocate( 128 );
        streamEncoder.encodeHeader( result, BOOTERCODE_TEST_ERROR, NORMAL_RUN, 1L );
        assertThat( toString( result ) )
            .isEqualTo( ":maven-surefire-event:" + (char) 10 + ":test-error:" + (char) 10
                + ":normal-run:\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001:" );

        result = ByteBuffer.allocate( 1024 );
        streamEncoder.encodeHeader( result, BOOTERCODE_CONSOLE_ERROR );
        streamEncoder.encodeCharset( result );
        assertThat( toString( result ) )
            .isEqualTo( ":maven-surefire-event:" + (char) 17 + ":console-error-log:" + (char) 5 + ":UTF-8:" );
    }

    @Test
    public void testEncodedString()
    {
        Encoder streamEncoder = new Encoder( new DummyChannel() );
        ByteBuffer result = ByteBuffer.allocate( 128 );
        streamEncoder.encode( streamEncoder.newCharsetEncoder(), result, BOOTERCODE_STDOUT, NORMAL_RUN, 1L, "msg" );
        assertThat( toString( result ) )
            .isEqualTo( ":maven-surefire-event:\u000e:std-out-stream:"
                + (char) 10 + ":normal-run:\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001"
                + ":\u0005:UTF-8:\u0000\u0000\u0000\u0003:msg:" );
    }

    @Test
    public void testIntegerAsNull()
    {
        Encoder streamEncoder = new Encoder( new DummyChannel() );
        ByteBuffer result = ByteBuffer.allocate( 4 );
        streamEncoder.encodeInteger( result, null );
        assertThat( ( (Buffer) result ).position() ).isEqualTo( 2 );
        assertThat( result.get( 0 ) ).isEqualTo( (byte) 0 );
        assertThat( result.get( 1 ) ).isEqualTo( (byte) ':' );
    }

    @Test
    public void testInteger()
    {
        Encoder streamEncoder = new Encoder( new DummyChannel() );
        ByteBuffer result = ByteBuffer.allocate( 8 );
        streamEncoder.encodeInteger( result, 5 );
        assertThat( ( (Buffer) result ).position() ).isEqualTo( 6 );
        ( (Buffer) result ).position( 0 );
        assertThat( result.get() ).isEqualTo( (byte) 0xff );
        assertThat( result.getInt() ).isEqualTo( (byte) 5 );
        assertThat( result.get( 5 ) ).isEqualTo( (byte) ':' );
    }

    @Test
    public void testWrite() throws Exception
    {
        DummyChannel channel = new DummyChannel();
        Encoder streamEncoder = new Encoder( channel );
        streamEncoder.write( ByteBuffer.allocate( 0 ), false );
        assertThat( channel.writeBuffered ).isTrue();
        assertThat( channel.write ).isFalse();
        channel.writeBuffered = false;
        channel.write = false;
        streamEncoder.write( ByteBuffer.allocate( 0 ), true );
        assertThat( channel.writeBuffered ).isFalse();
        assertThat( channel.write ).isTrue();
    }

    private static String toString( ByteBuffer frame )
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ( (Buffer) frame ).flip();
        os.write( frame.array(), frame.arrayOffset() + ( (Buffer) frame ).position(), frame.remaining() );
        return new String( os.toByteArray(), UTF_8 );
    }

    private static class Encoder extends AbstractStreamEncoder<ForkedProcessEventType>
    {

        Encoder( WritableBufferedByteChannel out )
        {
            super( out );
        }

        @Nonnull
        @Override
        public byte[] getEncodedMagicNumber()
        {
            return MAGIC_NUMBER_FOR_EVENTS_BYTES;
        }

        @Nonnull
        @Override
        protected byte[] enumToByteArray( ForkedProcessEventType forkedProcessEventType )
        {
            return forkedProcessEventType.getOpcodeBinary();
        }

        @Nonnull
        @Override
        protected byte[] getEncodedCharsetName()
        {
            return getCharset().name().getBytes( US_ASCII );
        }

        @Nonnull
        @Override
        public Charset getCharset()
        {
            return UTF_8;
        }

        @Nonnull
        @Override
        public CharsetEncoder newCharsetEncoder()
        {
            return getCharset().newEncoder();
        }
    }

    private static class DummyChannel implements WritableBufferedByteChannel
    {
        boolean writeBuffered;
        boolean write;

        @Override
        public void writeBuffered( ByteBuffer src )
        {
            writeBuffered = true;
        }

        @Override
        public long countBufferOverflows()
        {
            return 0;
        }

        @Override
        public int write( ByteBuffer src )
        {
            write = true;
            return 0;
        }

        @Override
        public boolean isOpen()
        {
            return false;
        }

        @Override
        public void close()
        {
        }
    }

}
