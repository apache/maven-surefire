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

    public static final RunResult No_Tests = new RunResult( -1, -1, -1, -1 );

    public static final RunResult Failure = new RunResult( -2, -2, -2, -2 );

    public static final int SUCCESS = 0;

    public static final int NO_TESTS = 254;

    public static final int FAILURE = 255;

    public RunResult( int completedCount, int errors, int failures, int skipped )
    {
        this.completedCount = completedCount;
        this.errors = errors;
        this.failures = failures;
        this.skipped = skipped;
    }

    public static RunResult totalCountOnly( int totalCount )
    {
        return new RunResult( totalCount, 0, 0, 0 );
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

    public int getBooterCode()
    {
        if ( this == No_Tests )
        {
            return NO_TESTS;
        }
        return getFailures() == 0 ? SUCCESS : FAILURE;
    }
}
