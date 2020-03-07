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
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.apache.maven.surefire.its.fixture.TestFile;
import org.junit.Test;

/**
 * Test Surefire-570 Multiple report directories
 *
 * @author Kristian Rosenvold
 */
public class Surefire570MultipleReportDirectoriesIT
    extends SurefireJUnit4IntegrationTestCase
{

    @Test
    public void testReportWithAggregate()
        throws Exception
    {

        SurefireLauncher surefireLauncher = unpack().failNever();
        surefireLauncher.executeTest();
        surefireLauncher.addGoal( "-Daggregate=true" );
        OutputValidator validator = surefireLauncher.executeSurefireReport( );
        TestFile siteFile = validator.getSiteFile( "surefire-report.html" );
        siteFile.assertContainsText( "MyModule1ClassTest" );
        siteFile.assertContainsText( "MyModule2ClassTest" );
        siteFile.assertContainsText( "MyDummyClassM1Test" );
    }

    @Test
    public void testReportWithoutAggregate()
        throws Exception
    {
        SurefireLauncher surefireLauncher = unpack().failNever();
        surefireLauncher.executeTest();
        surefireLauncher.reset();
        surefireLauncher.executeSurefireReport( );
        OutputValidator module1 = surefireLauncher.getSubProjectValidator( "module1" );
        TestFile siteFile = module1.getSiteFile( "surefire-report.html" );
        siteFile.assertContainsText( "MyModule1ClassTest" );
        siteFile.assertContainsText( "MyDummyClassM1Test" );
    }

    public SurefireLauncher unpack()
    {
        return unpack( "/surefire-570-multipleReportDirectories" );
    }

}
