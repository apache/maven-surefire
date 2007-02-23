package org.apache.maven.surefire;

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

import org.apache.maven.surefire.report.AbstractReporter;
import org.apache.maven.surefire.report.ReportEntry;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class TestReport
    extends AbstractReporter
{
    public TestReport()
    {
        super( Boolean.TRUE );
    }

    public void writeMessage( String message )
    {
        System.out.println( "TestReport::writeMessage -> " + message );
    }

    public void runStarting( int testCount )
    {
        System.out.println( "TestReport::runStarting -> " + testCount );
    }

    public void testStarting( ReportEntry reportEntry )
    {
        System.out.println( "TestReport::testStarting -> " + reportEntry.getMessage() );
    }

    public void testSucceeded( ReportEntry reportEntry )
    {
        System.out.println( "TestReport::testSucceeded -> " + reportEntry.getMessage() );
    }

    public void testError( ReportEntry reportEntry )
    {
        System.out.println( "TestReport::testError -> " + reportEntry.getMessage() );
    }

    public void testFailed( ReportEntry reportEntry )
    {
        System.out.println( "TestReport::testFailed -> " + reportEntry.getMessage() );
    }

    public void testSetStarting( ReportEntry reportEntry )
    {
        System.out.println( "TestReport::suiteStarting -> " + reportEntry.getMessage() );
    }

    public void testSetCompleted( ReportEntry reportEntry )
    {
        System.out.println( "TestReport::suiteCompleted -> " + reportEntry.getMessage() );
    }

    public void testSetAborted( ReportEntry reportEntry )
    {
        System.out.println( "TestReport::suiteAborted -> " + reportEntry.getMessage() );
    }

    public void infoProvided( ReportEntry reportEntry )
    {
        System.out.println( "TestReport::infoProvided -> " + reportEntry.getMessage() );
    }

    public void runStopped()
    {
        System.out.println( "TestReport::runStopped" );
    }

    public void runAborted( ReportEntry reportEntry )
    {
        System.out.println( "TestReport::runAborted -> " + reportEntry.getMessage() );
    }

    public void runCompleted()
    {
        System.out.println( "TestReport::runCompleted" );
    }

    public void reset()
    {
        System.out.println( "TestReport::dispose" );
    }

    public int getNumErrors()
    {
        System.out.println( "TestReport::getNumErrors" );
        return 0;
    }

    public int getNumFailures()
    {
        System.out.println( "TestReport::getNumFailures" );
        return 0;
    }

    public int getNumTests()
    {
        System.out.println( "TestReport::getNumTests" );
        return 0;
    }
}
