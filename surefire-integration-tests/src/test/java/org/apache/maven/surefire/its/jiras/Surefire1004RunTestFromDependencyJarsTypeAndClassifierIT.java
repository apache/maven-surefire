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

import static org.junit.Assert.*;

import java.util.Collection;

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

public class Surefire1004RunTestFromDependencyJarsTypeAndClassifierIT
    extends SurefireJUnit4IntegrationTestCase
{
    
    @Test
    public void shouldScanAndRunTestsInDependencyJars()
        throws Exception
    {
        SurefireLauncher launcher = unpack( "surefire-1004-RunTestFromDependencyJarsTypeAndClassifier" );
        launcher.addGoal("test").addGoal("install");
        OutputValidator wholeExecValidator = launcher.executeCurrentGoals();
        wholeExecValidator.verifyErrorFreeLog();

        OutputValidator module1 = launcher.getSubProjectValidator("surefire-1004-module1");
        module1.assertTestSuiteResults(3, 0, 0, 0);

        // Tests from dependencies
        wholeExecValidator.verifyTextInLog( "Running org.acme.tests.TestA" );
        wholeExecValidator.verifyTextInLog( "Running org.acme.classifiedtests.ClassifiedTestA" );
        // Tests from module1 to verify classpath
        wholeExecValidator.verifyTextInLog( "Running org.acme.tests.ClasspathTest" );
        // Should not run these tests
        Collection<String> logLines = wholeExecValidator.loadLogLines();
        assertFalse( logLines.contains( "org.acme.othertests.OtherTestA" ) );
        assertFalse( logLines.contains( "org.acme.tests.TestB" ) );
    }
}
