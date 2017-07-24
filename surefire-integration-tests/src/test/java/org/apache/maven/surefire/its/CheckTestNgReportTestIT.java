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

import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * Test surefire-report on TestNG test
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 */
public class CheckTestNgReportTestIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void testNgReport()
        throws Exception
    {
        unpack( "/testng-simple" )
                .sysProp( "testNgVersion", "5.7" )
                .sysProp( "testNgClassifier", "jdk15" )
                .addSurefireReportGoal()
                .executeCurrentGoals()
                .verifyErrorFree( 3 )
                .getSiteFile( "surefire-report.html" )
                .assertFileExists();
    }

    @Test
    public void shouldNotBeVerbose()
        throws Exception
    {
        unpack( "/testng-simple" )
            .sysProp( "testNgVersion", "5.10" )
            .sysProp( "testNgClassifier", "jdk15" )
            .executeTest()
            .verifyErrorFreeLog()
            .assertThatLogLine( containsString( "[Parser] Running:" ), is( 0 ) );
    }

    @Test
    public void shouldBeVerbose()
        throws Exception
    {
        unpack( "/testng-simple" )
            .sysProp( "testNgVersion", "5.10" )
            .sysProp( "testNgClassifier", "jdk15" )
            .sysProp( "surefire.testng.verbose", "10" )
            .executeTest()
            .verifyErrorFreeLog()
            .assertThatLogLine( containsString( "[Parser] Running:" ), is( 1 ) );
    }
}
