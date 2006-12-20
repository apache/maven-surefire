package org.apache.maven.plugins.surefire.report;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class ReportTestSuiteTest
    extends TestCase
{
    private ReportTestSuite tSuite;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        tSuite = new ReportTestSuite();
    }

    public void testSetTestCases()
    {
        ReportTestCase tCase = new ReportTestCase();

        List tCaseList = new ArrayList();

        tCaseList.add( tCase );

        tSuite.setTestCases( tCaseList );

        assertEquals( tCase, tSuite.getTestCases().get( 0 ) );
    }

    public void testSetNumberdOfErrors()
    {
        tSuite.setNumberOfErrors( 9 );

        assertEquals( 9, tSuite.getNumberOfErrors() );
    }

    public void testSetNumberOfFailures()
    {
        tSuite.setNumberOfFailures( 10 );

        assertEquals( 10, tSuite.getNumberOfFailures() );
    }

    public void testSetNumberOfSkipped()
    {
        tSuite.setNumberOfSkipped( 5 );

        assertEquals( 5, tSuite.getNumberOfSkipped() );
    }
    
    public void testSetNumberOfTests()
    {
        tSuite.setNumberOfTests( 11 );

        assertEquals( 11, tSuite.getNumberOfTests() );
    }

    public void testSetName()
    {
        tSuite.setName( "Suite Name" );

        assertEquals( "Suite Name", tSuite.getName() );
    }

    public void testSetPackageName()
    {
        tSuite.setPackageName( "Suite Package Name" );

        assertEquals( "Suite Package Name", tSuite.getPackageName() );
    }

    public void testSetTimeElapsed()
    {
        tSuite.setTimeElapsed( .06f );

        assertEquals( .06f, tSuite.getTimeElapsed(), 0.0 );
    }
}
