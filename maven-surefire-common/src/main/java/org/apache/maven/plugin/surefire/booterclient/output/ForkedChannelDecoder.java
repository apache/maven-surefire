package org.apache.maven.plugin.surefire.booterclient.output;

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

import org.apache.commons.codec.binary.Base64;
import org.apache.maven.surefire.booter.ForkedProcessEvent;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunMode;
import org.apache.maven.surefire.report.StackTraceWriter;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.MAGIC_NUMBER;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_STDERR;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_STDERR_NEW_LINE;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_STDOUT;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_STDOUT_NEW_LINE;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_BYE;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_CONSOLE_DEBUG;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_CONSOLE_INFO;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_CONSOLE_WARNING;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_NEXT_TEST;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_STOP_ON_NEXT_TEST;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TEST_ASSUMPTIONFAILURE;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TEST_ERROR;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TEST_FAILED;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TEST_SKIPPED;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TEST_STARTING;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TEST_SUCCEEDED;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TESTSET_COMPLETED;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TESTSET_STARTING;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.EVENTS;
import static org.apache.maven.surefire.report.CategorizedReportEntry.reportEntry;
import static org.apache.maven.surefire.report.RunMode.MODES;
import static org.apache.maven.surefire.util.internal.StringUtils.isBlank;
import static org.apache.maven.surefire.util.internal.StringUtils.isNotBlank;
import static java.util.Objects.requireNonNull;

