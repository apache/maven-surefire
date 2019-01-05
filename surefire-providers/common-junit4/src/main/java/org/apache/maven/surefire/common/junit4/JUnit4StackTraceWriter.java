package org.apache.maven.surefire.common.junit4;

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

import org.apache.maven.surefire.report.SafeThrowable;
import org.apache.maven.surefire.report.SmartStackTraceParser;
import org.apache.maven.surefire.report.StackTraceWriter;
import org.apache.maven.surefire.util.internal.ClassMethod;
import org.junit.runner.notification.Failure;

import static org.apache.maven.surefire.common.junit4.JUnit4ProviderUtil.toClassMethod;
import static org.apache.maven.surefire.report.SmartStackTraceParser.stackTraceWithFocusOnClassAsString;

/**
 * Writes out a specific {@link org.junit.runner.notification.Failure} for
 * surefire as a stacktrace.
 *
 * @author Karl M. Davis
 */
public class JUnit4StackTraceWriter
    implements StackTraceWriter
{
    private final Failure junitFailure;

    /**
     * Constructor.
     *
     * @param junitFailure the {@link Failure} that this will be operating on
     */
    public JUnit4StackTraceWriter( Failure junitFailure )
    {
        this.junitFailure = junitFailure;
    }

    /*
      * (non-Javadoc)
      *
      * @see org.apache.maven.surefire.report.StackTraceWriter#writeTraceToString()
      */
    @Override
    public String writeTraceToString()
    {
        Throwable t = junitFailure.getException();
        if ( t != null )
        {
            String originalTrace = junitFailure.getTrace();
            if ( isMultiLineExceptionMessage( t ) )
            {
                // SUREFIRE-986
                StringBuilder builder = new StringBuilder( originalTrace );
                String exc = t.getClass().getName() + ": ";
                if ( originalTrace.startsWith( exc ) )
                {
                    builder.insert( exc.length(), '\n' );
                }
                return builder.toString();
            }
            return originalTrace;
        }
        return "";
    }

    @Override
    public String smartTrimmedStackTrace()
    {
        Throwable exception = junitFailure.getException();
        ClassMethod classMethod = toClassMethod( junitFailure.getDescription() );
        return exception == null
            ? junitFailure.getMessage()
            : new SmartStackTraceParser( classMethod.getClazz(), exception, classMethod.getMethod() ).getString();
    }

    /**
     * At the moment, returns the same as {@link #writeTraceToString()}.
     *
     * @see org.apache.maven.surefire.report.StackTraceWriter#writeTrimmedTraceToString()
     */
    @Override
    public String writeTrimmedTraceToString()
    {
        String testClass = toClassMethod( junitFailure.getDescription() ).getClazz();
        try
        {
            Throwable e = junitFailure.getException();
            return stackTraceWithFocusOnClassAsString( e, testClass );
        }
        catch ( Throwable t )
        {
            return stackTraceWithFocusOnClassAsString( t, testClass );
        }
    }

    /**
     * Returns the exception associated with this failure.
     *
     * @see org.apache.maven.surefire.report.StackTraceWriter#getThrowable()
     */
    @Override
    public SafeThrowable getThrowable()
    {
        return new SafeThrowable( junitFailure.getException() );
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
