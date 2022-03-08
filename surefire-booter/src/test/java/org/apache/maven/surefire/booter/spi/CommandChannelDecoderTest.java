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

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.api.booter.Command;
import org.apache.maven.surefire.api.booter.DumpErrorSingleton;
import org.apache.maven.surefire.api.booter.Shutdown;
import org.apache.maven.surefire.api.fork.ForkNodeArguments;
import org.apache.maven.surefire.booter.ForkedNodeArg;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.nio.channels.Channels.newChannel;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.BYE_ACK;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.NOOP;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.RUN_CLASS;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.SHUTDOWN;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.SKIP_SINCE_NEXT_TEST;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.TEST_SET_FINISHED;
import static org.apache.maven.surefire.api.booter.Shutdown.DEFAULT;
import static org.apache.maven.surefire.api.booter.Shutdown.EXIT;
import static org.apache.maven.surefire.api.booter.Shutdown.KILL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Tests for {@link CommandChannelDecoder}.
 */
@SuppressWarnings( "checkstyle:magicnumber" )
public class CommandChannelDecoderTest
{
    private static final Random RND = new Random();

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void initTmpFile()
    {
        File reportsDir = tempFolder.getRoot();
        String dumpFileName = "surefire-" + RND.nextLong();
        DumpErrorSingleton.getSingleton().init( reportsDir, dumpFileName );
    }

    @After
    public void deleteTmpFiles()
    {
        tempFolder.delete();
    }

    @Test
    public void testDecoderRunClass() throws IOException
    {
        assertEquals( String.class, RUN_CLASS.getDataType() );
        byte[] encoded = new StringBuilder( 512 )
            .append( ":maven-surefire-command:" )
            .append( (char) 13 )
            .append( ":run-testclass:" )
            .append( (char) 5 )
            .append( ":UTF-8:" )
            .append( (char) 0 )
            .append( (char) 0 )
            .append( (char) 0 )
            .append( (char) 8 )
            .append( ":" )
            .append( "pkg.Test" )
            .append( ":" )
            .toString()
            .getBytes( UTF_8 );
        InputStream is = new ByteArrayInputStream( encoded );
        ForkNodeArguments args = new ForkedNodeArg( 1, false );
        CommandChannelDecoder decoder = new CommandChannelDecoder( newChannel( is ), args );
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
        byte[] encoded = ":maven-surefire-command:\u0010:testset-finished:".getBytes();
        ByteArrayInputStream is = new ByteArrayInputStream( encoded );
        ForkNodeArguments args = new ForkedNodeArg( 1, false );
        CommandChannelDecoder decoder = new CommandChannelDecoder( newChannel( is ), args );
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
        byte[] encoded = ":maven-surefire-command:\u0014:skip-since-next-test:".getBytes();
        ByteArrayInputStream is = new ByteArrayInputStream( encoded );
        ForkNodeArguments args = new ForkedNodeArg( 1, false );
        CommandChannelDecoder decoder = new CommandChannelDecoder( newChannel( is ), args );
        command = decoder.decode();
        assertThat( command.getCommandType() ).isSameAs( SKIP_SINCE_NEXT_TEST );
        assertNull( command.getData() );
    }

    @Test
    public void testDecoderShutdownWithExit() throws IOException
    {
        Shutdown shutdownType = EXIT;
        assertEquals( String.class, SHUTDOWN.getDataType() );
        byte[] encoded = ( ":maven-surefire-command:\u0008:shutdown:\u0005:UTF-8:\u0000\u0000\u0000\u0004:"
            + shutdownType.getParam() + ":" ).getBytes();
        ByteArrayInputStream is = new ByteArrayInputStream( encoded );
        ForkNodeArguments args = new ForkedNodeArg( 1, false );
        CommandChannelDecoder decoder = new CommandChannelDecoder( newChannel( is ), args );
        Command command = decoder.decode();
        assertThat( command.getCommandType() ).isSameAs( SHUTDOWN );
        assertThat( command.getData() ).isEqualTo( shutdownType.name() );
    }

