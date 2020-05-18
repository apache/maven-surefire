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

import org.apache.maven.plugin.surefire.booterclient.output.DeserializedStacktraceWriter;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.api.booter.ForkedProcessEventType;
import org.apache.maven.surefire.api.event.ConsoleDebugEvent;
import org.apache.maven.surefire.api.event.ConsoleErrorEvent;
import org.apache.maven.surefire.api.event.ConsoleInfoEvent;
import org.apache.maven.surefire.api.event.ConsoleWarningEvent;
import org.apache.maven.surefire.api.event.ControlByeEvent;
import org.apache.maven.surefire.api.event.ControlNextTestEvent;
import org.apache.maven.surefire.api.event.ControlStopOnNextTestEvent;
import org.apache.maven.surefire.api.event.Event;
import org.apache.maven.surefire.api.event.JvmExitErrorEvent;
import org.apache.maven.surefire.api.event.StandardStreamErrEvent;
import org.apache.maven.surefire.api.event.StandardStreamErrWithNewLineEvent;
import org.apache.maven.surefire.api.event.StandardStreamOutEvent;
import org.apache.maven.surefire.api.event.StandardStreamOutWithNewLineEvent;
import org.apache.maven.surefire.api.event.SystemPropertyEvent;
import org.apache.maven.surefire.api.event.TestAssumptionFailureEvent;
import org.apache.maven.surefire.api.event.TestErrorEvent;
import org.apache.maven.surefire.api.event.TestFailedEvent;
import org.apache.maven.surefire.api.event.TestSkippedEvent;
import org.apache.maven.surefire.api.event.TestStartingEvent;
import org.apache.maven.surefire.api.event.TestSucceededEvent;
import org.apache.maven.surefire.api.event.TestsetCompletedEvent;
import org.apache.maven.surefire.api.event.TestsetStartingEvent;
import org.apache.maven.surefire.extensions.CloseableDaemonThread;
import org.apache.maven.surefire.extensions.EventHandler;
import org.apache.maven.surefire.extensions.ForkNodeArguments;
import org.apache.maven.surefire.extensions.util.CountdownCloseable;
import org.apache.maven.surefire.api.report.RunMode;
import org.apache.maven.surefire.api.report.StackTraceWriter;
import org.apache.maven.surefire.api.report.TestSetReportEntry;
import org.apache.maven.surefire.shared.codec.binary.Base64;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.MAGIC_NUMBER;
import static org.apache.maven.surefire.api.report.CategorizedReportEntry.reportEntry;
import static org.apache.maven.surefire.api.report.RunMode.MODES;

/**
 *
 */
public class EventConsumerThread extends CloseableDaemonThread
{
    private static final String[] JVM_ERROR_PATTERNS =
        {
            "could not create the java virtual machine",
            "error occurred during initialization", // of VM, of boot layer
            "error:", // general errors
            "could not reserve enough space", "could not allocate", "unable to allocate", // memory errors
            "java.lang.module.findexception" // JPMS errors
        };
    private static final String PRINTABLE_JVM_NATIVE_STREAM = "Listening for transport dt_socket at address:";
    private static final Base64 BASE64 = new Base64();

    private final ReadableByteChannel channel;
    private final EventHandler<Event> eventHandler;
    private final CountdownCloseable countdownCloseable;
    private final ForkNodeArguments arguments;
    private volatile boolean disabled;

    public EventConsumerThread( @Nonnull String threadName,
                                @Nonnull ReadableByteChannel channel,
                                @Nonnull EventHandler<Event> eventHandler,
                                @Nonnull CountdownCloseable countdownCloseable,
                                @Nonnull ForkNodeArguments arguments )
    {
        super( threadName );
        this.channel = channel;
        this.eventHandler = eventHandler;
        this.countdownCloseable = countdownCloseable;
        this.arguments = arguments;
    }

    @Override
    public void run()
    {
        try ( ReadableByteChannel stream = channel;
              CountdownCloseable c = countdownCloseable; )
        {
            decode();
        }
        catch ( IOException e )
        {
            // not needed
        }
    }

    @Override
    public void disable()
    {
        disabled = true;
    }

