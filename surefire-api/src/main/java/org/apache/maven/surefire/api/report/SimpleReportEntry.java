package org.apache.maven.surefire.api.report;

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

import org.apache.maven.surefire.api.util.internal.ImmutableMap;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Basic implementation of {@link TestSetReportEntry} (immutable and thread-safe object).
 *
 * @author Kristian Rosenvold
 */
public class SimpleReportEntry
    implements TestSetReportEntry
{
    private final RunMode runMode;

    private final Long testRunId;

    private final Map<String, String> systemProperties;

    private final String source;

    private final String sourceText;

    private final String name;

    private final String nameText;

    private final StackTraceWriter stackTraceWriter;

    private final Integer elapsed;

    private final String message;

    public SimpleReportEntry( @Nonnull RunMode runMode, Long testRunId,
                              String source, String sourceText, String name, String nameText )
    {
        this( runMode, testRunId, source, sourceText, name, nameText, null, null );
    }

    public SimpleReportEntry( @Nonnull RunMode runMode, Long testRunId,
                              String source, String sourceText, String name, String nameText,
                              Map<String, String> systemProperties )
    {
        this( runMode, testRunId, source, sourceText, name, nameText, null, null, systemProperties );
    }

    private SimpleReportEntry( @Nonnull RunMode runMode, Long testRunId,
                               String source, String sourceText, String name, String nameText,
                               StackTraceWriter stackTraceWriter )
    {
        this( runMode, testRunId, source, sourceText, name, nameText, stackTraceWriter, null );
    }

    public SimpleReportEntry( @Nonnull RunMode runMode, Long testRunId,
                              String source, String sourceText, String name, String nameText, Integer elapsed )
    {
        this( runMode, testRunId, source, sourceText, name, nameText, null, elapsed );
    }

    public SimpleReportEntry( @Nonnull RunMode runMode, Long testRunId,
                              String source, String sourceText, String name, String nameText, String message )
    {
        this( runMode, testRunId,
            source, sourceText, name, nameText, null, null, message, Collections.<String, String>emptyMap() );
    }

    public SimpleReportEntry( @Nonnull RunMode runMode, Long testRunId,
                              String source, String sourceText, String name, String nameText,
                              StackTraceWriter stackTraceWriter, Integer elapsed, String message,
                              Map<String, String> systemProperties )
    {
        this.runMode = runMode;
        this.testRunId = testRunId;
        this.source = source;
        this.sourceText = sourceText;
        this.name = name;
        this.nameText = nameText;
        this.stackTraceWriter = stackTraceWriter;
        this.message = message;
        this.elapsed = elapsed;
        this.systemProperties = new ImmutableMap<>( systemProperties );
    }

    public SimpleReportEntry( @Nonnull RunMode runMode, Long testRunId,
                               String source, String sourceText, String name, String nameText,
                               StackTraceWriter stackTraceWriter, Integer elapsed )
    {
        this( runMode, testRunId,
            source, sourceText, name, nameText, stackTraceWriter, elapsed, Collections.<String, String>emptyMap() );
    }

    public SimpleReportEntry( @Nonnull RunMode runMode, Long testRunId,
                              String source, String sourceText, String name, String nameText,
                              StackTraceWriter stackTraceWriter, Integer elapsed, Map<String, String> systemProperties )
    {
        this( runMode, testRunId, source, sourceText, name, nameText,
            stackTraceWriter, elapsed, safeGetMessage( stackTraceWriter ), systemProperties );
    }

    public static SimpleReportEntry assumption( RunMode runMode, Long testRunId, String source,
                                                String sourceText, String name, String nameText, String message )
    {
        return new SimpleReportEntry( runMode, testRunId, source, sourceText, name, nameText, message );
    }

    public static SimpleReportEntry ignored( RunMode runMode, Long testRunId, String source,
                                             String sourceText, String name, String nameText, String message )
    {
        return new SimpleReportEntry( runMode, testRunId, source, sourceText, name, nameText, message );
    }

    public static SimpleReportEntry withException( RunMode runMode, Long testRunId, String source,
                                                   String sourceText, String name, String nameText,
                                                   StackTraceWriter stackTraceWriter )
    {
        return new SimpleReportEntry( runMode, testRunId, source, sourceText, name, nameText, stackTraceWriter );
    }

    private static String safeGetMessage( StackTraceWriter stackTraceWriter )
    {
        try
        {
            SafeThrowable t = stackTraceWriter == null ? null : stackTraceWriter.getThrowable();
            return t == null ? null : t.getMessage();
        }
        catch ( Throwable t )
        {
            return t.getMessage();
        }
    }

    @Override
    public String getSourceName()
    {
        return source;
    }

    @Override
    public String getSourceText()
    {
        return sourceText;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getNameText()
    {
        return nameText;
    }

    @Override
    public String getGroup()
    {
        return null;
    }

    @Override
    public StackTraceWriter getStackTraceWriter()
    {
        return stackTraceWriter;
    }

    @Override
    public Integer getElapsed()
    {
        return elapsed;
    }

    @Override
    public int getElapsed( int fallback )
    {
        return elapsed == null ? fallback : elapsed;
    }

    @Override
    public String toString()
    {
        return "ReportEntry{" + "runMode='" + runMode + "', testRunId='" + testRunId
            + "', source='" + source + "', sourceText='" + sourceText
            + "', name='" + name + "', nameText='" + nameText + "', stackTraceWriter='" + stackTraceWriter
            + "', elapsed='" + elapsed + "', message='" + message + "'}";
    }

    @Override
    public String getMessage()
    {
        return message;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        SimpleReportEntry that = (SimpleReportEntry) o;
        return isRunModeEqual( that ) && isTestRunIdEqual( that )
                && isSourceEqual( that ) && isSourceTextEqual( that )
                && isNameEqual( that ) && isNameTextEqual( that )
                && isStackEqual( that )
                && isElapsedTimeEqual( that )
                && isSystemPropertiesEqual( that )
                && isMessageEqual( that );
    }

    @Override
    public int hashCode()
    {
        int result = Objects.hashCode( getSourceName() );
        result = 31 * result + Objects.hashCode( getRunMode() );
        result = 31 * result + Objects.hashCode( getTestRunId() );
        result = 31 * result + Objects.hashCode( getSourceText() );
        result = 31 * result + Objects.hashCode( getName() );
        result = 31 * result + Objects.hashCode( getNameText() );
        result = 31 * result + Objects.hashCode( getStackTraceWriter() );
        result = 31 * result + Objects.hashCode( getElapsed() );
        result = 31 * result + Objects.hashCode( getSystemProperties() );
        result = 31 * result + Objects.hashCode( getMessage() );
        return result;
    }

    @Override
    public String getNameWithGroup()
    {
        return getSourceName();
    }

    @Override
    public String getReportNameWithGroup()
    {
        return getSourceText();
    }

    @Override
    @Nonnull
    public final RunMode getRunMode()
    {
        return runMode;
    }

    @Override
    public final Long getTestRunId()
    {
        return testRunId;
    }

    @Override
    public Map<String, String> getSystemProperties()
    {
        return systemProperties;
    }

    private boolean isRunModeEqual( SimpleReportEntry en )
    {
        return Objects.equals( getRunMode(), en.getRunMode() );
    }

    private boolean isTestRunIdEqual( SimpleReportEntry en )
    {
        return Objects.equals( getTestRunId(), en.getTestRunId() );
    }

    private boolean isElapsedTimeEqual( SimpleReportEntry en )
    {
        return Objects.equals( getElapsed(), en.getElapsed() );
    }

    private boolean isNameTextEqual( SimpleReportEntry en )
    {
        return Objects.equals( getNameText(), en.getNameText() );
    }

    private boolean isNameEqual( SimpleReportEntry en )
    {
        return Objects.equals( getName(), en.getName() );
    }

    private boolean isSourceEqual( SimpleReportEntry en )
    {
        return Objects.equals( getSourceName(), en.getSourceName() );
    }

    private boolean isSourceTextEqual( SimpleReportEntry en )
    {
        return Objects.equals( getSourceText(), en.getSourceText() );
    }

    private boolean isStackEqual( SimpleReportEntry en )
    {
        return Objects.equals( getStackTraceWriter(), en.getStackTraceWriter() );
    }

    private boolean isSystemPropertiesEqual( SimpleReportEntry en )
    {
        return Objects.equals( getSystemProperties(), en.getSystemProperties() );
    }

    private boolean isMessageEqual( SimpleReportEntry en )
    {
        return Objects.equals( getMessage(), en.getMessage() );
    }
}