    @Test
    public void testDecoderShutdownWithKill() throws IOException
    {
        Shutdown shutdownType = KILL;
        assertEquals( String.class, SHUTDOWN.getDataType() );
        byte[] encoded = ( ":maven-surefire-command:\u0008:shutdown:\u0005:UTF-8:\u0000\u0000\u0000\u0004:"
            + shutdownType.getParam() + ":" ).getBytes();
        ByteArrayInputStream is = new ByteArrayInputStream( encoded );
        ForkNodeArguments args = new ForkedNodeArg( 1, false );
        CommandChannelDecoder decoder = new CommandChannelDecoder( newChannel( is ), args );
        Command command = decoder.decode();
        assertThat( command.getCommandType() ).isSameAs( SHUTDOWN );
        assertThat( command.getData() ).isEqualTo( shutdownType.name() );
    }

    @Test
    public void testDecoderShutdownWithDefault() throws IOException
    {
        Shutdown shutdownType = DEFAULT;
        assertEquals( String.class, SHUTDOWN.getDataType() );
        byte[] encoded = ( ":maven-surefire-command:\u0008:shutdown:\u0005:UTF-8:\u0000\u0000\u0000\u0007:"
            + shutdownType.getParam() + ":" ).getBytes();
        ByteArrayInputStream is = new ByteArrayInputStream( encoded );
        ForkNodeArguments args = new ForkedNodeArg( 1, false );
        CommandChannelDecoder decoder = new CommandChannelDecoder( newChannel( is ), args );
        Command command = decoder.decode();
        assertThat( command.getCommandType() ).isSameAs( SHUTDOWN );
        assertThat( command.getData() ).isEqualTo( shutdownType.name() );
    }

    @Test
    public void testDecoderNoop() throws IOException
    {
        assertThat( NOOP ).isSameAs( Command.NOOP.getCommandType() );
        assertEquals( Void.class, NOOP.getDataType() );
        byte[] encoded = ":maven-surefire-command:\u0004:noop:".getBytes();
        ByteArrayInputStream is = new ByteArrayInputStream( encoded );
        ForkNodeArguments args = new ForkedNodeArg( 1, false );
        CommandChannelDecoder decoder = new CommandChannelDecoder( newChannel( is ), args );
        Command command = decoder.decode();
        assertThat( command.getCommandType() ).isSameAs( NOOP );
        assertNull( command.getData() );
    }

    @Test
    public void shouldIgnoreDamagedStream() throws IOException
    {
        assertThat( BYE_ACK ).isSameAs( Command.BYE_ACK.getCommandType() );
        assertEquals( Void.class, BYE_ACK.getDataType() );
        byte[] encoded = ":maven-surefire-command:\u0007:bye-ack:".getBytes();
        byte[] streamContent = ( "<something>" + new String( encoded ) + "<damaged>" ).getBytes();
        ByteArrayInputStream is = new ByteArrayInputStream( streamContent );
        ForkNodeArguments args = new ForkedNodeArg( 1, false );
        CommandChannelDecoder decoder = new CommandChannelDecoder( newChannel( is ), args );
        Command command = decoder.decode();
        assertThat( command.getCommandType() ).isSameAs( BYE_ACK );
        assertNull( command.getData() );
    }

    @Test
    public void shouldIgnoreDamagedHeader() throws IOException
    {
        assertThat( BYE_ACK ).isSameAs( Command.BYE_ACK.getCommandType() );
        assertEquals( Void.class, BYE_ACK.getDataType() );
        byte[] encoded = ":maven-surefire-command:\u0007:bye-ack:".getBytes();
        byte[] streamContent = ( ":<damaged>:" + new String( encoded ) ).getBytes();
        ByteArrayInputStream is = new ByteArrayInputStream( streamContent );
        ForkNodeArguments args = new ForkedNodeArg( 1, false );
        CommandChannelDecoder decoder = new CommandChannelDecoder( newChannel( is ), args );
        Command command = decoder.decode();
        assertThat( command.getCommandType() ).isSameAs( BYE_ACK );
        assertNull( command.getData() );
    }

