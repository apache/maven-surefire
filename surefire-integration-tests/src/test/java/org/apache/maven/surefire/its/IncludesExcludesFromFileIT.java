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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireIntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;

/**
 * Test include/exclude from files.
 * <p/>
 * Based on {@link IncludesExcludesIT}.
 */
public class IncludesExcludesFromFileIT
    extends SurefireIntegrationTestCase
{
    private SurefireLauncher unpack()
    {
        return unpack( "/includes-excludes-from-file" );
    }

    public void testSimple()
    {
        testWithProfile( "-Psimple" );
    }

    public void testSimpleMixed()
    {
        testWithProfile( "-Psimple-mixed" );
    }

    public void testRegex()
    {
        testWithProfile( "-Pregex" );
    }

    public void testPath()
    {
        testWithProfile( "-Ppath" );
    }

    private void testWithProfile( String profile )
    {
        final OutputValidator outputValidator = unpack().
            addGoal( profile ).executeTest().verifyErrorFree( 2 );

        outputValidator.getTargetFile( "testTouchFile.txt" ).assertFileExists();
        outputValidator.getTargetFile( "defaultTestTouchFile.txt" ).assertFileExists();
    }

    public void testMissingExcludes()
    {
        expectBuildFailure( "-Pmissing-excludes-file", "Failed to load list from file", "no-such-excludes-file" );
    }

    public void testMissingIncludes()
    {
        expectBuildFailure( "-Pmissing-includes-file", "Failed to load list from file", "no-such-includes-file" );
    }

    private void expectBuildFailure( final String profile, final String... messages )
    {
        final OutputValidator outputValidator = unpack().
            addGoal( profile ).executeTestWithFailure();

        for ( String message : messages )
        {
            outputValidator.verifyTextInLog( message );
        }
    }
}
