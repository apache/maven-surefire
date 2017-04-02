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
 * Write the trace out for a POJO test.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class PojoStackTraceWriter
    implements StackTraceWriter
{
    private final Throwable t;

    private final String testClass;

    private final String testMethod;

    public PojoStackTraceWriter( String testClass, String testMethod, Throwable t )
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
        return t == null ? "" : new SmartStackTraceParser( testClass, t, testMethod ).getString();
    }

    public String writeTrimmedTraceToString()
    {
        return t == null ? "" : SmartStackTraceParser.stackTraceWithFocusOnClassAsString( t, testClass );
    }

    public SafeThrowable getThrowable()
    {
        return t == null ? null : new SafeThrowable( t );
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
}
