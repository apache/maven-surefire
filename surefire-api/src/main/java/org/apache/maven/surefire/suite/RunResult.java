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

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

/**
 * Represents a test-run-result; this may be from a single test run or an aggregated result.
 * <br>
 * In the case of timeout==true, the run-counts reflect the state of the test-run at the time
 * of the timeout.
 *
 * @author Kristian Rosenvold
 */
public class RunResult
{
    private final int completedCount;

    private final int errors;

    private final int failures;

    private final int skipped;

    private final int flakes;

    private final String failure;

    private final boolean timeout;

    public static final int SUCCESS = 0;

    private static final int FAILURE = 255;

    private static final int NO_TESTS = 254;

    public static RunResult timeout( RunResult accumulatedAtTimeout )
    {
        return errorCode( accumulatedAtTimeout, accumulatedAtTimeout.getFailure(), true );
    }

    public static RunResult failure( RunResult accumulatedAtTimeout, Exception cause )
    {
        return errorCode( accumulatedAtTimeout, getStackTrace( cause ), accumulatedAtTimeout.isTimeout() );
    }

    private static RunResult errorCode( RunResult other, String failure, boolean timeout )
    {
        return new RunResult( other.getCompletedCount(), other.getErrors(), other.getFailures(), other.getSkipped(),
                                    failure, timeout );
    }

    public RunResult( int completedCount, int errors, int failures, int skipped )
    {
        this( completedCount, errors, failures, skipped, null, false );
    }

    public RunResult( int completedCount, int errors, int failures, int skipped, int flakes )
    {
        this( completedCount, errors, failures, skipped, flakes, null, false );
    }

    public RunResult( int completedCount, int errors, int failures, int skipped, String failure, boolean timeout )
    {
        this( completedCount, errors, failures, skipped, 0, failure, timeout );
    }

    public RunResult( int completedCount, int errors, int failures, int skipped, int flakes, String failure,
                      boolean timeout )
    {
        this.completedCount = completedCount;
        this.errors = errors;
        this.failures = failures;
        this.skipped = skipped;
        this.failure = failure;
        this.timeout = timeout;
        this.flakes = flakes;
    }

    private static String getStackTrace( Exception e )
    {
        if ( e == null )
        {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try ( PrintWriter pw = new PrintWriter( out ) )
        {
            e.printStackTrace( pw );
        }
        return new String( out.toByteArray() );
    }

    public int getCompletedCount()
    {
        return completedCount;
    }

    public int getErrors()
    {
        return errors;
    }

    public int getFlakes()
    {
        return flakes;
    }

    public int getFailures()
    {
        return failures;
    }

    public int getSkipped()
    {
        return skipped;
    }

    public Integer getFailsafeCode()  // Only used for compatibility reasons.
    {
        if ( completedCount == 0 )
        {
            return NO_TESTS;
        }
        if ( !isErrorFree() )
        {
            return FAILURE;
        }
        return null;
    }

    /* Indicates if the tests are error free */
    public boolean isErrorFree()
    {
        return getFailures() == 0 && getErrors() == 0 && !isFailure();
    }

    public boolean isInternalError()
    {
        return getFailures() == 0 && getErrors() == 0 && isFailure();
    }

    /* Indicates test timeout or technical failure */
    public boolean isFailureOrTimeout()
    {
        return isTimeout() || isFailure();
    }

    public boolean isFailure()
    {
        return failure != null;
    }

    public String getFailure()
    {
        return failure;
    }

    public boolean isTimeout()
    {
        return timeout;
    }

    public RunResult aggregate( RunResult other )
    {
        String failureMessage = getFailure() != null ? getFailure() : other.getFailure();
        boolean timeout = isTimeout() || other.isTimeout();
        int completed = getCompletedCount() + other.getCompletedCount();
        int fail = getFailures() + other.getFailures();
        int ign = getSkipped() + other.getSkipped();
        int err = getErrors() + other.getErrors();
        int flakes = getFlakes() + other.getFlakes();
        return new RunResult( completed, err, fail, ign, flakes, failureMessage, timeout );
    }

    public static RunResult noTestsRun()
    {
        return new RunResult( 0, 0, 0, 0 );
    }

    @Override
    @SuppressWarnings( "RedundantIfStatement" )
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

        RunResult runResult = (RunResult) o;

        if ( completedCount != runResult.completedCount )
        {
            return false;
        }
        if ( errors != runResult.errors )
        {
            return false;
        }
        if ( failures != runResult.failures )
        {
            return false;
        }
        if ( skipped != runResult.skipped )
        {
            return false;
        }
        if ( timeout != runResult.timeout )
        {
            return false;
        }
        if ( failure != null ? !failure.equals( runResult.failure ) : runResult.failure != null )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = completedCount;
        result = 31 * result + errors;
        result = 31 * result + failures;
        result = 31 * result + skipped;
        result = 31 * result + ( failure != null ? failure.hashCode() : 0 );
        result = 31 * result + ( timeout ? 1 : 0 );
        return result;
    }
}
