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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.surefire.its.misc.HelperAssertions;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Contains commonly used featurtes for most tests, encapsulating
 * common use cases.
 * <p/>
 * Also includes thread-safe access to the extracted resource
 * files, which AbstractSurefireIntegrationTestClass does not.
 * Thread safe only for running in "classes" mode.
 *
 * @author Kristian Rosenvold
 */
public abstract class SurefireVerifierTestClass
    extends TestCase
{
    private final File testDir;

    private final List<String> cliOptions = new ArrayList<String>();

    private final List<String> goals;

    private final Verifier verifier;

    private final String testNgVersion = System.getProperty( "testng.version" );

    private final String surefireVersion = System.getProperty( "surefire.version" );

    protected SurefireVerifierTestClass( String testProject )
    {
        try
        {
            testDir = simpleExtractResources( getClass(), testProject );
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

    private File simpleExtractResources( Class<?> cl, String resourcePath )
        throws IOException
    {
        String tempDirPath = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File tempDir = new File( tempDirPath, this.getClass().getSimpleName() );
        File testDir = new File( tempDir, resourcePath );
        FileUtils.deleteDirectory( testDir );

        return ResourceExtractor.extractResourcePath( cl, resourcePath, tempDir, true );
    }

    protected void reset()
    {
        goals.clear();
        goals.addAll( getInitialGoals() );
        cliOptions.clear();
    }

    private List<String> getInitialGoals()
    {
        List<String> goals1 = new ArrayList<String>();
        goals1.add( "-Dsurefire.version=" + surefireVersion );

        if ( testNgVersion != null )
        {
            goals1.add( "-DtestNgVersion=" + testNgVersion );

            ArtifactVersion v = new DefaultArtifactVersion( testNgVersion );
            try
            {
                if ( VersionRange.createFromVersionSpec( "(,5.12.1)" ).containsVersion( v ) )
                {
                    goals1.add( "-DtestNgClassifier=jdk15" );
                }
            }
            catch ( InvalidVersionSpecificationException e )
            {
                throw new RuntimeException( e.getMessage(), e );
            }
        }

        return goals1;
    }

    protected List<String> getInitialGoals( String testNgVersion )
    {
        List<String> goals = new ArrayList<String>();
        goals.add( "-Dsurefire.version=" + surefireVersion );

        if ( testNgVersion != null )
        {
            goals.add( "-DtestNgVersion=" + testNgVersion );

            ArtifactVersion v = new DefaultArtifactVersion( testNgVersion );
            try
            {
                if ( VersionRange.createFromVersionSpec( "(,5.12.1)" ).containsVersion( v ) )
                {
                    goals.add( "-DtestNgClassifier=jdk15" );
                }
            }
            catch ( InvalidVersionSpecificationException e )
            {
                throw new RuntimeException( e.getMessage(), e );
            }
        }

        return goals;
    }

    /**
     * Returns a file, referenced from the extracted root (where pom.xml is located)
     *
     * @param path The subdirectory under basedir
     * @return A file
     */
    protected File getSubFile( String path )
    {
        return new File( testDir, path );
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

    protected Verifier executeVerify()
        throws VerificationException
    {
        return execute( "verify" );
    }

    protected Verifier execute( String goal )
        throws VerificationException
    {
        addGoal( goal );
        verifier.setCliOptions( cliOptions );
        try
        {
            verifier.executeGoals( goals );
            return verifier;
        }
        finally
        {
            verifier.resetStreams();
        }
    }

    protected File getSurefireReportsFile( String fileName )
    {
        File targetDir = getSubFile( "target/surefire-reports" );
        return new File( targetDir, fileName );
    }

    protected File getSiteFile( String fileName )
    {
        File targetDir = getSubFile( "target/site" );
        return new File( targetDir, fileName );
    }

    protected File getTargetFile( String fileName )
    {
        File targetDir = getSubFile( "target" );
        return new File( targetDir, fileName );
    }

    protected File getSiteFile( String moduleName, String fileName )
    {
        File targetDir = getSubFile( moduleName + "/target/site" );
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

    protected void activateProfile( String profile )
    {
        addGoal( "-P" + profile );
    }

    public void assertTestSuiteResults( int total, int errors, int failures, int skipped )
    {
        HelperAssertions.assertTestSuiteResults( total, errors, failures, skipped, testDir );
    }

    public void verifyTextInLog( String text )
        throws VerificationException
    {
        verifier.verifyTextInLog( text );
    }

    protected void verifyErrorFreeLog()
        throws VerificationException
    {
        verifier.verifyErrorFreeLog();
    }

    protected Verifier getVerifier()
    {
        return verifier;
    }

    public File getTestDir()
    {
        return testDir;
    }

    protected boolean assertContainsText( File file, String text )
        throws VerificationException
    {
        final List<String> list = getVerifier().loadFile( file, false );
        for ( String line : list )
        {

            if ( line.contains( text ) )
            {
                return true;
            }
        }
        Assert.fail( "Did not find expected message in log" );
        return false; // doh
    }

    protected boolean isMaven3X()
    {
        return matchesVersionRange( "[3.0,)" );
    }

    private DefaultArtifactVersion getMavenVersion()
    {
        try
        {
            String v = verifier.getMavenVersion();
            // NOTE: If the version looks like "${...}" it has been configured from an undefined expression
            if ( v != null && v.length() > 0 && !v.startsWith( "${" ) )
            {
                return new DefaultArtifactVersion( v );
            }
        }
        catch ( VerificationException e )
        {
            throw new RuntimeException( e );
        }

        return null;
    }

    /**
     * This allows fine-grained control over execution of individual test methods
     * by allowing tests to adjust to the current maven version, or else simply avoid
     * executing altogether if the wrong version is present.
     */
    private boolean matchesVersionRange( String versionRangeStr )
    {
        VersionRange versionRange;
        try
        {
            versionRange = VersionRange.createFromVersionSpec( versionRangeStr );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw (RuntimeException) new IllegalArgumentException(
                "Invalid version range: " + versionRangeStr ).initCause( e );
        }

        ArtifactVersion version = getMavenVersion();
        if ( version != null )
        {
            return versionRange.containsVersion( version );
        }
        else
        {
            throw new IllegalStateException( "Cannot determine maven version" );
        }
    }

    protected String getSurefireVersion()
    {
        return surefireVersion;
    }

}
