package org.apache.maven.surefire.report;

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

import org.apache.maven.surefire.util.internal.ImmutableMap;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * @author Kristian Rosenvold
 */
public class SimpleReportEntry
    implements TestSetReportEntry
{
    private final Map<String, String> systemProperties;

    private final String source;

    private final String name;

    private final StackTraceWriter stackTraceWriter;

    private final Integer elapsed;

    private final String message;

    public SimpleReportEntry()
    {
        this( null, null );
    }

    public SimpleReportEntry( String source, String name )
    {
        this( source, name, null, null );
    }

    public SimpleReportEntry( String source, String name, Map<String, String> systemProperties )
    {
        this( source, name, null, null, systemProperties );
    }

    private SimpleReportEntry( String source, String name, StackTraceWriter stackTraceWriter )
    {
        this( source, name, stackTraceWriter, null );
    }

    public SimpleReportEntry( String source, String name, Integer elapsed )
    {
        this( source, name, null, elapsed );
    }

    public SimpleReportEntry( String source, String name, String message )
    {
        this( source, name, null, null, message, Collections.<String, String>emptyMap() );
    }

    protected SimpleReportEntry( String source, String name, StackTraceWriter stackTraceWriter, Integer elapsed,
                                 String message, Map<String, String> systemProperties )
    {
        this.source = source;
        this.name = name;
        this.stackTraceWriter = stackTraceWriter;
        this.message = message;
        this.elapsed = elapsed;
        this.systemProperties = new ImmutableMap<>( systemProperties );
    }

    public SimpleReportEntry( String source, String name, StackTraceWriter stackTraceWriter, Integer elapsed )
    {
        this( source, name, stackTraceWriter, elapsed, Collections.<String, String>emptyMap() );
    }

    public SimpleReportEntry( String source, String name, StackTraceWriter stackTraceWriter, Integer elapsed,
                              Map<String, String> systemProperties )
    {
        this( source, name, stackTraceWriter, elapsed, safeGetMessage( stackTraceWriter ), systemProperties );
    }

    public static SimpleReportEntry assumption( String source, String name, String message )
    {
        return new SimpleReportEntry( source, name, message );
    }

    public static SimpleReportEntry ignored( String source, String name, String message )
    {
        return new SimpleReportEntry( source, name, message );
    }

    public static SimpleReportEntry withException( String source, String name, StackTraceWriter stackTraceWriter )
    {
        return new SimpleReportEntry( source, name, stackTraceWriter );
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
    public String getName()
    {
        return name;
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
        return "ReportEntry{" + "source='" + source + '\'' + ", name='" + name + '\'' + ", stackTraceWriter="
            + stackTraceWriter + ", elapsed=" + elapsed + ", message=" + message + '}';
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
        return isElapsedTimeEqual( that ) && isNameEqual( that ) && isSourceEqual( that ) && isStackEqual( that );
    }

    @Override
    public int hashCode()
    {
        int result = Objects.hashCode( source );
        result = 31 * result + Objects.hashCode( name );
        result = 31 * result + Objects.hashCode( stackTraceWriter );
        result = 31 * result + Objects.hashCode( elapsed );
        return result;
    }

    @Override
    public String getNameWithGroup()
    {
        return getSourceName();
    }

    @Override
    public Map<String, String> getSystemProperties()
    {
        return systemProperties;
    }

    private boolean isElapsedTimeEqual( SimpleReportEntry en )
    {
        return Objects.equals( elapsed, en.elapsed );
    }

    private boolean isNameEqual( SimpleReportEntry en )
    {
        return Objects.equals( name, en.name );
    }

    private boolean isSourceEqual( SimpleReportEntry en )
    {
        return Objects.equals( source, en.source );
    }

    private boolean isStackEqual( SimpleReportEntry en )
    {
        return Objects.equals( stackTraceWriter, en.stackTraceWriter );
    }
}