    @Test
    public void testDecoderByeAck() throws IOException
    {
        assertThat( BYE_ACK ).isSameAs( Command.BYE_ACK.getCommandType() );
        assertEquals( Void.class, BYE_ACK.getDataType() );
        byte[] encoded = ":maven-surefire-command:\u0007:bye-ack:".getBytes();
        ByteArrayInputStream is = new ByteArrayInputStream( encoded );
        ForkNodeArguments args = new ForkedNodeArg( 1, false );
        CommandChannelDecoder decoder = new CommandChannelDecoder( newChannel( is ), args );
        Command command = decoder.decode();
        assertThat( command.getCommandType() ).isSameAs( BYE_ACK );
        assertNull( command.getData() );
    }

    @Test
    public void shouldDecodeTwoCommands() throws IOException
    {
        String cmd = ":maven-surefire-command:\u0007:bye-ack:\r\n:maven-surefire-command:\u0007:bye-ack:";
        InputStream is = new ByteArrayInputStream( cmd.getBytes() );
        ForkNodeArguments args = new ForkedNodeArg( 1, false );
        CommandChannelDecoder decoder = new CommandChannelDecoder( newChannel( is ), args );

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
        ForkNodeArguments args = new ForkedNodeArg( 1, false );
        CommandChannelDecoder decoder = new CommandChannelDecoder( newChannel( is ), args );
        decoder.decode();
        fail();
    }

    @Test( expected = EOFException.class )
    public void testIncompleteCommandStart() throws IOException
    {

        ByteArrayInputStream is = new ByteArrayInputStream( new byte[] {':', '\r'} );
        ForkNodeArguments args = new ForkedNodeArg( 1, false );
        CommandChannelDecoder decoder = new CommandChannelDecoder( newChannel( is ), args );
        decoder.decode();
        fail();
    }

    @Test( expected = EOFException.class )
    public void shouldNotDecodeCorruptedCommand() throws IOException
    {
        String cmd = ":maven-surefire-command:\u0007:bye-ack ::maven-surefire-command:";
        InputStream is = new ByteArrayInputStream( cmd.getBytes() );
        ForkNodeArguments args = new ForkedNodeArg( 1, false );
        CommandChannelDecoder decoder = new CommandChannelDecoder( newChannel( is ), args );

        decoder.decode();
    }

    @Test
    public void shouldSkipCorruptedCommand() throws IOException
    {
        String cmd = ":maven-surefire-command:\0007:bye-ack\r\n::maven-surefire-command:\u0004:noop:";
        InputStream is = new ByteArrayInputStream( cmd.getBytes() );
        ForkNodeArguments args = new ForkedNodeArg( 1, false );
        CommandChannelDecoder decoder = new CommandChannelDecoder( newChannel( is ), args );

        Command command = decoder.decode();
        assertThat( command.getCommandType() ).isSameAs( NOOP );
        assertNull( command.getData() );
    }

    @Test
    public void testBinaryCommandStream() throws Exception
    {
        InputStream commands = getClass().getResourceAsStream( "/binary-commands/75171711-encoder.bin" );
        assertThat( commands ).isNotNull();
        ConsoleLoggerMock logger = new ConsoleLoggerMock( true, true, true, true );
        ForkNodeArguments args = new ForkNodeArgumentsMock( logger, new File( "" ) );
        CommandChannelDecoder decoder = new CommandChannelDecoder( newChannel( commands ), args );

        Command command = decoder.decode();
        assertThat( command ).isNotNull();
        assertThat( command.getCommandType() ).isEqualTo( NOOP );
        assertThat( command.getData() ).isNull();

        command = decoder.decode();
        assertThat( command ).isNotNull();
        assertThat( command.getCommandType() ).isEqualTo( RUN_CLASS );
        assertThat( command.getData() ).isEqualTo( "pkg.ATest" );

        for ( int i = 0; i < 24; i++ )
        {
            command = decoder.decode();
            assertThat( command ).isNotNull();
            assertThat( command.getCommandType() ).isEqualTo( NOOP );
            assertThat( command.getData() ).isNull();
        }
    }

