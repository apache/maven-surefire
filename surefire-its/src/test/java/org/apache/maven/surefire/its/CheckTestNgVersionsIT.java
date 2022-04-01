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

import static org.apache.maven.surefire.its.fixture.HelperAssertions.assumeJavaMaxVersion;
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
    {
        runTestNgTest( "4.7", "jdk15" );
    }

    @Test
    @Ignore( "5.0 and 5.0.1 jars on central are malformed SUREFIRE-375 + MAVENUPLOAD-1024" )
    public void test50()
    {
        runTestNgTest( "5.0", "jdk15" );
    }

    @Test
    @Ignore( "5.0 and 5.0.1 jars on central are malformed SUREFIRE-375 + MAVENUPLOAD-1024" )
    public void test501()
    {
        runTestNgTest( "5.0.1", "jdk15" );
    }

    @Test public void test502()
    {
        runTestNgTest( "5.0.2", "jdk15" );
    }

    @Test public void test51()
    {
        runTestNgTest( "5.1", "jdk15" );
    }

    @Test public void test55()
    {
        runTestNgTest( "5.5", "jdk15" );
    }

    @Test public void test56()
    {
        runTestNgTest( "5.6", "jdk15" );
    }

    @Test public void test57()
    {
        runTestNgTest( "5.7", "jdk15" );
    }

    @Test public void test58()
    {
        runTestNgTest( "5.8", "jdk15" );
    }

    @Test public void test59()
    {
        runTestNgTest( "5.9", "jdk15" );
    }

    @Test public void test510()
    {
        runTestNgTest( "5.10", "jdk15" );
    }

    @Test public void test511()
    {
        runTestNgTest( "5.11", "jdk15" );
    }

    @Test public void test512()
    {
        runTestNgTest( "5.12.1" );
    }

    @Test public void test513()
    {
        runTestNgTest( "5.13" );
    }

    @Test public void test5131()
    {
        runTestNgTest( "5.13.1" );
    }

    @Test public void test514()
    {
        runTestNgTest( "5.14" );
    }

    @Test public void test5141()
    {
        runTestNgTest( "5.14.1" );
    }

    @Test public void test5142()
    {
        runTestNgTest( "5.14.2" );
    }

    @Test public void test60()
    {
        runTestNgTest( "6.0" );
    }

    @Test public void test685()
    {
        runTestNgTestWithRunOrder( "6.8.5" );
    }

    private void runTestNgTestWithRunOrder( String version )
    {
        runTestNgTest( version, null, true );
    }

    private void runTestNgTest( String version )
    {
        runTestNgTest( version, null, false );
    }

    private void runTestNgTest( String version, String classifier )
    {
        runTestNgTest( version, classifier, false );
    }

    private void runTestNgTest( String version, String classifier, boolean validateRunOrder )
    {
        final SurefireLauncher launcher = unpack( "testng-simple" )
                                            .sysProp( "testNgVersion", version );

        if ( classifier != null )
        {
            launcher.sysProp( "testNgClassifier", classifier );
        }

        if ( version.startsWith( "5.12" ) || version.startsWith( "5.13" ) || version.startsWith( "5.14" ) )
        {
            // TestNG 5.12 - 5.14 uses Guava lib ang CGLib with reflective access.
            // WARNING: Illegal reflective access by com.google.inject.internal.cglib.core.ReflectUtils$2
            // (testng-5.12.1.jar) to method
            // java.lang.ClassLoader.defineClass(java.lang.String,byte[],int,int,java.security.ProtectionDomain)
            assumeJavaMaxVersion( 15 );
        }

        final OutputValidator outputValidator = launcher.executeTest();

        outputValidator.verifyErrorFreeLog().assertTestSuiteResults( 3, 0, 0, 0 );

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
