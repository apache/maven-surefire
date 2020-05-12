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

import java.util.List;

import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.surefire.its.fixture.HelperAssertions;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Basic suite test using all known versions of TestNG. Used for regression testing Surefire against old versions. To
 * check new versions of TestNG work with current versions of Surefire, instead run the full test suite with
 * -Dtestng.version=5.14.2 (for example)
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * @author <a href="mailto:krosenvold@apache.org">Kristian Rosenvold</a>
 */
public class CheckTestNgVersionsIT
    extends SurefireJUnit4IntegrationTestCase
{

    @Test public void test47()
        throws Exception
    {
        runTestNgTest( "4.7", "jdk15" );
    }

    @Test
    @Ignore( "5.0 and 5.0.1 jars on central are malformed SUREFIRE-375 + MAVENUPLOAD-1024" )
    public void xXXtest50()
        throws Exception
    {
        runTestNgTest( "5.0", "jdk15" );
    }

    @Test
    @Ignore( "5.0 and 5.0.1 jars on central are malformed SUREFIRE-375 + MAVENUPLOAD-1024" )
    public void xXXtest501()
        throws Exception
    {
        runTestNgTest( "5.0.1", "jdk15" );
    }

    @Test public void test502()
        throws Exception
    {
        runTestNgTest( "5.0.2", "jdk15" );
    }

    @Test public void test51()
        throws Exception
    {
        runTestNgTest( "5.1", "jdk15" );
    }

    @Test public void test55()
        throws Exception
    {
        runTestNgTest( "5.5", "jdk15" );
    }

    @Test public void test56()
        throws Exception
    {
        runTestNgTest( "5.6", "jdk15" );
    }

    @Test public void test57()
        throws Exception
    {
        runTestNgTest( "5.7", "jdk15" );
    }

    @Test public void test58()
        throws Exception
    {
        runTestNgTest( "5.8", "jdk15" );
    }

    @Test public void test59()
        throws Exception
    {
        runTestNgTest( "5.9", "jdk15" );
    }

    @Test public void test510()
        throws Exception
    {
        runTestNgTest( "5.10", "jdk15" );
    }

    @Test public void test511()
        throws Exception
    {
        runTestNgTest( "5.11", "jdk15" );
    }

    @Test public void test512()
        throws Exception
    {
        runTestNgTest( "5.12.1" );
    }

    @Test public void test513()
        throws Exception
    {
        runTestNgTest( "5.13" );
    }

    @Test public void test5131()
        throws Exception
    {
        runTestNgTest( "5.13.1" );
    }

    @Test public void test514()
        throws Exception
    {
        runTestNgTest( "5.14" );
    }

    @Test public void test5141()
        throws Exception
    {
        runTestNgTest( "5.14.1" );
    }

    @Test public void test5142()
        throws Exception
    {
        runTestNgTest( "5.14.2" );
    }

    @Test public void test60()
        throws Exception
    {
        runTestNgTest( "6.0" );
    }

    @Test public void test685()
        throws Exception
    {
        runTestNgTestWithRunOrder( "6.8.5" );
    }

    private void runTestNgTestWithRunOrder( String version )
        throws Exception
    {
        runTestNgTest( version, null, true );
    }

    private void runTestNgTest( String version )
            throws Exception
    {
        runTestNgTest( version, null, false );
    }

    private void runTestNgTest( String version, boolean validateRunOrder )
            throws Exception
    {
        runTestNgTest( version, null, validateRunOrder );
    }

    private void runTestNgTest( String version, String classifier )
        throws Exception
    {
        runTestNgTest( version, classifier, false );
    }

    private void runTestNgTest( String version, String classifier, boolean validateRunOrder )
        throws Exception
    {
        final SurefireLauncher launcher = unpack( "testng-simple" )
                                            .sysProp( "testNgVersion", version );

        if ( classifier != null )
        {
            launcher.sysProp( "testNgClassifier", classifier );
        }

        final OutputValidator outputValidator = launcher.executeTest();

        outputValidator.assertTestSuiteResults( 3, 0, 0, 0 );

        if ( validateRunOrder )
        {
            // assert correct run order of tests
            List<ReportTestSuite> report = HelperAssertions.extractReports( outputValidator.getBaseDir() );

            assertEquals( 3, report.size() );

            assertTrue( "TestNGSuiteTestC was executed first",
                    getTestClass( report, 0 ).endsWith( "TestNGSuiteTestC" ) );

            assertTrue( "TestNGSuiteTestB was executed second",
                    getTestClass( report, 1 ).endsWith( "TestNGSuiteTestB" ) );

            assertTrue( "TestNGSuiteTestA was executed last",
                    getTestClass( report, 2 ).endsWith( "TestNGSuiteTestA" ) );
        }
    }

    private String getTestClass( List<ReportTestSuite> report, int i )
    {
        return report.get( i ).getFullClassName();
    }
}
