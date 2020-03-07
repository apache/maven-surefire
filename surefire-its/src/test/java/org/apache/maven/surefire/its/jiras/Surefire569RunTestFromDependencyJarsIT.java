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
import org.junit.Test;

/**
 * SUREFIRE-569 Add support for scanning Dependencies for TestClasses
 *
 * @author Aslak Knutsen
 */
public class Surefire569RunTestFromDependencyJarsIT extends SurefireJUnit4IntegrationTestCase
{

    @Test
    public void shouldScanAndRunTestsInDependencyJars() throws Exception
    {
        SurefireLauncher launcher = unpack( "surefire-569-RunTestFromDependencyJars" );
        launcher.addGoal( "test" ).addGoal( "install" );
        launcher.executeCurrentGoals();

        OutputValidator module1 = launcher.getSubProjectValidator( "module1" );
        module1.assertTestSuiteResults( 1, 0, 0, 0 );
    }
}
