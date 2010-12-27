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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains commonly used featurtes for most tests, encapsulating
 * common use cases
 *
 * @author Kristian Rosenvold
 */
public abstract class SurefireVerifierTestClass
    extends AbstractSurefireIntegrationTestClass
{
    private final File testDir;

    private final List<String> cliOptions = new ArrayList<String>();

    private final List<String> goals;

    private final Verifier verifier;


    protected SurefireVerifierTestClass( String testProject )
    {
        try
        {
            testDir = ResourceExtractor.simpleExtractResources( getClass(), testProject );
            this.goals = getInitialGoals();
            this.verifier = new Verifier( testDir.getAbsolutePath() );
        }
        catch ( VerificationException e )
        {
            throw new RuntimeException( e );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    protected void assertPresent( File file )
    {
        verifier.assertFilePresent( file.getAbsolutePath() );
    }

    protected void assertNotPresent( File file )
    {
        verifier.assertFileNotPresent( file.getAbsolutePath() );
    }

    protected void showErrorStackTraces()
    {
        cliOptions.add( "-e" );
    }

    protected void debugLogging()
    {
        cliOptions.add( "-X" );
    }


    protected void failNever()
    {
        cliOptions.add( "-fn" );
    }

    protected SurefireVerifierTestClass addGoal( String goal )
    {
        goals.add( goal );
        return this;
    }

    protected Verifier executeTest()
        throws VerificationException
    {
        return execute( "test" );
    }

    protected Verifier execute( String goal )
        throws VerificationException
    {
        addGoal( goal );
        verifier.setCliOptions( cliOptions );
        verifier.executeGoals( goals );
        verifier.resetStreams();
        return verifier;
    }

    protected File getSurefireReportsFile( String fileName )
    {
        File targetDir = new File( testDir, "target/surefire-reports" );
        return new File( targetDir, fileName );
    }

    protected void printSummary( boolean printsummary )
    {
        addGoal( "-DprintSummary=" + printsummary );
    }

    protected void redirectToFile( boolean redirect )
    {
        addGoal( "-Dredirect.to.file=" + redirect );
    }

    protected void forkOnce()
    {
        forkMode( "once" );
    }

    protected void forkNever()
    {
        forkMode( "never" );
    }

    protected void forkAlways()
    {
        forkMode( "always" );
    }

    protected void forkMode( String forkMode )
    {
        addGoal( "-DforkMode=" + forkMode );
    }

    public void assertTestSuiteResults( int total, int errors, int failures, int skipped )
        throws MavenReportException
    {
        HelperAssertions.assertTestSuiteResults( total, errors, failures, skipped, testDir );
    }

    public void verifyTextInLog( String text )
        throws VerificationException

    {
        verifier.verifyTextInLog( text );
    }
}
