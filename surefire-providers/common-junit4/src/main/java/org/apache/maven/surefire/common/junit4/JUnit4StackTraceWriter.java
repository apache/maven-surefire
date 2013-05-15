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

import org.junit.runner.notification.Failure;

/**
 * Writes out a specific {@link org.junit.runner.notification.Failure} for
 * surefire as a stacktrace.
 *
 * @author Karl M. Davis
 */
public class JUnit4StackTraceWriter
    implements StackTraceWriter
{
    // Member Variables
    protected final Failure junitFailure;

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
    public String writeTraceToString()
    {
        return junitFailure.getTrace();
    }


    protected String getTestClassName()
    {
        return JUnit4RunListener.extractClassName( junitFailure.getDescription() );
    }

    protected String getTestMethodName()
    {
        return JUnit4RunListener.extractMethodName( junitFailure.getDescription() );
    }

    @SuppressWarnings( "ThrowableResultOfMethodCallIgnored" )
    public String smartTrimmedStackTrace()
    {
        Throwable exception = junitFailure.getException();
        if ( exception != null )
        {
            SmartStackTraceParser smartStackTraceParser = new SmartStackTraceParser( getTestClassName(), exception,
                                                                                     getTestMethodName() );
            return smartStackTraceParser.getString();
        }
        else
        {
            return junitFailure.getMessage();
        }
    }

    /**
     * At the moment, returns the same as {@link #writeTraceToString()}.
     *
     * @see org.apache.maven.surefire.report.StackTraceWriter#writeTrimmedTraceToString()
     */
    public String writeTrimmedTraceToString()
    {
        String testClass = getTestClassName();
        try
        {
            return SmartStackTraceParser.innerMostWithFocusOnClass( junitFailure.getException(), testClass );
        }
        catch ( Throwable t )
        {
            return SmartStackTraceParser.innerMostWithFocusOnClass( t, testClass );
        }
    }

    /**
     * Returns the exception associated with this failure.
     *
     * @see org.apache.maven.surefire.report.StackTraceWriter#getThrowable()
     */
    public SafeThrowable getThrowable()
    {
        return new SafeThrowable( junitFailure.getException() );
    }

}
