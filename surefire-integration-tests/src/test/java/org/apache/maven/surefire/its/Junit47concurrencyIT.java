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


import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.List;

/**
 * Basic suite test using all known versions of JUnit 4.x
 *
 * @author Kristian Rosenvold
 */
public class Junit47concurrencyIT
    extends AbstractSurefireIntegrationTestClass
{


    public void test47()
        throws Exception
    {
        runJUnitTest();
    }


    public void runJUnitTest()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/concurrentjunit47" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List arguments = this.getInitialGoals();
        arguments.add( "test" );
        // DGF we have to pass in the version as a command line argument
        // and NOT as a system property; otherwise our setting will be ignored
        arguments.add( "-DjunitVersion=4.7" );
        verifier.executeGoals( arguments );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        IntegrationTestSuiteResults suite = HelperAssertions.parseTestResults( new File[]{ testDir } );
        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, suite );
    }
}