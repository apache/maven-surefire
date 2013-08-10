package org.apache.maven.plugin.surefire.report;

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

import java.text.NumberFormat;
import java.util.Locale;

import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.StackTraceWriter;

/**
 * @author Kristian Rosenvold
 */
public class WrappedReportEntry
    implements ReportEntry
{
    private final ReportEntry original;

    private final ReportEntryType reportEntryType;

    private final Integer elapsed;

    private final Utf8RecodingDeferredFileOutputStream stdout;

    private final Utf8RecodingDeferredFileOutputStream stdErr;

    private final NumberFormat numberFormat = NumberFormat.getInstance( Locale.ENGLISH );

    private static final int MS_PER_SEC = 1000;

    static final String NL = System.getProperty( "line.separator" );

    public WrappedReportEntry( ReportEntry original, ReportEntryType reportEntryType, Integer estimatedElapsed,
                               Utf8RecodingDeferredFileOutputStream stdout, Utf8RecodingDeferredFileOutputStream stdErr )
    {
        this.original = original;
        this.reportEntryType = reportEntryType;
        this.elapsed = estimatedElapsed;
        this.stdout = stdout;
        this.stdErr = stdErr;
    }

    public Integer getElapsed()
    {
        return elapsed;
    }

    public ReportEntryType getReportEntryType()
    {
        return reportEntryType;
    }

    public Utf8RecodingDeferredFileOutputStream getStdout()
    {
        return stdout;
    }

    public Utf8RecodingDeferredFileOutputStream getStdErr()
    {
        return stdErr;
    }

    public String getSourceName()
    {
        return original.getSourceName();
    }

    public String getName()
    {
        return original.getName();
    }

    public String getGroup()
    {
        return original.getGroup();
    }

    public StackTraceWriter getStackTraceWriter()
    {
        return original.getStackTraceWriter();
    }

    public String getMessage()
    {
        return original.getMessage();
    }

    public String getStackTrace( boolean trimStackTrace )
    {
        StackTraceWriter writer = original.getStackTraceWriter();
        if ( writer == null )
        {
            return null;
        }
        return trimStackTrace ? writer.writeTrimmedTraceToString() : writer.writeTraceToString();
    }

    public String elapsedTimeAsString()
    {
        return elapsedTimeAsString( getElapsed() );
    }

    String elapsedTimeAsString( long runTime )
    {
        return numberFormat.format( (double) runTime / MS_PER_SEC );
    }

    public String getReportName()
    {
        final int i = getName().lastIndexOf( "(" );
        return i > 0 ? getName().substring( 0, i ) : getName();
    }

    public String getReportName( String suffix )
    {
        return suffix != null && suffix.length() > 0 ? getReportName() + "(" + suffix + ")" : getReportName();
    }

    public String getOutput( boolean trimStackTrace )
    {
        StringBuilder buf = new StringBuilder();

        buf.append( getElapsedTimeSummary() );

        buf.append( "  <<< " ).append( getReportEntryType().toString().toUpperCase() ).append( "!" ).append( NL );

        buf.append( getStackTrace( trimStackTrace ) );

        return buf.toString();
    }

    public String getElapsedTimeSummary()
    {
        StringBuilder reportContent = new StringBuilder();
        reportContent.append( getName() );
        reportContent.append( "  Time elapsed: " );
        reportContent.append( elapsedTimeAsString() );
        reportContent.append( " sec" );

        return reportContent.toString();
    }

    public boolean isErrorOrFailure()
    {
        ReportEntryType thisType = getReportEntryType();
        return ReportEntryType.failure == thisType || ReportEntryType.error == thisType;
    }

    public boolean isSkipped()
    {
        return ReportEntryType.skipped == getReportEntryType();
    }

    public boolean isSucceeded()
    {
        return ReportEntryType.success == getReportEntryType();
    }

    public String getNameWithGroup()
    {
        return original.getNameWithGroup();
    }
}
