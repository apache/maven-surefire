package org.apache.maven.surefire.its.fixture;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Contains commonly used features for most tests, encapsulating
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

    private final OutputValidator defaultVerifier;

    private final Map<String, String> envvars = new HashMap<String, String>();

    private final String testNgVersion = System.getProperty( "testng.version" );

    private final String surefireVersion = System.getProperty( "surefire.version" );

    protected SurefireVerifierTestClass( String testProject )
    {
        try
        {
            testDir = simpleExtractResources( getClass(), testProject );
            this.goals = getInitialGoals();
            this.defaultVerifier = OutputValidator.fromDirectory( testDir );
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


    protected void addEnvVar( String key, String value )
    {
        envvars.put( key, value );
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
        defaultVerifier.assertFilePresent( file.getAbsolutePath() );
    }

    protected void assertNotPresent( File file )
    {
        defaultVerifier.assertFileNotPresent( file.getAbsolutePath() );
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

    protected OutputValidator executeTest()
        throws VerificationException
    {
        return execute( "test" );
    }

    protected OutputValidator executeTestWithFailure()
    {
        try {
            execute( "test" );
        } catch (VerificationException ignore) {
        }
        return getDefaultVerifier();
    }

    protected OutputValidator executeVerify()
        throws VerificationException
    {
        return execute( "verify" );
    }

    protected OutputValidator execute( String goal )
        throws VerificationException
    {
        addGoal( goal );
        defaultVerifier.setCliOptions( cliOptions );
        try
        {
            defaultVerifier.executeGoals( goals, envvars );
            return defaultVerifier;
        }
        finally
        {
            defaultVerifier.resetStreams();
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

    protected void redirectToFileReally( boolean redirect )
    {
        addGoal( "-Dmaven.test.redirectTestOutputToFile=" + redirect );
    }

    protected void redirectToFile( boolean redirect )
    {
        redirectToFileReally( redirect );
        //addGoal( "-Dredirect.to.file=" + redirect );
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
    protected void runOrder( String runOrder )
    {
        addGoal( "-DrunOrder=" + runOrder);
    }

    protected void failIfNoTests( boolean fail )
    {
        addGoal( "-DfailIfNoTests=" + fail );
    }

    protected void activateProfile( String profile )
    {
        addGoal( "-P" + profile );
    }

    public void assertTestSuiteResults( int total, int errors, int failures, int skipped )
    {
        getDefaultVerifier().assertTestSuiteResults( total, errors, failures, skipped );
    }

    public void verifyTextInLog( String text )
        throws VerificationException
    {
        defaultVerifier.verifyTextInLog( text );
    }

    protected void verifyErrorFreeLog()
    throws VerificationException
    {
        defaultVerifier.verifyErrorFreeLog();
    }

    protected OutputValidator getDefaultVerifier()
    {
        return defaultVerifier;
    }

    public File getTestDir()
    {
        return testDir;
    }

    protected boolean assertContainsText( File file, String text )
        throws VerificationException
    {
        final List<String> list = getDefaultVerifier().loadFile( file, false );
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

    protected boolean stringsAppearInSpecificOrderInLog( String[] strings )
        throws VerificationException
    {
        int i = 0;
        for ( String line : getLog() )
        {
            if ( line.startsWith( strings[i] ) )
            {
                if ( i == strings.length - 1 )
                {
                    return true;
                }
                ++i;
            }
        }
        return false;
    }

    private List<String> getLog()
        throws VerificationException
    {
        return defaultVerifier.loadFile( defaultVerifier.getBasedir(), defaultVerifier.getLogFileName(), false );
    }



    private DefaultArtifactVersion getMavenVersion()
    {
        try
        {
            String v = defaultVerifier.getMavenVersion();
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

    protected String getSurefireReportGoal()
    {
        return "org.apache.maven.plugins:maven-surefire-report-plugin:" + getSurefireVersion() + ":report";
    }

    protected String getSurefireReportOnlyGoal()
    {
        return "org.apache.maven.plugins:maven-surefire-report-plugin:" + getSurefireVersion() + ":report-only";
    }

    protected String getFailsafeReportOnlyGoal()
    {
        return "org.apache.maven.plugins:maven-surefire-report-plugin:" + getSurefireVersion()
            + ":failsafe-report-only";
    }

    protected void parallel( String parallel )
    {
        addD( "parallel", parallel );
    }


    protected void parallelClasses()
    {
        parallel( "classes" );
    }

    protected void parallelMethods()
    {
        parallel( "methods" );
    }


    protected void addD( String variable, String value )
    {
        addGoal( "-D" + variable + "=" + value );
    }

    public void setGroups( String groups )
    {
        addD( "groups", groups );
    }

    public void setExcludedGroups( String excludedGroups )
    {
        addD( "excludedGroups", excludedGroups );
    }

}
