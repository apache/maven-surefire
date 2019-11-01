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

import com.googlecode.junittoolbox.ParallelParameterized;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.maven.surefire.its.fixture.HelperAssertions.assumeJavaVersion;

@RunWith( ParallelParameterized.class )
public class JUnitPlatformIT
        extends SurefireJUnit4IntegrationTestCase
{
    private static final String XML_TESTSUITE_FRAGMENT =
            "<testsuite xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation="
                    + "\"https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report-3.0.xsd\" "
                    + "version=\"3.0\" name=\"&lt;&lt; ✨ &gt;&gt;\"";

    @Parameter
    public String junit5Version;

    @Parameter( 1 )
    public String jqwikVersion;

    @Parameters( name = "{0}" )
    public static Iterable<Object[]> artifactVersions()
    {
        ArrayList<Object[]> args = new ArrayList<>();
        args.add( new Object[] { "5.2.0", "0.8.0" } );
        args.add( new Object[] { "5.3.0", "0.8.15" } );
        args.add( new Object[] { "5.3.2", "0.9.0" } );
        args.add( new Object[] { "5.4.0", "0.9.3" } );
        args.add( new Object[] { "5.4.2", "1.0.0" } );
        args.add( new Object[] { "5.5.0", "1.1.0" } );
        args.add( new Object[] { "5.5.2", "1.1.0" } );
        args.add( new Object[] { "5.6.0-M1", "1.1.0" } );
        //args.add( new Object[] { "5.6.0-SNAPSHOT", "1.1.6" } );
        return args;
    }

    @Before
    public void setUp()
    {
        assumeJavaVersion( 1.8d );
    }

    @Test
    public void testJupiterEngine()
    {
        unpack( "junit-platform-engine-jupiter", "-" + junit5Version + "-" + jqwikVersion )
                .setTestToRun( "Basic*Test" )
                .sysProp( "junit5.version", junit5Version )
                .sysProp( "jqwik.version", jqwikVersion )
                .executeTest()
                .verifyErrorFree( 5 );
    }

    @Test
    public void testJupiterEngineWithDisplayNames()
    {
        OutputValidator validator = unpack( "junit-platform-engine-jupiter", "-" + junit5Version + "-" + jqwikVersion )
                .sysProp( "junit5.version", junit5Version )
                .sysProp( "jqwik.version", jqwikVersion )
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
        unpack( "junit-platform-engine-vintage", "-" + junit5Version + "-" + jqwikVersion )
                .sysProp( "junit5.version", junit5Version )
                .sysProp( "jqwik.version", jqwikVersion )
                .executeTest()
                .verifyErrorFree( 1 );
    }

    @Test
    public void testJQwikEngine()
    {
        unpack( "junit-platform-engine-jqwik", "-" + junit5Version + "-" + jqwikVersion )
                .sysProp( "junit5.version", junit5Version )
                .sysProp( "jqwik.version", jqwikVersion )
                .executeTest()
                .verifyErrorFree( 1 );
    }

    @Test
    public void testMultipleEngines()
    {
        unpack( "junit-platform-multiple-engines", "-" + junit5Version + "-" + jqwikVersion )
                .sysProp( "junit5.version", junit5Version )
                .sysProp( "jqwik.version", jqwikVersion )
                .executeTest()
                .verifyErrorFree( 7 );
    }

    @Test
    public void testTags()
    {
        unpack( "junit-platform-tags", "-" + junit5Version + "-" + jqwikVersion )
                .sysProp( "junit5.version", junit5Version )
                .executeTest()
                .verifyErrorFree( 2 );
    }
}
