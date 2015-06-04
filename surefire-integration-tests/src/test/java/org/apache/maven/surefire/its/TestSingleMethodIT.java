package org.apache.maven.surefire.its;

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

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

/**
 * Test project using -Dtest=mtClass#myMethod
 *
 * @author Olivier Lamy
 */
public class TestSingleMethodIT
    extends SurefireJUnit4IntegrationTestCase
{
    private static final String RUNNING_WITH_PROVIDER47 = "parallel='none', perCoreThreadCount=true, threadCount=0";

    public OutputValidator singleMethod( String projectName, String... goals )
        throws Exception
    {
        SurefireLauncher launcher = unpack( projectName );
        for ( String goal : goals )
        {
            launcher.addGoal( goal );
        }
        return launcher.showErrorStackTraces().debugLogging()
            .executeTest()
            .verifyErrorFreeLog()
            .assertTestSuiteResults( 1, 0, 0, 0 );
    }

    @Test
    public void testJunit44()
        throws Exception
    {
        singleMethod( "junit44-single-method" );
    }

    @Test
    public void testJunit48Provider4()
        throws Exception
    {
        singleMethod( "junit48-single-method", "-P surefire-junit4" );
    }

    @Test
    public void testJunit48Provider47()
        throws Exception
    {
        singleMethod( "junit48-single-method", "-P surefire-junit47" )
            .verifyTextInLog( RUNNING_WITH_PROVIDER47 );
    }

    @Test
    public void testJunit48parallel()
        throws Exception
    {
        unpack( "junit48-single-method" )
            .parallel( "all" )
            .useUnlimitedThreads()
            .executeTest()
            .verifyErrorFreeLog()
            .assertTestSuiteResults( 1, 0, 0, 0 );
    }

    @Test
    public void testTestNg()
        throws Exception
    {
        singleMethod( "testng-single-method" );
    }

    @Test
    public void testTestNg5149()
        throws Exception
    {
        singleMethod( "/testng-single-method-5-14-9" );
    }

}
