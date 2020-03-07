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
 * @author Kristian Rosenvold
 */
public final class RunStatistics
{
    private int completedCount;

    private int errors;

    private int failures;

    private int skipped;

    private int flakes;

    public synchronized int getCompletedCount()
    {
        return completedCount;
    }

    public synchronized int getSkipped()
    {
        return skipped;
    }

    public synchronized int getFailures()
    {
        return failures;
    }

    public synchronized int getErrors()
    {
        return errors;
    }

    public synchronized int getFlakes()
    {
        return flakes;
    }

    public synchronized void set( int completedCount, int errors, int failures, int skipped, int flakes )
    {
        this.completedCount = completedCount;
        this.errors = errors;
        this.failures = failures;
        this.skipped = skipped;
        this.flakes = flakes;
    }

    public synchronized RunResult getRunResult()
    {
        return new RunResult( completedCount, errors, failures, skipped, flakes );
    }

    public synchronized String getSummary()
    {
        String summary =
            "Tests run: " + completedCount + ", Failures: " + failures + ", Errors: " + errors + ", Skipped: "
                + skipped;
        if ( flakes > 0 )
        {
            summary += ", Flakes: " + flakes;
        }
        return summary;
    }
}
