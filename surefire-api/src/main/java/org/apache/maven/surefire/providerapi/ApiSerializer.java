package org.apache.maven.surefire.providerapi;

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

import java.util.Properties;

public class ApiSerializer
{
    private static final String RESULTS_ERRORS = "errors";

    private static final String RESULTS_COMPLETED_COUNT = "completedCount";

    private static final String RESULTS_FAILURES = "failures";

    private static final String RESULTS_SKIPPED = "skipped";

    public RunResult fromProperties( Properties results )
    {
        int completedCount = Integer.valueOf( results.getProperty( RESULTS_COMPLETED_COUNT, "0" ) ).intValue();
        int errors = Integer.valueOf( results.getProperty( RESULTS_ERRORS, "0" ) ).intValue();
        int failures = Integer.valueOf( results.getProperty( RESULTS_FAILURES, "0" ) ).intValue();
        int skipped = Integer.valueOf( results.getProperty( RESULTS_SKIPPED, "0" ) ).intValue();
        return new RunResult( completedCount, errors, failures, skipped );
    }

    public void updateProperties( Properties results, RunResult runResult )
    {
        results.setProperty( RESULTS_ERRORS, String.valueOf( runResult.getErrors() ) );
        results.setProperty( RESULTS_COMPLETED_COUNT, String.valueOf( runResult.getCompletedCount() ) );
        results.setProperty( RESULTS_FAILURES, String.valueOf( runResult.getFailures() ) );
        results.setProperty( RESULTS_SKIPPED, String.valueOf( runResult.getSkipped() ) );
    }
}