/**
 * magic number : run mode : opcode [: opcode specific data]*
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
public final class ForkedChannelDecoder
{
    private static final Base64 BASE64 = new Base64();

    private volatile ForkedProcessPropertyEventListener propertyEventListener;
    private volatile ForkedProcessStackTraceEventListener consoleErrorEventListener;
    private volatile ForkedProcessExitErrorListener exitErrorEventListener;

    private final ConcurrentMap<ForkedProcessEvent, ForkedProcessReportEventListener<?>> reportEventListeners =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<ForkedProcessEvent, ForkedProcessStandardOutErrEventListener> stdOutErrEventListeners =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<ForkedProcessEvent, ForkedProcessStringEventListener> consoleEventListeners =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<ForkedProcessEvent, ForkedProcessEventListener> controlEventListeners =
            new ConcurrentHashMap<>();

    public void setSystemPropertiesListener( ForkedProcessPropertyEventListener listener )
    {
        propertyEventListener = requireNonNull( listener );
    }

    public <T extends ReportEntry> void setTestSetStartingListener( ForkedProcessReportEventListener<T> listener )
    {
        reportEventListeners.put( BOOTERCODE_TESTSET_STARTING, requireNonNull( listener ) );
    }

    public void setTestSetCompletedListener( ForkedProcessReportEventListener<?> listener )
    {
        reportEventListeners.put( BOOTERCODE_TESTSET_COMPLETED, requireNonNull( listener ) );
    }

    public void setTestStartingListener( ForkedProcessReportEventListener<?> listener )
    {
        reportEventListeners.put( BOOTERCODE_TEST_STARTING, requireNonNull( listener ) );
    }

    public void setTestSucceededListener( ForkedProcessReportEventListener<?> listener )
    {
        reportEventListeners.put( BOOTERCODE_TEST_SUCCEEDED, requireNonNull( listener ) );
    }

    public void setTestFailedListener( ForkedProcessReportEventListener<?> listener )
    {
        reportEventListeners.put( BOOTERCODE_TEST_FAILED, requireNonNull( listener ) );
    }

    public void setTestSkippedListener( ForkedProcessReportEventListener<?> listener )
    {
        reportEventListeners.put( BOOTERCODE_TEST_SKIPPED, requireNonNull( listener ) );
    }

    public void setTestErrorListener( ForkedProcessReportEventListener<?> listener )
    {
        reportEventListeners.put( BOOTERCODE_TEST_ERROR, requireNonNull( listener ) );
    }

    public void setTestAssumptionFailureListener( ForkedProcessReportEventListener<?> listener )
    {
        reportEventListeners.put( BOOTERCODE_TEST_ASSUMPTIONFAILURE, requireNonNull( listener ) );
    }

    public void setStdOutListener( ForkedProcessStandardOutErrEventListener listener )
    {
        stdOutErrEventListeners.put( BOOTERCODE_STDOUT, requireNonNull( listener ) );
        stdOutErrEventListeners.put( BOOTERCODE_STDOUT_NEW_LINE, requireNonNull( listener ) );
    }

    public void setStdErrListener( ForkedProcessStandardOutErrEventListener listener )
    {
        stdOutErrEventListeners.put( BOOTERCODE_STDERR, requireNonNull( listener ) );
        stdOutErrEventListeners.put( BOOTERCODE_STDERR_NEW_LINE, requireNonNull( listener ) );
    }

    public void setConsoleInfoListener( ForkedProcessStringEventListener listener )
    {
        consoleEventListeners.put( BOOTERCODE_CONSOLE_INFO, requireNonNull( listener ) );
    }

    public void setConsoleErrorListener( ForkedProcessStackTraceEventListener listener )
    {
        consoleErrorEventListener = requireNonNull( listener );
    }

    public void setConsoleDebugListener( ForkedProcessStringEventListener listener )
    {
        consoleEventListeners.put( BOOTERCODE_CONSOLE_DEBUG, requireNonNull( listener ) );
    }

    public void setConsoleWarningListener( ForkedProcessStringEventListener listener )
    {
        consoleEventListeners.put( BOOTERCODE_CONSOLE_WARNING, requireNonNull( listener ) );
    }

    public void setByeListener( ForkedProcessEventListener listener )
    {
        controlEventListeners.put( BOOTERCODE_BYE, requireNonNull( listener ) );
    }

    public void setStopOnNextTestListener( ForkedProcessEventListener listener )
    {
        controlEventListeners.put( BOOTERCODE_STOP_ON_NEXT_TEST, requireNonNull( listener ) );
    }

    public void setAcquireNextTestListener( ForkedProcessEventListener listener )
    {
        controlEventListeners.put( BOOTERCODE_NEXT_TEST, requireNonNull( listener ) );
    }

    public void setExitErrorEventListener( ForkedProcessExitErrorListener listener )
    {
        exitErrorEventListener = requireNonNull( listener );
    }

    public void handleEvent( String line, ForkedChannelDecoderErrorHandler errorHandler )
    {
        if ( line == null || !line.startsWith( MAGIC_NUMBER ) )
        {
            errorHandler.handledError( line, null );
            return;
        }

        StringTokenizer tokenizer = new StringTokenizer( line.substring( MAGIC_NUMBER.length() ), ":" );
        String opcode = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
        ForkedProcessEvent event = opcode == null ? null : EVENTS.get( opcode );
        if ( event == null )
        {
            errorHandler.handledError( line, null );
            return;
        }

        try
        {
            if ( event.isControlCategory() )
            {
                ForkedProcessEventListener listener = controlEventListeners.get( event );
                if ( listener != null )
                {
                    listener.handle();
                }
            }
            else if ( event.isConsoleCategory() )
            {
                ForkedProcessStringEventListener listener = consoleEventListeners.get( event );
                Charset encoding = tokenizer.hasMoreTokens() ? Charset.forName( tokenizer.nextToken() ) : null;
                if ( listener != null && encoding != null )
                {
                    String msg = tokenizer.hasMoreTokens() ? decode( tokenizer.nextToken(), encoding ) : "";
                    listener.handle( msg );
                }
            }
            else if ( event.isConsoleErrorCategory() )
            {
                Charset encoding = tokenizer.hasMoreTokens() ? Charset.forName( tokenizer.nextToken() ) : null;
                if ( consoleErrorEventListener != null && encoding != null )
                {
                    String msg = tokenizer.hasMoreTokens() ? decode( tokenizer.nextToken(), encoding ) : null;
                    String smartStackTrace =
                            tokenizer.hasMoreTokens() ? decode( tokenizer.nextToken(), encoding ) : null;
                    String stackTrace = tokenizer.hasMoreTokens() ? decode( tokenizer.nextToken(), encoding ) : null;
                    consoleErrorEventListener.handle( msg, smartStackTrace, stackTrace );
                }
            }
            else if ( event.isStandardStreamCategory() )
            {
                ForkedProcessStandardOutErrEventListener listener = stdOutErrEventListeners.get( event );
                RunMode mode = tokenizer.hasMoreTokens() ? MODES.get( tokenizer.nextToken() ) : null;
                Charset encoding = tokenizer.hasMoreTokens() ? Charset.forName( tokenizer.nextToken() ) : null;
                if ( listener != null && encoding != null && mode != null )
                {
                    boolean newLine = event == BOOTERCODE_STDOUT_NEW_LINE || event == BOOTERCODE_STDERR_NEW_LINE;
                    String output = tokenizer.hasMoreTokens() ? decode( tokenizer.nextToken(), encoding ) : "";
                    listener.handle( mode, output, newLine );
                }
            }
            else if ( event.isSysPropCategory() )
            {
                RunMode mode = tokenizer.hasMoreTokens() ? MODES.get( tokenizer.nextToken() ) : null;
                Charset encoding = tokenizer.hasMoreTokens() ? Charset.forName( tokenizer.nextToken() ) : null;
                String key = tokenizer.hasMoreTokens() ? decode( tokenizer.nextToken(), encoding ) : "";
                if ( propertyEventListener != null && isNotBlank( key ) )
                {
                    String value = tokenizer.hasMoreTokens() ? decode( tokenizer.nextToken(), encoding ) : "";
                    propertyEventListener.handle( mode, key, value );
                }
            }
            else if ( event.isTestCategory() )
            {
                ForkedProcessReportEventListener listener = reportEventListeners.get( event );
                RunMode mode = tokenizer.hasMoreTokens() ? MODES.get( tokenizer.nextToken() ) : null;
                Charset encoding = tokenizer.hasMoreTokens() ? Charset.forName( tokenizer.nextToken() ) : null;
                if ( listener != null && encoding != null && mode != null )
                {
                    String sourceName = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
                    String sourceText = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
                    String name = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
                    String nameText = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
                    String group = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
                    String message = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
                    String elapsed = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
                    String traceMessage = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
                    String smartTrimmedStackTrace = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
                    String stackTrace = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
                    ReportEntry reportEntry = toReportEntry( encoding, sourceName, sourceText, name, nameText,
                            group, message, elapsed, traceMessage, smartTrimmedStackTrace, stackTrace );
                    listener.handle( mode, reportEntry );
                }
            }
            else if ( event.isJvmExitError() )
            {
                if ( exitErrorEventListener != null )
                {
                    Charset encoding = tokenizer.hasMoreTokens() ? Charset.forName( tokenizer.nextToken() ) : null;
                    String message = tokenizer.hasMoreTokens() ? decode( tokenizer.nextToken(), encoding ) : "";
                    String smartTrimmedStackTrace =
                            tokenizer.hasMoreTokens() ? decode( tokenizer.nextToken(), encoding ) : "";
                    String stackTrace = tokenizer.hasMoreTokens() ? decode( tokenizer.nextToken(), encoding ) : "";
                    exitErrorEventListener.handle( message, smartTrimmedStackTrace, stackTrace );
                }
            }
        }
        catch ( IllegalArgumentException e )
        {
            errorHandler.handledError( line, e );
        }
    }

    static ReportEntry toReportEntry( Charset encoding,
                   // ReportEntry:
                   String encSource, String encSourceText, String encName, String encNameText,
                                      String encGroup, String encMessage, String encTimeElapsed,
                   // StackTraceWriter:
                   String encTraceMessage, String encSmartTrimmedStackTrace, String encStackTrace )
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

    static String decode( String line, Charset encoding )
    {
        // ForkedChannelEncoder is encoding the stream with US_ASCII
        return line == null || "-".equals( line )
                ? null
                : new String( BASE64.decode( line.getBytes( US_ASCII ) ), encoding );
    }

    static Integer decodeToInteger( String line )
    {
        return line == null || "-".equals( line ) ? null : Integer.decode( line );
    }

    private static StackTraceWriter decodeTrace( Charset encoding, String encTraceMessage,
                                                 String encSmartTrimmedStackTrace, String encStackTrace )
    {
        if ( isBlank( encStackTrace ) || "-".equals( encStackTrace ) )
        {
            return null;
        }
        else
        {
            String traceMessage = decode( encTraceMessage, encoding );
            String stackTrace = decode( encStackTrace, encoding );
            String smartTrimmedStackTrace = decode( encSmartTrimmedStackTrace, encoding );
            return new DeserializedStacktraceWriter( traceMessage, smartTrimmedStackTrace, stackTrace );
        }
    }
}
