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

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

/**
 * JUnit4 RunListener Integration Test.
 *
 * @author <a href="mailto:matthew.gilliard@gmail.com">Matthew Gilliard</a>
 */
public class JUnit4RunListenerIT
    extends SurefireJUnit4IntegrationTestCase
{
    private SurefireLauncher unpack()
    {
        return unpack( "/junit4-runlistener" );
    }

    @Test
    public void testJUnit4RunListener()
        throws Exception
    {
        final OutputValidator outputValidator = unpack().addGoal( "-Dprovider=surefire-junit4" ).setJUnitVersion(
            "4.4" ).executeTest().verifyErrorFreeLog();
        assertResults( outputValidator );
        outputValidator.verifyTextInLog( "testRunStarted null" );
        outputValidator.verifyTextInLog( "testFinished simpleTest" );
        outputValidator.verifyTextInLog( "testRunFinished org.junit.runner.Result" );
    }

    @Test
    public void testRunlistenerJunitCoreProvider()
        throws Exception
    {
        final OutputValidator outputValidator =
            unpack().addGoal( "-Dprovider=surefire-junit47" ).setJUnitVersion( "4.8.1" ).addGoal(
                "-DjunitVersion=4.8.1" ).executeTest().verifyErrorFreeLog();   // Todo: Fix junitVesion
        assertResults( outputValidator );
        outputValidator.verifyTextInLog( "testRunStarted null" );
        outputValidator.verifyTextInLog( "testFinished simpleTest" );
        outputValidator.verifyTextInLog( "testRunFinished org.junit.runner.Result" );
    }

    private void assertResults( OutputValidator outputValidator )
    {
        outputValidator.assertTestSuiteResults( 1, 0, 0, 0 );
        outputValidator.getTargetFile( "runlistener-output-1.txt" ).assertFileExists();
        outputValidator.getTargetFile( "runlistener-output-2.txt" ).assertFileExists();
    }


}
