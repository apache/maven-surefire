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
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

/**
 * Test simple TestNG suite XML file
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 */
public class CheckTestNgSuiteXmlIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void suiteXml()
    {
        unpack().executeTest()
                .verifyErrorFree( 2 );
    }

    @Test
    public void suiteXmlForkModeAlways()
    {
        unpack().forkAlways()
                .executeTest()
                .verifyTextInLog( "Tests run: 2, Failures: 0, Errors: 0, Skipped: 0" );
    }

    @Test
    public void suiteXmlForkCountTwoReuse()
    {
        unpack().forkCount( 2 )
                .reuseForks( true )
                .executeTest()
                .verifyErrorFree( 2 );
    }

    private SurefireLauncher unpack()
    {
        return unpack( "testng-suite-xml" )
                       .sysProp( "testNgVersion", "5.7" )
                       .sysProp( "testNgClassifier", "jdk15" );
    }
}
