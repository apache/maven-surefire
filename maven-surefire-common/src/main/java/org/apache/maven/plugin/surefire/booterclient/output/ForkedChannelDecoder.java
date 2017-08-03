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

import org.apache.maven.surefire.booter.ForkedProcessEvent;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunMode;
import org.apache.maven.surefire.report.StackTraceWriter;

import java.nio.charset.Charset;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static javax.xml.bind.DatatypeConverter.parseBase64Binary;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.MAGIC_NUMBER;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_BYE;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_CONSOLE_INFO;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_CONSOLE_DEBUG;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_NEXT_TEST;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_STDERR;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_STDOUT;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_STOP_ON_NEXT_TEST;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TESTSET_COMPLETED;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TESTSET_STARTING;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TEST_ASSUMPTIONFAILURE;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TEST_ERROR;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TEST_FAILED;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TEST_SKIPPED;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TEST_STARTING;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TEST_SUCCEEDED;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_CONSOLE_WARNING;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.EVENTS;
import static org.apache.maven.surefire.report.CategorizedReportEntry.reportEntry;
import static org.apache.maven.surefire.report.RunMode.MODES;
import static org.apache.maven.surefire.util.internal.ObjectUtils.requireNonNull;
import static org.apache.maven.surefire.util.internal.StringUtils.isBlank;
import static org.apache.maven.surefire.util.internal.StringUtils.isNotBlank;

/**
 * magic number : opcode : run mode [: opcode specific data]*
 * <p/>
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
 */
public final class ForkedChannelDecoder
{
    private static final byte[] EMPTY = {};

    private volatile ForkedProcessPropertyEventListener propertyEventListener;
    private volatile ForkedProcessStackTraceEventListener consoleErrorEventListener;
    private volatile ForkedProcessExitErrorListener exitErrorEventListener;

    private final ConcurrentMap<ForkedProcessEvent, ForkedProcessReportEventListener> reportEventListeners =
            new ConcurrentHashMap<ForkedProcessEvent, ForkedProcessReportEventListener>();

    private final ConcurrentMap<ForkedProcessEvent, ForkedProcessBinaryEventListener> binaryEventListeners =
            new ConcurrentHashMap<ForkedProcessEvent, ForkedProcessBinaryEventListener>();

    private final ConcurrentMap<ForkedProcessEvent, ForkedProcessStringEventListener> consoleEventListeners =
            new ConcurrentHashMap<ForkedProcessEvent, ForkedProcessStringEventListener>();

    private final ConcurrentMap<ForkedProcessEvent, ForkedProcessEventListener> controlEventListeners =
            new ConcurrentHashMap<ForkedProcessEvent, ForkedProcessEventListener>();

    public void setSystemPropertiesListener( ForkedProcessPropertyEventListener listener )
    {
        propertyEventListener = requireNonNull( listener );
    }

    public void setTestSetStartingListener( ForkedProcessReportEventListener listener )
    {
        reportEventListeners.put( BOOTERCODE_TESTSET_STARTING, requireNonNull( listener ) );
    }

    public void setTestSetCompletedListener( ForkedProcessReportEventListener listener )
    {
        reportEventListeners.put( BOOTERCODE_TESTSET_COMPLETED, requireNonNull( listener ) );
    }

    public void setTestStartingListener( ForkedProcessReportEventListener listener )
    {
        reportEventListeners.put( BOOTERCODE_TEST_STARTING, requireNonNull( listener ) );
    }

    public void setTestSucceededListener( ForkedProcessReportEventListener listener )
    {
        reportEventListeners.put( BOOTERCODE_TEST_SUCCEEDED, requireNonNull( listener ) );
    }

    public void setTestFailedListener( ForkedProcessReportEventListener listener )
    {
        reportEventListeners.put( BOOTERCODE_TEST_FAILED, requireNonNull( listener ) );
    }

    public void setTestSkippedListener( ForkedProcessReportEventListener listener )
    {
        reportEventListeners.put( BOOTERCODE_TEST_SKIPPED, requireNonNull( listener ) );
    }

    public void setTestErrorListener( ForkedProcessReportEventListener listener )
    {
        reportEventListeners.put( BOOTERCODE_TEST_ERROR, requireNonNull( listener ) );
    }

    public void setTestAssumptionFailureListener( ForkedProcessReportEventListener listener )
    {
        reportEventListeners.put( BOOTERCODE_TEST_ASSUMPTIONFAILURE, requireNonNull( listener ) );
    }

