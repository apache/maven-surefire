package org.apache.maven.surefire.its.jiras;

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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 *
 */
@RunWith( Parameterized.class )
@SuppressWarnings( "checkstyle:magicnumber" )
public class Surefire1914XmlReportingParameterizedTestIT extends SurefireJUnit4IntegrationTestCase
{
    @Parameter
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public String platform;

    @Parameter( 1 )
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public String jupiter;

    @Parameter( 2 )
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public String opentest;

    @Parameter( 3 )
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public String apiguardian;

    @Parameters( name = "{0}" )
    public static Iterable<Object[]> artifactVersions()
    {
        List<Object[]> args = new ArrayList<>();
        args.add( new Object[] {"1.0.3", "5.0.3", "1.0.0", "1.0.0"} );
        args.add( new Object[] {"1.1.1", "5.1.1", "1.0.0", "1.0.0"} );
        args.add( new Object[] {"1.2.0", "5.2.0", "1.1.0", "1.0.0"} );
        args.add( new Object[] {"1.3.2", "5.3.2", "1.1.1", "1.0.0"} );
        args.add( new Object[] {"1.4.2", "5.4.2", "1.1.1", "1.0.0"} );
        args.add( new Object[] {"1.5.2", "5.5.2", "1.2.0", "1.1.0"} );
        args.add( new Object[] {"1.6.2", "5.6.2", "1.2.0", "1.1.0"} );
        args.add( new Object[] {"1.7.1", "5.7.1", "1.2.0", "1.1.0" } );
        return args;
    }

    @Test
    public void testXmlReport()
    {
        OutputValidator validator = unpack( "surefire-1914-xml-reporting-parameterizedtest", "-" + jupiter )
                .sysProp( "junit5.version", jupiter )
                .executeTest()
                .verifyErrorFree( 16 );

        validator.getSurefireReportsFile( "TEST-jira1914.ParameterizedDisplayNameTest.xml", UTF_8 )
                .assertContainsText( "testcase name=\"theDisplayNameOfTestMethod1[1] a\" "
                        + "classname=\"theDisplayNameOfTheClass\"" )
                .assertContainsText( "testcase name=\"theDisplayNameOfTestMethod1[2] b\" "
                        + "classname=\"theDisplayNameOfTheClass\"" )
                .assertContainsText( "testcase name=\"theDisplayNameOfTestMethod1[3] c\" "
                        + "classname=\"theDisplayNameOfTheClass\"" )
                .assertContainsText( "testcase name=\"theDisplayNameOfTestMethod2 with param a\" "
                        + "classname=\"theDisplayNameOfTheClass\"" )
                .assertContainsText( "testcase name=\"theDisplayNameOfTestMethod2 with param b\" "
                        + "classname=\"theDisplayNameOfTheClass\"" )
                .assertContainsText( "testcase name=\"theDisplayNameOfTestMethod2 with param c\" "
                        + "classname=\"theDisplayNameOfTheClass\"" );

        validator.getSurefireReportsFile( "TEST-jira1914.ParameterizedJupiterTest.xml", UTF_8 )
                .assertContainsText( "<testcase name=\"add(int, int, int) 0 + 1 = 1\" "
                        + "classname=\"jira1914.ParameterizedJupiterTest\"" )
                .assertContainsText( "<testcase name=\"add(int, int, int) 1 + 2 = 3\" "
                        + "classname=\"jira1914.ParameterizedJupiterTest\"" )
                .assertContainsText( "<testcase name=\"add(int, int, int) 49 + 51 = 100\" "
                        + "classname=\"jira1914.ParameterizedJupiterTest\"" )
                .assertContainsText( "<testcase name=\"add(int, int, int) 1 + 100 = 101\" "
                        + "classname=\"jira1914.ParameterizedJupiterTest\"" )
                .assertContainsText( "<testcase name=\"square(int, int)[1] 1, 1\" "
                        + "classname=\"jira1914.ParameterizedJupiterTest\"" )
                .assertContainsText( "<testcase name=\"square(int, int)[2] 2, 4\" "
                        + "classname=\"jira1914.ParameterizedJupiterTest\"" )
                .assertContainsText( "<testcase name=\"square(int, int)[3] 3, 9\" "
                        + "classname=\"jira1914.ParameterizedJupiterTest\"" )
                .assertContainsText( "<testcase name=\"cube(int, int)[1] 1, 1\" "
                        + "classname=\"jira1914.ParameterizedJupiterTest\"" )
                .assertContainsText( "<testcase name=\"cube(int, int)[2] 2, 8\" "
                        + "classname=\"jira1914.ParameterizedJupiterTest\"" )
                .assertContainsText( "<testcase name=\"cube(int, int)[3] 3, 27\" "
                        + "classname=\"jira1914.ParameterizedJupiterTest\"" );
    }

}
