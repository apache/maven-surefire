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

import java.util.Properties;

/**
 * Run-statistics for a testset
 *
 * @author Kristian Rosenvold
 *         Note; synchronization is questionable. Whiled this class alone is ok, there's a higher level concern about
 *         synchronization interactions within ReporterManager. See ReporterManager class.
 */
public class TestSetStatistics
{
    private static final String RESULTS_ERRORS = "errors";

    private static final String RESULTS_COMPLETED_COUNT = "completedCount";

    private static final String RESULTS_FAILURES = "failures";

    private static final String RESULTS_SKIPPED = "skipped";


    protected int completedCount;

    protected int errors;

    protected int failures;

    protected int skipped;

    public synchronized void incrementCompletedCount()
    {
        completedCount += 1;
    }

    public synchronized void incrementErrorsCount()
    {
        errors += 1;
    }

    public synchronized void incrementFailureCount()
    {
        failures += 1;
    }

    public synchronized void incrementSkippedCount()
    {
        skipped += 1;
    }

    public synchronized boolean isProblemFree()
    {
        return !hadFailures() && !hadErrors();
    }

    public synchronized boolean hadFailures()
    {
        return failures > 0;
    }

    public synchronized boolean hadErrors()
    {
        return errors > 0;
    }

    public synchronized int getCompletedCount()
    {
        return completedCount;
    }

    public int getSkipped()
    {
        return skipped;
    }

    public synchronized void initResultsFromProperties( Properties results )
    {
        errors = Integer.valueOf( results.getProperty( RESULTS_ERRORS, "0" ) ).intValue();
        skipped = Integer.valueOf( results.getProperty( RESULTS_SKIPPED, "0" ) ).intValue();
        failures = Integer.valueOf( results.getProperty( RESULTS_FAILURES, "0" ) ).intValue();
        completedCount = Integer.valueOf( results.getProperty( RESULTS_COMPLETED_COUNT, "0" ) ).intValue();
    }

    public synchronized void updateResultsProperties( Properties results )
    {
        results.setProperty( RESULTS_ERRORS, String.valueOf( errors ) );
        results.setProperty( RESULTS_COMPLETED_COUNT, String.valueOf( completedCount ) );
        results.setProperty( RESULTS_FAILURES, String.valueOf( failures ) );
        results.setProperty( RESULTS_SKIPPED, String.valueOf( skipped ) );
    }

    public synchronized String getSummary()
    {
        return "Tests run: " + completedCount + ", Failures: " + failures + ", Errors: " + errors + ", Skipped: " +
            skipped;
    }

}
