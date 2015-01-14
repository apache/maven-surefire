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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.FileNotFoundException;

import org.apache.maven.shared.utils.xml.Xpp3Dom;
import org.apache.maven.shared.utils.xml.Xpp3DomBuilder;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.Test;

/**
 * Test surefire-report on TestNG test
 *
 * @author <a href="mailto:michal.bocek@gmail.com">Michal Bocek</a>
 */
public class Surefire1135ImproveIgnoreMessageForTestNGIT
    extends SurefireJUnit4IntegrationTestCase
{
    
    private enum ResultType
    {
        SKIPPED( "skipped" ), FAILURE( "failure" );

        private String type;
        
        private ResultType(String type)
        {
            this.type = type;
        }
        
        public String getType() {
            return type;
        }
    }

    @Test
    public void testNgReport688() throws Exception {
        testNgReport( "6.8.8", ResultType.SKIPPED,
                                   "Skip test",
                                   "org.testng.SkipException",
                                   "SkipExceptionReportTest.java:30" );
    }

    @Test
    public void testNgReport57() throws Exception {
        testNgReport( "5.7", ResultType.SKIPPED,
                                   "Skip test",
                                   "org.testng.SkipException",
                                   "SkipExceptionReportTest.java:30" );
    }
    
    private void testNgReport( String version, ResultType resultType, String message, String type, String stackTrace )
        throws Exception
    {
        final OutputValidator outputValidator = runTest( version, resultType, "/surefire-1135-improve-ignore-message-for-testng" );
        
        Xpp3Dom[] children = readTests( outputValidator, "testng.SkipExceptionReportTest" );
        assertThat( "Report should contains only one test case", children.length, is( equalTo( 1 ) ));
        
        Xpp3Dom test = children[0];
        assertThat( "Not expected classname", test.getAttribute( "classname" ),
                    is( equalTo( "testng.SkipExceptionReportTest" ) ) );
        assertThat( "Not expected test name", test.getAttribute( "name" ),
                    is( equalTo( "testSkipException" ) ) );

        children = test.getChildren( resultType.getType() );
        assertThat( "Test should contains only one " + resultType.getType() + " element", children.length,
                    is( equalTo( 1 ) ) );

        Xpp3Dom result = children[0];
        if ( message == null )
        {
            assertThat( "Subelement message attribute must be null", result.getAttribute( "message" ), is( nullValue() ) );
        }
        else
        {
            assertThat( "Subelement should contains message attribute", result.getAttribute( "message" ),
                        is( equalTo( message ) ) );
        }

        if ( type == null )
        {
            assertThat( "Subelement type attribute must be null", result.getAttribute( "type" ), is( nullValue() ) );
        } else {
            assertThat( "Subelement should contains type attribute", result.getAttribute( "type" ),
                        is( equalTo( type ) ) );
        }
        
        if ( stackTrace == null )
        {
            assertThat( "Element body must be null", result.getValue() , is( nullValue() ) );
        }
        else
        {
            assertThat( "Element body must contains", result.getValue(),
                        is( containsString( stackTrace ) ) );
        }
    }

    private OutputValidator runTest( String version, ResultType resultType, String resource )
    {
        int skipped = ResultType.SKIPPED.equals( resultType ) ? 1 : 0;
        int failure = ResultType.FAILURE.equals( resultType ) ? 1 : 0;
        
        final OutputValidator outputValidator = unpack( resource )
                        .resetInitialGoals( version )
                        .addSurefireReportGoal()
                        .executeCurrentGoals()
                        .assertTestSuiteResults( 1, 0, failure, skipped );
        return outputValidator;
    }
    
    private Xpp3Dom[] readTests( OutputValidator validator, String className )
        throws FileNotFoundException
    {
        Xpp3Dom testResult =
            Xpp3DomBuilder.build( validator.getSurefireReportsXmlFile( "TEST-" + className + ".xml" ).getFileInputStream(),
                                  "UTF-8" );
        Xpp3Dom[] children = testResult.getChildren( "testcase" );
        return children;
    }
    
}