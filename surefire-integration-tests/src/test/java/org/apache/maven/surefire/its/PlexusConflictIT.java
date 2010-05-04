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
import java.util.ArrayList;

/**
 * Test library using a conflicting version of plexus-utils
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 */
public class PlexusConflictIT
    extends AbstractSurefireIntegrationTestClass
{
    public void testPlexusConflict()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/plexus-conflict" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        this.executeGoal( verifier, "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, testDir );
    }

    public void testPlexusConflictIsolatedClassLoader()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/plexus-conflict" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        ArrayList goals = this.getInitialGoals();
        goals.add( "test" );
        goals.add( "-Dsurefire.useSystemClassLoader=false" );
        verifier.executeGoals( goals );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, testDir );
    }
}