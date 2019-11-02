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

import static org.apache.maven.shared.utils.xml.Xpp3DomBuilder.build;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.FileNotFoundException;

import org.apache.maven.shared.utils.xml.Xpp3Dom;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

/**
 * Test surefire-report on TestNG test
 *
 * @author <a href="mailto:michal.bocek@gmail.com">Michal Bocek</a>
 */
public class Surefire1135ImproveIgnoreMessageForTestNGIT extends SurefireJUnit4IntegrationTestCase
{

    private enum ResultType
    {
        SKIPPED( "skipped" ),
        FAILURE( "failure" );

        private final String type;

        ResultType( String type )
        {
            this.type = type;
        }

        public String getType()
        {
            return type;
        }
    }

    @Test
    public void testNgReport688() throws Exception
    {
        testNgReport( "6.8.8", null, ResultType.SKIPPED, "Skip test",
                /*"org.testng.SkipException"*/ null,
                /*"SkipExceptionReportTest.java:30"*/ null );
    }

    @Test
    public void testNgReport57() throws Exception
    {
        testNgReport( "5.7", "jdk15", ResultType.SKIPPED, "Skip test",
                /*"org.testng.SkipException"*/ null,
                /*"SkipExceptionReportTest.java:30"*/ null );
    }

    private void testNgReport( String version, String classifier, ResultType resultType, String message, String type,
                               String stackTrace ) throws Exception
    {
        OutputValidator outputValidator = runTest( version, classifier, resultType,
                "/surefire-1135-improve-ignore-message-for-testng" );

        Xpp3Dom[] children = readTests( outputValidator, "testng.SkipExceptionReportTest" );
        assertThat( "Report should contains only one test case", children.length, is( 1 ) );

        Xpp3Dom test = children[0];
        assertThat( "Not expected classname", test.getAttribute( "classname" ),
                is( "testng.SkipExceptionReportTest" ) );

        assertThat( "Not expected test name", test.getAttribute( "name" ), is( "testSkipException" ) );

        children = test.getChildren( resultType.getType() );
        assertThat( "Test should contains only one " + resultType.getType() + " element", children,
                is( arrayWithSize( 1 ) ) );

        Xpp3Dom result = children[0];
        if ( message == null )
        {
            assertThat( "Subelement message attribute must be null", result.getAttribute( "message" ),
                    is( nullValue() ) );
        }
        else
        {
            assertThat( "Subelement should contains message attribute", result.getAttribute( "message" ),
                    is( message ) );
        }

        if ( type == null )
        {
            assertThat( "Subelement type attribute must be null", result.getAttribute( "type" ), is( nullValue() ) );
        }
        else
        {
            assertThat( "Subelement should contains type attribute", result.getAttribute( "type" ), is( type ) );
        }

        if ( stackTrace == null )
        {
            assertThat( "Element body must be null", result.getValue(), isEmptyOrNullString() );
        }
        else
        {
            assertThat( "Element body must contains", result.getValue(), containsString( stackTrace ) );
        }
    }

    private OutputValidator runTest( String version, String classifier, ResultType resultType, String resource )
    {
        int skipped = ResultType.SKIPPED.equals( resultType ) ? 1 : 0;
        int failure = ResultType.FAILURE.equals( resultType ) ? 1 : 0;

        SurefireLauncher launcher = unpack( resource ).sysProp( "testNgVersion", version );

        if ( classifier != null )
        {
            launcher.sysProp( "testNgClassifier", classifier );
        }

        return launcher.addSurefireReportGoal().executeCurrentGoals().assertTestSuiteResults( 1, 0, failure, skipped );
    }

    private static Xpp3Dom[] readTests( OutputValidator validator, String className ) throws FileNotFoundException
    {
        Xpp3Dom testResult = build(
                validator.getSurefireReportsXmlFile( "TEST-" + className + ".xml" ).getFileInputStream(), "UTF-8" );
        return testResult.getChildren( "testcase" );
    }
}