    /**
     * Threadsafe impl. Mockito and Powermock are not thread-safe.
     */
    private static class ForkNodeArgumentsMock implements ForkNodeArguments
    {
        private final ConcurrentLinkedQueue<String> dumpStreamText = new ConcurrentLinkedQueue<>();
        private final ConcurrentLinkedQueue<String> logWarningAtEnd = new ConcurrentLinkedQueue<>();
        private final ConsoleLogger logger;
        private final File dumpStreamTextFile;

        ForkNodeArgumentsMock( ConsoleLogger logger, File dumpStreamTextFile )
        {
            this.logger = logger;
            this.dumpStreamTextFile = dumpStreamTextFile;
        }

        @Nonnull
        @Override
        public String getSessionId()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getForkChannelId()
        {
            return 0;
        }

        @Nonnull
        @Override
        public File dumpStreamText( @Nonnull String text )
        {
            dumpStreamText.add( text );
            return dumpStreamTextFile;
        }

        @Nonnull
        @Override
        public File dumpStreamException( @Nonnull Throwable t )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void logWarningAtEnd( @Nonnull String text )
        {
            logWarningAtEnd.add( text );
        }

        @Nonnull
        @Override
        public ConsoleLogger getConsoleLogger()
        {
            return logger;
        }

        @Nonnull
        @Override
        public Object getConsoleLock()
        {
            return logger;
        }

        boolean isCalled()
        {
            return !dumpStreamText.isEmpty() || !logWarningAtEnd.isEmpty();
        }

        @Override
        public File getEventStreamBinaryFile()
        {
            return null;
        }

        @Override
        public File getCommandStreamBinaryFile()
        {
            return null;
        }
    }

    /**
     * Threadsafe impl. Mockito and Powermock are not thread-safe.
     */
    private static class ConsoleLoggerMock implements ConsoleLogger
    {
        final ConcurrentLinkedQueue<String> debug = new ConcurrentLinkedQueue<>();
        final ConcurrentLinkedQueue<String> info = new ConcurrentLinkedQueue<>();
        final ConcurrentLinkedQueue<String> error = new ConcurrentLinkedQueue<>();
        final boolean isDebug;
        final boolean isInfo;
        final boolean isWarning;
        final boolean isError;
        private volatile boolean called;
        private volatile boolean isDebugEnabledCalled;
        private volatile boolean isInfoEnabledCalled;

        ConsoleLoggerMock( boolean isDebug, boolean isInfo, boolean isWarning, boolean isError )
        {
            this.isDebug = isDebug;
            this.isInfo = isInfo;
            this.isWarning = isWarning;
            this.isError = isError;
        }

        @Override
        public synchronized boolean isDebugEnabled()
        {
            isDebugEnabledCalled = true;
            called = true;
            return isDebug;
        }

        @Override
        public synchronized void debug( String message )
        {
            debug.add( message );
            called = true;
        }

        @Override
        public synchronized boolean isInfoEnabled()
        {
            isInfoEnabledCalled = true;
            called = true;
            return isInfo;
        }

        @Override
        public synchronized void info( String message )
        {
            info.add( message );
            called = true;
        }

        @Override
        public synchronized boolean isWarnEnabled()
        {
            called = true;
            return isWarning;
        }

        @Override
        public synchronized void warning( String message )
        {
            called = true;
        }

        @Override
        public synchronized boolean isErrorEnabled()
        {
            called = true;
            return isError;
        }

        @Override
        public synchronized void error( String message )
        {
            error.add( message );
            called = true;
        }

        @Override
        public synchronized void error( String message, Throwable t )
        {
            called = true;
        }

        @Override
        public synchronized void error( Throwable t )
        {
            called = true;
        }

        synchronized boolean isCalled()
        {
            return called;
        }
    }
}
