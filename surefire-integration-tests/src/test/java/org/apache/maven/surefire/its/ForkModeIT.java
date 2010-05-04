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

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.reporting.MavenReportException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Test forkMode
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 */
public class ForkModeIT
    extends AbstractSurefireIntegrationTestClass
{
    public void testForkModeAlways()
        throws Exception
    {
        String[] pids = doTest( "always" );

        assertDifferentPids( pids );
    }

    public void testForkModePerTest()
        throws Exception
    {
        String[] pids = doTest( "pertest" );

        assertDifferentPids( pids );
    }

    public void testForkModeNever()
        throws Exception
    {
        String[] pids = doTest( "never" );

        assertSamePids( pids );
    }

    public void testForkModeNone()
        throws Exception
    {
        String[] pids = doTest( "none" );

        assertSamePids( pids );
    }

    public void testForkModeOnce()
        throws Exception
    {
        String[] pids = doTest( "once" );
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

    private String[] doTest( String forkMode )
        throws IOException, VerificationException, MavenReportException
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/fork-mode" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List goals = this.getInitialGoals();
        goals.add( "test" );
        goals.add( "-DforkMode=" + forkMode );
        verifier.executeGoals( goals );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        HelperAssertions.assertTestSuiteResults( 3, 0, 0, 0, testDir );

        File targetDir = new File( testDir, "target" );
        String[] pids = new String[3];
        for ( int i = 1; i <= pids.length; i++ )
        {
            File pidFile = new File( targetDir, "test" + i + "-pid" );
            String pid = slurpFile( pidFile );
            pids[i - 1] = pid;
        }
        return pids;
    }

    private String slurpFile( File textFile )
        throws IOException
    {
        StringBuffer sb = new StringBuffer();
        BufferedReader reader = new BufferedReader( new FileReader( textFile ) );
        for ( String line = reader.readLine(); line != null; line = reader.readLine() )
        {
            sb.append( line );
        }
        reader.close();
        return sb.toString();
    }
}
