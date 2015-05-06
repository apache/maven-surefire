package org.apache.maven.plugin.surefire.runorder;

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
import org.apache.maven.surefire.report.ReportEntry;

/**
 * @author Kristian Rosenvold
 */
public class RunEntryStatistics
{
    private final int runTime;

    private final int successfulBuilds;

    private final String testName;

    private RunEntryStatistics( int runTime, int successfulBuilds, String testName )
    {
        this.runTime = runTime;
        this.successfulBuilds = successfulBuilds;
        this.testName = testName;
    }

    public static RunEntryStatistics fromReportEntry( ReportEntry previous )
    {
        final Integer elapsed = previous.getElapsed();
        return new RunEntryStatistics( elapsed != null ? elapsed : 0, 0, previous.getName() );
    }

    public static RunEntryStatistics fromValues( int runTime, int successfulBuilds, Class clazz, String testName )
    {
        return new RunEntryStatistics( runTime, successfulBuilds, testName + "(" + clazz.getName() + ")" );
    }

    public RunEntryStatistics nextGeneration( int runTime )
    {
        return new RunEntryStatistics( runTime, this.successfulBuilds + 1, this.testName );
    }

    public RunEntryStatistics nextGenerationFailure( int runTime )
    {
        return new RunEntryStatistics( runTime, 0, this.testName );
    }

    public String getTestName()
    {
        return testName;
    }

    public int getRunTime()
    {
        return runTime;
    }

    public int getSuccessfulBuilds()
    {
        return successfulBuilds;
    }

    public static RunEntryStatistics fromString( String line )
    {
        StringTokenizer tok = new StringTokenizer( line, "," );
        int successfulBuilds = Integer.parseInt( tok.nextToken() );
        int runTime = Integer.parseInt( tok.nextToken() );
        String className = tok.nextToken();
        return new RunEntryStatistics( runTime, successfulBuilds, className );
    }

    @Override
    public String toString()
    {
        return successfulBuilds + "," + runTime + "," + testName;
    }

}
