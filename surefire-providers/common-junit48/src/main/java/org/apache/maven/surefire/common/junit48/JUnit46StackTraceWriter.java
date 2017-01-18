package org.apache.maven.surefire.common.junit48;

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

import org.apache.maven.surefire.common.junit4.JUnit4RunListener;
import org.apache.maven.surefire.common.junit4.JUnit4StackTraceWriter;

import org.junit.runner.notification.Failure;

/**
 * A stacktrace writer that requires at least junit 4.6 to run. Note that we only use this for 4.8 and higher
 * <p/>
 * Writes out a specific {@link org.junit.runner.notification.Failure} for
 * surefire as a stacktrace.
 *
 * @author Karl M. Davis
 * @author Kristian Rosenvold
 */
public class JUnit46StackTraceWriter
    extends JUnit4StackTraceWriter
{

    /**
     * Constructor.
     *
     * @param junitFailure the {@link org.junit.runner.notification.Failure} that this will be operating on
     */
    public JUnit46StackTraceWriter( Failure junitFailure )
    {
        super( junitFailure );
    }


    protected final String getTestClassName()
    {
        return JUnit4RunListener.extractClassName( junitFailure.getDescription() );
    }
}
