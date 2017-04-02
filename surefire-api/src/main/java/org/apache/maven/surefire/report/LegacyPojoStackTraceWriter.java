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


import org.apache.maven.surefire.util.internal.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Write the trace out for a POJO test. Java 1.5 compatible.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @noinspection ThrowableResultOfMethodCallIgnored
 */
public class LegacyPojoStackTraceWriter
    implements StackTraceWriter
{
    private static final int MAX_LINE_LENGTH = 77;

    private final Throwable t;

    private final String testClass;

    private final String testMethod;

    public LegacyPojoStackTraceWriter( String testClass, String testMethod, Throwable t )
    {
        this.testClass = testClass;
        this.testMethod = testMethod;
        this.t = t;
    }

    public String writeTraceToString()
    {
        if ( t != null )
        {
            StringWriter w = new StringWriter();
            PrintWriter stackTrace = new PrintWriter( w );
            try
            {
                t.printStackTrace( stackTrace );
                stackTrace.flush();
            }
            finally
            {
                stackTrace.close();
            }
            StringBuffer builder = w.getBuffer();
            if ( isMultiLineExceptionMessage( t ) )
            {
                // SUREFIRE-986
                String exc = t.getClass().getName() + ": ";
                if ( StringUtils.startsWith( builder, exc ) )
                {
                    builder.insert( exc.length(), '\n' );
                }
            }
            return builder.toString();
        }
        return "";
    }

    public String smartTrimmedStackTrace()
    {
        StringBuilder result = new StringBuilder();
        result.append( testClass );
        result.append( "#" );
        result.append( testMethod );
        SafeThrowable throwable = getThrowable();
        if ( throwable.getTarget() instanceof AssertionError )
        {
            result.append( " " );
            result.append( getTruncatedMessage( throwable.getMessage(), MAX_LINE_LENGTH - result.length() ) );
        }
        else
        {
            Throwable target = throwable.getTarget();
            if ( target != null )
            {
                result.append( " " );
                result.append( target.getClass().getSimpleName() );
                result.append( getTruncatedMessage( throwable.getMessage(), MAX_LINE_LENGTH - result.length() ) );
            }
        }
        return result.toString();
    }

    private static boolean isMultiLineExceptionMessage( Throwable t )
    {
        String msg = t.getLocalizedMessage();
        if ( msg != null )
        {
            int countNewLines = 0;
            for ( int i = 0, length = msg.length(); i < length; i++ )
            {
                if ( msg.charAt( i ) == '\n' )
                {
                    if ( ++countNewLines == 2 )
                    {
                        break;
                    }
                }
            }
            return countNewLines > 1 || countNewLines == 1 && !msg.trim().endsWith( "\n" );
        }
        return false;
    }

    private static String getTruncatedMessage( String msg, int i )
    {
        if ( i < 0 )
        {
            return "";
        }
        if ( msg == null )
        {
            return "";
        }
        String substring = msg.substring( 0, Math.min( i, msg.length() ) );
        if ( i < msg.length() )
        {
            return " " + substring + "...";
        }
        else
        {
            return " " + substring;
        }
    }


    public String writeTrimmedTraceToString()
    {
        String text = writeTraceToString();

        String marker = "at " + testClass + "." + testMethod;

        String[] lines = StringUtils.split( text, "\n" );
        int lastLine = lines.length - 1;
        int causedByLine = -1;
        // skip first
        for ( int i = 1; i < lines.length; i++ )
        {
            String line = lines[i].trim();
            if ( line.startsWith( marker ) )
            {
                lastLine = i;
            }
            else if ( line.startsWith( "Caused by" ) )
            {
                causedByLine = i;
                break;
            }
        }

        StringBuilder trace = new StringBuilder();
        for ( int i = 0; i <= lastLine; i++ )
        {
            trace.append( lines[i] );
            trace.append( "\n" );
        }

        if ( causedByLine != -1 )
        {
            for ( int i = causedByLine; i < lines.length; i++ )
            {
                trace.append( lines[i] );
                trace.append( "\n" );
            }
        }
        return trace.toString();
    }

    public SafeThrowable getThrowable()
    {
        return new SafeThrowable( t );
    }
}