    @Override
    public void close() throws IOException
    {
        channel.close();
    }

    @SuppressWarnings( "checkstyle:innerassignment" )
    private void decode() throws IOException
    {
        List<String> tokens = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        StringBuilder token = new StringBuilder( MAGIC_NUMBER.length() );
        ByteBuffer buffer = ByteBuffer.allocate( 1024 );
        buffer.position( buffer.limit() );
        boolean streamContinues;

        start:
        do
        {
            line.setLength( 0 );
            tokens.clear();
            token.setLength( 0 );
            FrameCompletion completion = null;
            for ( boolean frameStarted = false; streamContinues = read( buffer ); completion = null )
            {
                char c = (char) buffer.get();

                if ( c == '\n' || c == '\r' )
                {
                    printExistingLine( line );
                    continue start;
                }

                line.append( c );

                if ( !frameStarted )
                {
                    if ( c == ':' )
                    {
                        frameStarted = true;
                        token.setLength( 0 );
                        tokens.clear();
                    }
                }
                else
                {
                    if ( c == ':' )
                    {
                        tokens.add( token.toString() );
                        token.setLength( 0 );
                        completion = frameCompleteness( tokens );
                        if ( completion == FrameCompletion.COMPLETE )
                        {
                            line.setLength( 0 );
                            break;
                        }
                        else if ( completion == FrameCompletion.MALFORMED )
                        {
                            printExistingLine( line );
                            continue start;
                        }
                    }
                    else
                    {
                        token.append( c );
                    }
                }
            }

            if ( completion == FrameCompletion.COMPLETE )
            {
                Event event = toEvent( tokens );
                if ( !disabled && event != null )
                {
                    eventHandler.handleEvent( event );
                }
            }

            if ( !streamContinues )
            {
                printExistingLine( line );
                return;
            }
        }
        while ( true );
    }

    private boolean read( ByteBuffer buffer ) throws IOException
    {
        if ( buffer.hasRemaining() && buffer.position() > 0 )
        {
            return true;
        }
        else
        {
            buffer.clear();
            boolean isEndOfStream = channel.read( buffer ) == -1;
            buffer.flip();
            return !isEndOfStream;
        }
    }

    private void printExistingLine( StringBuilder line )
    {
        if ( line.length() != 0 )
        {
            ConsoleLogger logger = arguments.getConsoleLogger();
            String s = line.toString().trim();
            if ( s.contains( PRINTABLE_JVM_NATIVE_STREAM ) )
            {
                if ( logger.isDebugEnabled() )
                {
                    logger.debug( s );
                }
                else if ( logger.isInfoEnabled() )
                {
                    logger.info( s );
                }
                else
                {
                    // In case of debugging forked JVM, see PRINTABLE_JVM_NATIVE_STREAM.
                    System.out.println( s );
                }
            }
            else
            {
                if ( isJvmError( s ) )
                {
                    logger.error( s );
                }
                String msg = "Corrupted STDOUT by directly writing to native stream in forked JVM "
                    + arguments.getForkChannelId() + ".";
                File dumpFile = arguments.dumpStreamText( msg + " Stream '" + s + "'." );
                arguments.logWarningAtEnd( msg + " See FAQ web page and the dump file " + dumpFile.getAbsolutePath() );

                if ( logger.isDebugEnabled() )
                {
                    logger.debug( s );
                }
            }
        }
    }

