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
import org.junit.Before;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.maven.surefire.its.fixture.HelperAssertions.assumeJavaVersion;
import static org.apache.maven.surefire.its.fixture.HelperAssertions.convertUnicodeToUTF8;

public class JUnitPlatformIT
        extends SurefireJUnit4IntegrationTestCase
{
    private static final String XML_TESTSUITE_FRAGMENT =
            "<testsuite xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation="
                    + "\"https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report-3.0.xsd\" "
                    + "version=\"3.0\" name=\"&lt;&lt; ✨ &gt;&gt;\"";

    @Before
    public void setUp()
    {
        assumeJavaVersion( 1.8d );
    }

    @Test
    public void testJupiterEngine()
    {
        unpack( "/junit-platform-engine-jupiter" )
                .setTestToRun( "Basic*Test" )
                .executeTest()
                .verifyErrorFree( 5 );
    }

    @Test
    public void testJupiterEngineWithDisplayNames()
    {
        OutputValidator validator = unpack( "/junit-platform-engine-jupiter" )
                .executeTest()
                .verifyErrorFree( 7 );

        validator.getSurefireReportsFile( "junitplatformenginejupiter.DisplayNameTest.txt", UTF_8 )
                 .assertContainsText( "<< ✨ >>" );

        validator.getSurefireReportsFile( "junitplatformenginejupiter.DisplayNameTest.txt", UTF_8 )
                .assertContainsText( "Test set: << ✨ >>" );

        validator.getSurefireReportsFile( "junitplatformenginejupiter.DisplayNameTest.txt", UTF_8 )
                .assertContainsText( " - in << ✨ >>" );


        validator.getSurefireReportsFile( "junitplatformenginejupiter.DisplayNameTest-output.txt", UTF_8 )
                .assertContainsText( "<< ✨ >>" );

        validator.getSurefireReportsFile( "junitplatformenginejupiter.DisplayNameTest-output.txt", UTF_8 )
                .assertContainsText( "73$71 ✔" );

        validator.getSurefireReportsFile( "junitplatformenginejupiter.DisplayNameTest-output.txt", UTF_8 )
                .assertContainsText( "73$72 ✔" );


        validator.getSurefireReportsFile( "TEST-junitplatformenginejupiter.DisplayNameTest.xml", UTF_8 )
                .assertContainsText( "testcase name=\"73$71 ✔\" classname=\"&lt;&lt; ✨ &gt;&gt;\"" )
                .assertContainsText( "testcase name=\"73$72 ✔\" classname=\"&lt;&lt; ✨ &gt;&gt;\"" )
                .assertContainsText( XML_TESTSUITE_FRAGMENT );
    }

    @Test
    public void testVintageEngine()
    {
        unpack( "/junit-platform-engine-vintage" )
                .executeTest()
                .verifyErrorFree( 1 );
    }

    @Test
    public void testJQwikEngine()
    {
        unpack( "/junit-platform-engine-jqwik" )
                .executeTest()
                .verifyErrorFree( 1 );
    }

    @Test
    public void testMultipleEngines()
    {
        unpack( "/junit-platform-multiple-engines" )
                .executeTest()
                .verifyErrorFree( 7 );
    }

    @Test
    public void testTags()
    {
        unpack( "/junit-platform-tags" )
                .executeTest()
                .verifyErrorFree( 2 );
    }
}