    public void setStdOutListener( ForkedProcessBinaryEventListener listener )
    {
        binaryEventListeners.put( BOOTERCODE_STDOUT, requireNonNull( listener ) );
    }

    public void setStdErrListener( ForkedProcessBinaryEventListener listener )
    {
        binaryEventListeners.put( BOOTERCODE_STDERR, requireNonNull( listener ) );
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
        if ( opcode != null && EVENTS.containsKey( opcode ) )
        {
            final ForkedProcessEvent event = EVENTS.get( opcode );
            try
            {
                if ( event.isControlCategory() )
                {
                    ForkedProcessEventListener listener = controlEventListeners.get( event );
                    if ( listener != null )
                    {
                        listener.handle();
                        return;
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
                        return;
                    }
                }
                else if ( event.isConsoleErrorCategory() )
                {
                    Charset encoding = tokenizer.hasMoreTokens() ? Charset.forName( tokenizer.nextToken() ) : null;
                    if ( consoleErrorEventListener != null && encoding != null )
                    {
                        String msg = tokenizer.hasMoreTokens() ? decode( tokenizer.nextToken(), encoding ) : "";
                        String stackTrace = tokenizer.hasMoreTokens() ? decode( tokenizer.nextToken(), encoding ) : "";
                        consoleErrorEventListener.handle( msg, stackTrace );
                        return;
                    }
                }
                else if ( event.isStandardStreamCategory() )
                {
                    ForkedProcessBinaryEventListener listener = binaryEventListeners.get( event );
                    Charset encoding = tokenizer.hasMoreTokens() ? Charset.forName( tokenizer.nextToken() ) : null;
                    RunMode mode = tokenizer.hasMoreTokens() ? MODES.get( tokenizer.nextToken() ) : null;
                    if ( listener != null && encoding != null && mode != null )
                    {
                        byte[] stream = tokenizer.hasMoreTokens() ? decodeToBytes( tokenizer.nextToken() ) : EMPTY;
                        listener.handle( mode, encoding, stream );
                        return;
                    }
                }
                else if ( event.isSysPropCategory() )
                {
                    Charset encoding = tokenizer.hasMoreTokens() ? Charset.forName( tokenizer.nextToken() ) : null;
                    String key = tokenizer.hasMoreTokens() ? decode( tokenizer.nextToken(), encoding ) : "";
                    if ( propertyEventListener != null && encoding != null && isNotBlank( key ) )
                    {
                        String value = tokenizer.hasMoreTokens() ? decode( tokenizer.nextToken(), encoding ) : "";
                        propertyEventListener.handle( key, value );
                        return;
                    }
                }
                else if ( event.isTestCategory() )
                {
                    ForkedProcessReportEventListener listener = reportEventListeners.get( event );
                    Charset encoding = tokenizer.hasMoreTokens() ? Charset.forName( tokenizer.nextToken() ) : null;
                    RunMode mode = tokenizer.hasMoreTokens() ? MODES.get( tokenizer.nextToken() ) : null;
                    if ( listener != null && encoding != null && mode != null )
                    {
                        String sourceName = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
                        String name = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
                        String group = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
                        String message = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
                        String elapsed = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
                        String traceMessage = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
                        String smartTrimmedStackTrace = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
                        String stackTrace = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;

                        listener.handle( mode, toReportEntry( encoding, sourceName, name, group, message, elapsed,
                                                              traceMessage, smartTrimmedStackTrace, stackTrace ) );
                        return;
                    }
                }
                else if ( event.isJvmExitError() )
                {//run mode nedavat vsade
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
                return;
            }
        }

        errorHandler.handledError( line, null );
    }

    static ReportEntry
    toReportEntry( Charset encoding,
                   // ReportEntry:
                   String encSource, String encName, String encGroup, String encMessage, String encTimeElapsed,
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
        String name = decode( encName, encoding );
        String group = decode( encGroup, encoding );
        StackTraceWriter stackTraceWriter =
                decodeTrace( encoding, encTraceMessage, encSmartTrimmedStackTrace, encStackTrace );
        Integer elapsed = decodeToInteger( encTimeElapsed );
        String message = decode( encMessage, encoding );
        return reportEntry( source, name, group, stackTraceWriter, elapsed, message );
    }

    static String decode( String line, Charset encoding )
    {
        return line == null || "-".equals( line ) ? null : new String( parseBase64Binary( line ), encoding );
    }

    static Integer decodeToInteger( String line )
    {
        return line == null || "-".equals( line ) ? null : Integer.decode( line );
    }

    static byte[] decodeToBytes( String line )
    {
        return "-".equals( line ) ? null : parseBase64Binary( line );
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