    private Event toEvent( List<String> tokensInFrame )
    {
        Iterator<String> tokens = tokensInFrame.iterator();
        String header = tokens.next();
        assert header != null;

        ForkedProcessEventType event = ForkedProcessEventType.byOpcode( tokens.next() );

        if ( event == null )
        {
            return null;
        }

        if ( event.isControlCategory() )
        {
            switch ( event )
            {
                case BOOTERCODE_BYE:
                    return new ControlByeEvent();
                case BOOTERCODE_STOP_ON_NEXT_TEST:
                    return new ControlStopOnNextTestEvent();
                case BOOTERCODE_NEXT_TEST:
                    return new ControlNextTestEvent();
                default:
                    throw new IllegalStateException( "Unknown enum " + event );
            }
        }
        else if ( event.isConsoleErrorCategory() || event.isJvmExitError() )
        {
            Charset encoding = Charset.forName( tokens.next() );
            StackTraceWriter stackTraceWriter = decodeTrace( encoding, tokens.next(), tokens.next(), tokens.next() );
            return event.isConsoleErrorCategory()
                ? new ConsoleErrorEvent( stackTraceWriter )
                : new JvmExitErrorEvent( stackTraceWriter );
        }
        else if ( event.isConsoleCategory() )
        {
            Charset encoding = Charset.forName( tokens.next() );
            String msg = decode( tokens.next(), encoding );
            switch ( event )
            {
                case BOOTERCODE_CONSOLE_INFO:
                    return new ConsoleInfoEvent( msg );
                case BOOTERCODE_CONSOLE_DEBUG:
                    return new ConsoleDebugEvent( msg );
                case BOOTERCODE_CONSOLE_WARNING:
                    return new ConsoleWarningEvent( msg );
                default:
                    throw new IllegalStateException( "Unknown enum " + event );
            }
        }
        else if ( event.isStandardStreamCategory() )
        {
            RunMode mode = MODES.get( tokens.next() );
            Charset encoding = Charset.forName( tokens.next() );
            String output = decode( tokens.next(), encoding );
            switch ( event )
            {
                case BOOTERCODE_STDOUT:
                    return new StandardStreamOutEvent( mode, output );
                case BOOTERCODE_STDOUT_NEW_LINE:
                    return new StandardStreamOutWithNewLineEvent( mode, output );
                case BOOTERCODE_STDERR:
                    return new StandardStreamErrEvent( mode, output );
                case BOOTERCODE_STDERR_NEW_LINE:
                    return new StandardStreamErrWithNewLineEvent( mode, output );
                default:
                    throw new IllegalStateException( "Unknown enum " + event );
            }
        }
        else if ( event.isSysPropCategory() )
        {
            RunMode mode = MODES.get( tokens.next() );
            Charset encoding = Charset.forName( tokens.next() );
            String key = decode( tokens.next(), encoding );
            String value = decode( tokens.next(), encoding );
            return new SystemPropertyEvent( mode, key, value );
        }
        else if ( event.isTestCategory() )
        {
            RunMode mode = MODES.get( tokens.next() );
            Charset encoding = Charset.forName( tokens.next() );
            TestSetReportEntry reportEntry =
                decodeReportEntry( encoding, tokens.next(), tokens.next(), tokens.next(), tokens.next(),
                    tokens.next(), tokens.next(), tokens.next(), tokens.next(), tokens.next(), tokens.next() );

            switch ( event )
            {
                case BOOTERCODE_TESTSET_STARTING:
                    return new TestsetStartingEvent( mode, reportEntry );
                case BOOTERCODE_TESTSET_COMPLETED:
                    return new TestsetCompletedEvent( mode, reportEntry );
                case BOOTERCODE_TEST_STARTING:
                    return new TestStartingEvent( mode, reportEntry );
                case BOOTERCODE_TEST_SUCCEEDED:
                    return new TestSucceededEvent( mode, reportEntry );
                case BOOTERCODE_TEST_FAILED:
                    return new TestFailedEvent( mode, reportEntry );
                case BOOTERCODE_TEST_SKIPPED:
                    return new TestSkippedEvent( mode, reportEntry );
                case BOOTERCODE_TEST_ERROR:
                    return new TestErrorEvent( mode, reportEntry );
                case BOOTERCODE_TEST_ASSUMPTIONFAILURE:
                    return new TestAssumptionFailureEvent( mode, reportEntry );
                default:
                    throw new IllegalStateException( "Unknown enum " + event );
            }
        }

        throw new IllegalStateException( "Missing a branch for the event type " + event );
    }

