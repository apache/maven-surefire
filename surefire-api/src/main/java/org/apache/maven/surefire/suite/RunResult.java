package org.apache.maven.surefire.suite;

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

import java.util.StringTokenizer;

/**
 * Represents a test-run-result; this may be from a single test run or an aggregated result.
 *
 * @author Kristian Rosenvold
 */
public class RunResult
{
    private final int completedCount;

    private final int errors;

    private final int failures;

    private final int skipped;

    private final boolean failure;

    private final boolean timeout;

    public static final int SUCCESS = 0;

    private static final int FAILURE = 255;

    private static final int NO_TESTS = 254;

    public static final RunResult Timeout = new RunResult( 0, 0, 0, 0, false, true );

    public static final RunResult Failure = new RunResult( 0, 0, 0, 0, true, false);

    public RunResult( int completedCount, int errors, int failures, int skipped )
    {
        this( completedCount, errors, failures, skipped, false, false );
    }

    public RunResult( int completedCount, int errors, int failures, int skipped, boolean failure, boolean timeout )
    {
        this.completedCount = completedCount;
        this.errors = errors;
        this.failures = failures;
        this.skipped = skipped;
        this.failure = failure;
        this.timeout = timeout;
    }

    public int getCompletedCount()
    {
        return completedCount;
    }

    public int getErrors()
    {
        return errors;
    }

    public int getFailures()
    {
        return failures;
    }

    public int getSkipped()
    {
        return skipped;
    }

    public int getForkedProcessCode()
    {
        return completedCount == 0 ? NO_TESTS : isErrorFree() ? SUCCESS : FAILURE;
    }

    public boolean isErrorFree()
    {
        return getFailures() == 0 && getErrors() == 0;
    }

    public String getAsString()
    {
        return getCompletedCount() + "," + getErrors() + "," + getFailures() + "," + getSkipped() + "," + isFailure()
            + "," + isTimeout();
    }

    public static RunResult fromString( String string )
    {
        StringTokenizer strTok = new StringTokenizer( string, "," );
        int completed = Integer.parseInt( strTok.nextToken() );
        int errors = Integer.parseInt( strTok.nextToken() );
        int failures = Integer.parseInt( strTok.nextToken() );
        int skipped = Integer.parseInt( strTok.nextToken() );
        boolean isFailure = Boolean.parseBoolean( strTok.nextToken() );
        boolean isTimeout = Boolean.parseBoolean( strTok.nextToken() );
        return new RunResult( completed, errors, failures, skipped, isFailure, isTimeout );
    }

    public boolean isFailureOrTimeout()
    {
        return this.timeout || this.failure;
    }

    public boolean isFailure()
    {
        return failure;
    }

    public boolean isTimeout()
    {
        return timeout;
    }


    public RunResult aggregate( RunResult other )
    {
        boolean failure = isFailure() || other.isFailure();
        boolean timeout = isTimeout() || other.isTimeout();
        int completed = getCompletedCount() + other.getCompletedCount();
        int fail = getFailures() + other.getFailures();
        int ign = getSkipped() + other.getSkipped();
        int err = getErrors() + other.getErrors();
        return new RunResult( completed, err, fail, ign, failure, timeout );
    }

    public static RunResult noTestsRun()
    {
        return new RunResult( 0, 0, 0, 0 );
    }
}
