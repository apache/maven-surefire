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
import org.apache.maven.surefire.its.fixture.SurefireIntegrationTestCase;

/**
 * Basic suite test using all known versions of TestNG. Used for regression testing Surefire against old versions.
 * To check new versions of TestNG work with current versions of Surefire, instead run the full test suite with
 * -Dtestng.version=5.14.2 (for example)
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * @author <a href="mailto:krosenvold@apache.org">Kristian Rosenvold</a>
 */
public class CheckTestNgVersionsIT
    extends SurefireIntegrationTestCase
{

    public void test47()
        throws Exception
    {
        runTestNgTest( "4.7" );
    }

    // DGF SUREFIRE-375 + MAVENUPLOAD-1024
    // The 5.0 and 5.0.1 jars on central are malformed

    public void XXXtest50()
        throws Exception
    {
        runTestNgTest( "5.0" );
    }

    public void XXXtest501()
        throws Exception
    {
        runTestNgTest( "5.0.1" );
    }

    public void test502()
        throws Exception
    {
        runTestNgTest( "5.0.2" );
    }

    public void test51()
        throws Exception
    {
        runTestNgTest( "5.1" );
    }

    public void test55()
        throws Exception
    {
        runTestNgTest( "5.5" );
    }

    public void test56()
        throws Exception
    {
        runTestNgTest( "5.6" );
    }

    public void test57()
        throws Exception
    {
        runTestNgTest( "5.7" );
    }

    public void test58()
        throws Exception
    {
        runTestNgTest( "5.8" );
    }

    public void test59()
        throws Exception
    {
        runTestNgTest( "5.9" );
    }

    public void test510()
        throws Exception
    {
        runTestNgTest( "5.10" );
    }

    public void test511()
        throws Exception
    {
        runTestNgTest( "5.11" );
    }

    public void test512()
        throws Exception
    {
        runTestNgTest( "5.12.1" );
    }

    public void test513()
        throws Exception
    {
        runTestNgTest( "5.13" );
    }

    public void test5131()
        throws Exception
    {
        runTestNgTest( "5.13.1" );
    }

    public void test514()
        throws Exception
    {
        runTestNgTest( "5.14" );
    }

    public void test5141()
        throws Exception
    {
        runTestNgTest( "5.14.1" );
    }

    public void test5142()
        throws Exception
    {
        runTestNgTest( "5.14.2" );
    }

    public void test60()
        throws Exception
    {
        runTestNgTest( "6.0" );
    }

    public void runTestNgTest( String version )
        throws Exception
    {

        final OutputValidator outputValidator = unpack( "testng-simple" ).resetInitialGoals( version ).executeTest();
        outputValidator.verifyErrorFreeLog().assertTestSuiteResults( 1, 0, 0, 0 );
    }
}
