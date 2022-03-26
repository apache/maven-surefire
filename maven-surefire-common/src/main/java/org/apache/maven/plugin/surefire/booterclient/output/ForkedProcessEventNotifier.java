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

import org.apache.maven.surefire.api.booter.ForkedProcessEventType;
import org.apache.maven.surefire.api.event.AbstractConsoleEvent;
import org.apache.maven.surefire.api.event.AbstractStandardStreamEvent;
import org.apache.maven.surefire.api.event.AbstractTestControlEvent;
import org.apache.maven.surefire.api.event.ConsoleErrorEvent;
import org.apache.maven.surefire.api.event.Event;
import org.apache.maven.surefire.api.event.JvmExitErrorEvent;
import org.apache.maven.surefire.api.event.SystemPropertyEvent;
import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.RunMode;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_BYE;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_CONSOLE_DEBUG;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_CONSOLE_INFO;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_CONSOLE_WARNING;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_NEXT_TEST;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_STDERR;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_STDERR_NEW_LINE;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_STDOUT;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_STDOUT_NEW_LINE;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_STOP_ON_NEXT_TEST;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_TESTSET_COMPLETED;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_TESTSET_STARTING;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_TEST_ASSUMPTIONFAILURE;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_TEST_ERROR;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_TEST_FAILED;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_TEST_SKIPPED;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_TEST_STARTING;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_TEST_SUCCEEDED;

/**
 * magic number : run mode : opcode [: opcode specific data]*
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
public final class ForkedProcessEventNotifier
{
    private volatile ForkedProcessPropertyEventListener propertyEventListener;
    private volatile ForkedProcessStackTraceEventListener consoleErrorEventListener;
    private volatile ForkedProcessExitErrorListener exitErrorEventListener;

    private final ConcurrentMap<ForkedProcessEventType, ForkedProcessReportEventListener<?>> reportEventListeners =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<ForkedProcessEventType, ForkedProcessStandardOutErrEventListener>
        stdOutErrEventListeners = new ConcurrentHashMap<>();

    private final ConcurrentMap<ForkedProcessEventType, ForkedProcessStringEventListener> consoleEventListeners =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<ForkedProcessEventType, ForkedProcessEventListener> controlEventListeners =
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

    public void notifyEvent( Event event )
    {
        ForkedProcessEventType eventType = event.getEventType();
        if ( event.isControlCategory() )
        {
            ForkedProcessEventListener listener = controlEventListeners.get( eventType );
            if ( listener != null )
            {
                listener.handle();
            }
        }
        else if ( event.isConsoleErrorCategory() )
        {
            if ( consoleErrorEventListener != null )
            {
                consoleErrorEventListener.handle( ( ( ConsoleErrorEvent ) event ).getStackTraceWriter() );
            }
        }
        else if ( event.isConsoleCategory() )
        {
            ForkedProcessStringEventListener listener = consoleEventListeners.get( eventType );
            if ( listener != null )
            {
                listener.handle( ( (AbstractConsoleEvent) event ).getMessage() );
            }
        }
        else if ( event.isStandardStreamCategory() )
        {
            boolean newLine = eventType == BOOTERCODE_STDOUT_NEW_LINE || eventType == BOOTERCODE_STDERR_NEW_LINE;
            AbstractStandardStreamEvent standardStreamEvent = (AbstractStandardStreamEvent) event;
            ForkedProcessStandardOutErrEventListener listener = stdOutErrEventListeners.get( eventType );
            if ( listener != null )
            {
                listener.handle( standardStreamEvent.getMessage(), newLine,
                    standardStreamEvent.getRunMode(), standardStreamEvent.getTestRunId() );
            }
        }
        else if ( event.isSysPropCategory() )
        {
            SystemPropertyEvent systemPropertyEvent = (SystemPropertyEvent) event;
            RunMode runMode = systemPropertyEvent.getRunMode();
            Long testRunId = systemPropertyEvent.getTestRunId();
            String key = systemPropertyEvent.getKey();
            String value = systemPropertyEvent.getValue();
            if ( propertyEventListener != null )
            {
                propertyEventListener.handle( key, value, runMode, testRunId );
            }
        }
        else if ( event.isTestCategory() )
        {
            ForkedProcessReportEventListener listener = reportEventListeners.get( eventType );
            AbstractTestControlEvent testControlEvent = (AbstractTestControlEvent) event;
            ReportEntry reportEntry = testControlEvent.getReportEntry();
            if ( listener != null )
            {
                listener.handle( reportEntry );
            }
        }
        else if ( event.isJvmExitError() )
        {
            JvmExitErrorEvent jvmExitErrorEvent = (JvmExitErrorEvent) event;
            if ( exitErrorEventListener != null )
            {
                exitErrorEventListener.handle( jvmExitErrorEvent.getStackTraceWriter() );
            }
        }
        else
        {
            throw new IllegalArgumentException( "Unknown event type " + eventType );
        }
    }
}
