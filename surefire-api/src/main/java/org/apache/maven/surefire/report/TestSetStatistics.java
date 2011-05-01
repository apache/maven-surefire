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

import org.apache.maven.surefire.suite.RunResult;

/**
 * Run-statistics for a testset
 *
 * @author Kristian Rosenvold
 *         Note; synchronization is questionable. Whiled this class alone is ok, there's a higher level concern about
 *         synchronization interactions within ReporterManager. See ReporterManager class.
 */
public class TestSetStatistics
{
    private int completedCount;

    private int errors;

    private int failures;

    private int skipped;

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

    public synchronized boolean hadFailures()
    {
        return failures > 0;
    }

    public synchronized void reset()
    {
        completedCount = 0;
        errors = 0;
        failures = 0;
        skipped = 0;
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

    public synchronized void add( TestSetStatistics testSetStatistics )
    {
        this.completedCount += testSetStatistics.completedCount;
        this.errors += testSetStatistics.errors;
        this.failures += testSetStatistics.failures;
        this.skipped += testSetStatistics.skipped;
    }

    public synchronized RunResult getRunResult()
    {
        return new RunResult( completedCount, errors, failures, skipped );
    }

    public synchronized String getSummary()
    {
        return "Tests run: " + completedCount + ", Failures: " + failures + ", Errors: " + errors + ", Skipped: "
            + skipped;
    }

}
