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
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.apache.maven.surefire.its.fixture.SurefireIntegrationTestCase;
import org.apache.maven.surefire.its.fixture.TestFile;

/**
 * Test forkMode
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 */
public class ForkModeIT
    extends SurefireIntegrationTestCase
{
    public void testForkModeAlways()
    {
        String[] pids = doTest( unpack( getProject() ).forkAlways() );
        assertDifferentPids( pids );
    }

    public void testForkModePerTest()
    {
        String[] pids = doTest( unpack( getProject() ).forkPerTest() );
        assertDifferentPids( pids );
    }

    public void testForkModeNever()
    {
        String[] pids = doTest( unpack( getProject() ).forkNever() );
        assertSamePids( pids );
    }

    public void testForkModeNone()
    {
        String[] pids = doTest( unpack( getProject() ).forkMode( "none" ) );
        assertSamePids( pids );
    }

    public void testForkModeOnce()
    {
        String[] pids = doTest( unpack( getProject() ).forkOnce() );
        // DGF It would be nice to assert that "once" was different
        // from "never" ... but there's no way to check the PID of
        // Maven itself.  No matter, "once" is tested by setting
        // argLine, which can't be done except by forking.
        assertSamePids( pids );
    }

    private void assertSamePids( String[] pids )
    {
        assertEquals( "pid 1 didn't match pid 2", pids[0], pids[1] );
        assertEquals( "pid 1 didn't match pid 3", pids[0], pids[2] );
    }

    private void assertDifferentPids( String[] pids )
    {
        if ( pids[0].equals( pids[1] ) )
        {
            fail( "pid 1 matched pid 2: " + pids[0] );
        }

        if ( pids[0].equals( pids[2] ) )
        {
            fail( "pid 1 matched pid 3: " + pids[0] );
        }

        if ( pids[1].equals( pids[2] ) )
        {
            fail( "pid 2 matched pid 3: " + pids[0] );
        }
    }

    private String[] doTest( SurefireLauncher forkMode )
    {
        final OutputValidator outputValidator = forkMode.executeTest();
        outputValidator.verifyErrorFreeLog().assertTestSuiteResults( 3, 0, 0, 0 );
        String[] pids = new String[3];
        for ( int i = 1; i <= pids.length; i++ )
        {
            final TestFile targetFile = outputValidator.getTargetFile( "test" + i + "-pid" );
            String pid = targetFile.slurpFile();
            pids[i - 1] = pid;
        }
        return pids;
    }

    protected String getProject()
    {
        return "fork-mode";
    }

}