    private static FrameCompletion frameCompleteness( List<String> tokens )
    {
        if ( !tokens.isEmpty() && !MAGIC_NUMBER.equals( tokens.get( 0 ) ) )
        {
            return FrameCompletion.MALFORMED;
        }

        if ( tokens.size() >= 2 )
        {
            String opcode = tokens.get( 1 );
            ForkedProcessEventType event = ForkedProcessEventType.byOpcode( opcode );
            if ( event == null )
            {
                return FrameCompletion.MALFORMED;
            }
            else if ( event.isControlCategory() )
            {
                return FrameCompletion.COMPLETE;
            }
            else if ( event.isConsoleErrorCategory() )
            {
                return tokens.size() == 6 ? FrameCompletion.COMPLETE : FrameCompletion.NOT_COMPLETE;
            }
            else if ( event.isConsoleCategory() )
            {
                return tokens.size() == 4 ? FrameCompletion.COMPLETE : FrameCompletion.NOT_COMPLETE;
            }
            else if ( event.isStandardStreamCategory() )
            {
                return tokens.size() == 5 ? FrameCompletion.COMPLETE : FrameCompletion.NOT_COMPLETE;
            }
            else if ( event.isSysPropCategory() )
            {
                return tokens.size() == 6 ? FrameCompletion.COMPLETE : FrameCompletion.NOT_COMPLETE;
            }
            else if ( event.isTestCategory() )
            {
                return tokens.size() == 14 ? FrameCompletion.COMPLETE : FrameCompletion.NOT_COMPLETE;
            }
            else if ( event.isJvmExitError() )
            {
                return tokens.size() == 6 ? FrameCompletion.COMPLETE : FrameCompletion.NOT_COMPLETE;
            }
        }
        return FrameCompletion.NOT_COMPLETE;
    }

    static String decode( String line, Charset encoding )
    {
        // ForkedChannelEncoder is encoding the stream with US_ASCII
        return line == null || "-".equals( line )
            ? null
            : new String( BASE64.decode( line.getBytes( US_ASCII ) ), encoding );
    }

    private static StackTraceWriter decodeTrace( Charset encoding, String encTraceMessage,
                                                 String encSmartTrimmedStackTrace, String encStackTrace )
    {
        String traceMessage = decode( encTraceMessage, encoding );
        String stackTrace = decode( encStackTrace, encoding );
        String smartTrimmedStackTrace = decode( encSmartTrimmedStackTrace, encoding );
        boolean exists = traceMessage != null || stackTrace != null || smartTrimmedStackTrace != null;
        return exists ? new DeserializedStacktraceWriter( traceMessage, smartTrimmedStackTrace, stackTrace ) : null;
    }

    static TestSetReportEntry decodeReportEntry( Charset encoding,
                                                 // ReportEntry:
                                                 String encSource, String encSourceText, String encName,
                                                 String encNameText, String encGroup, String encMessage,
                                                 String encTimeElapsed,
                                                 // StackTraceWriter:
                                                 String encTraceMessage,
                                                 String encSmartTrimmedStackTrace, String encStackTrace )
        throws NumberFormatException
    {
        if ( encoding == null )
        {
            // corrupted or incomplete stream
            return null;
        }

        String source = decode( encSource, encoding );
        String sourceText = decode( encSourceText, encoding );
        String name = decode( encName, encoding );
        String nameText = decode( encNameText, encoding );
        String group = decode( encGroup, encoding );
        StackTraceWriter stackTraceWriter =
            decodeTrace( encoding, encTraceMessage, encSmartTrimmedStackTrace, encStackTrace );
        Integer elapsed = decodeToInteger( encTimeElapsed );
        String message = decode( encMessage, encoding );
        return reportEntry( source, sourceText, name, nameText,
            group, stackTraceWriter, elapsed, message, Collections.<String, String>emptyMap() );
    }

    static Integer decodeToInteger( String line )
    {
        return line == null || "-".equals( line ) ? null : Integer.decode( line );
    }

    private static boolean isJvmError( String line )
    {
        String lineLower = line.toLowerCase();
        for ( String errorPattern : JVM_ERROR_PATTERNS )
        {
            if ( lineLower.contains( errorPattern ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether the frame is complete or malformed.
     */
    private enum FrameCompletion
    {
        NOT_COMPLETE,
        COMPLETE,
        MALFORMED
    }
}
