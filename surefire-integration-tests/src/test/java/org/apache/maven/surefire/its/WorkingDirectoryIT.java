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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Test working directory configuration, SUREFIRE-416
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 */
public class WorkingDirectoryIT
    extends AbstractSurefireIntegrationTestClass
{

    private File testDir;

    private File childTestDir;

    private File targetDir;

    private File outFile;

    public void setUp()
        throws IOException
    {
        testDir = ResourceExtractor.simpleExtractResources( getClass(), "/working-directory" );
        childTestDir = new File( testDir, "child" );
        targetDir = new File( childTestDir, "target" );
        outFile = new File( targetDir, "out.txt" );
        outFile.delete();
    }

    public void testWorkingDirectory()
        throws Exception
    {
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        this.executeGoal( verifier, "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, childTestDir );
        verifyOutputDirectory( childTestDir );
    }

    public void verifyOutputDirectory( File childTestDir )
        throws IOException
    {
        assertTrue( "out.txt doesn't exist: " + outFile.getAbsolutePath(), outFile.exists() );
        Properties p = new Properties();
        FileInputStream is = new FileInputStream( outFile );
        p.load( is );
        is.close();
        String userDirPath = p.getProperty( "user.dir" );
        assertNotNull( "user.dir was null in property file", userDirPath );
        File userDir = new File( userDirPath );
        assertEquals( "wrong user.dir", childTestDir.getAbsolutePath(), userDir.getAbsolutePath() );
    }

    public void testWorkingDirectoryNoFork()
        throws Exception
    {
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        ArrayList goals = this.getInitialGoals();
        goals.add( "test" );
        goals.add( "-DforkMode=never" );
        verifier.executeGoals( goals );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, childTestDir );
        verifyOutputDirectory( childTestDir );
    }

    public void testWorkingDirectoryChildOnly()
        throws Exception
    {
        Verifier verifier = new Verifier( childTestDir.getAbsolutePath() );
        this.executeGoal( verifier, "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, childTestDir );
        verifyOutputDirectory( childTestDir );
    }

    public void testWorkingDirectoryChildOnlyNoFork()
        throws Exception
    {

        Verifier verifier = new Verifier( childTestDir.getAbsolutePath() );
        ArrayList goals = this.getInitialGoals();
        goals.add( "test" );
        goals.add( "-DforkMode=never" );
        verifier.executeGoals( goals );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, childTestDir );
        verifyOutputDirectory( childTestDir );
    }
}
