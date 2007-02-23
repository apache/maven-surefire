package org.apache.maven.surefire.junit4;

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
    private Failure junitFailure;

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

    /**
     * At the moment, returns the same as {@link #writeTraceToString()}.
     *
     * @see org.apache.maven.surefire.report.StackTraceWriter#writeTrimmedTraceToString()
     */
    public String writeTrimmedTraceToString()
    {
        return junitFailure.getTrace();
    }

    /**
     * Returns the exception associated with this failure.
     *
     * @see org.apache.maven.surefire.report.StackTraceWriter#getThrowable()
     */
    public Throwable getThrowable()
    {
        return junitFailure.getException();
    }

}
