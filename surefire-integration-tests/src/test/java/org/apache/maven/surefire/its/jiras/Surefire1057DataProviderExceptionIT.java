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

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.Test;

/**
 * Test surefire-report on TestNG test
 *
 * @author <a href="mailto:michal.bocek@gmail.com">Michal Bocek</a>
 */
public class Surefire1057DataProviderExceptionIT
    extends SurefireJUnit4IntegrationTestCase
{

    static final String NL = System.getProperty( "line.separator" );
    
    @Test
    public void testNgReport()
        throws Exception
    {
        final OutputValidator outputValidator = unpack( "/surefire-1057-dataprovider-exception" )
                        .addSurefireReportGoal()
                        .executeCurrentGoals()
                        .assertTestSuiteResults( 2, 0, 1, 1 );
        outputValidator.getSurefireReportsXmlFile( "TEST-testng.DataProviderExceptionReportTest.xml" )
                        .assertContainsText( "<skipped message=\"Skip tests message\" type=\"org.testng.SkipException\">org.testng.SkipException: Skip tests message");

        outputValidator.getSurefireReportsXmlFile( "TEST-testng.DataProviderExceptionReportTest.xml" )
                        .assertContainsText( "<failure message=\"&#10;Data Provider public java.lang.Object[] testng.DataProviderExceptionReportTest.dataProvider() must "
                                           + "return either Object[][] or Iterator&lt;Object&gt;[], not class [Ljava.lang.Object;\" type=\"org.testng.TestNGException\"><![CDATA[org.testng.TestNGException:" );
    }
}
