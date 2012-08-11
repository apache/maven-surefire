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

import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulate all needed features to start a surefire run
 * <p/>
 * Also includes thread-safe access to the extracted resource
 * files, which AbstractSurefireIntegrationTestClass does not.
 * Thread safe only for running in "classes" mode.
 *
 * @author Kristian Rosenvold
 */
public class SurefireLauncher
{
    private final List<String> cliOptions = new ArrayList<String>();

    private final List<String> goals = getInitialGoals();

    private final Map<String, String> envvars = new HashMap<String, String>();

    private final String testNgVersion = System.getProperty( "testng.version" );

    private final String surefireVersion = System.getProperty( "surefire.version" );

    private final Verifier verifier;

    private final OutputValidator surefireVerifier;

    private boolean failIfNoTests;


    public SurefireLauncher( Class testClass, String resourceName )
        throws VerificationException, IOException
    {
        this( createVerifier( testClass, resourceName ) );

    }

    public SurefireLauncher( Verifier verifier )
    {
        this.verifier = verifier;
        this.surefireVerifier = new OutputValidator( verifier );
        goals.clear();
        goals.addAll( getInitialGoals() );
        cliOptions.clear();
    }

    private static Verifier createVerifier( Class testClass, String resourceName )
        throws IOException, VerificationException
    {
        return new Verifier( simpleExtractResources( testClass, resourceName ).getAbsolutePath() );
    }

    private static File simpleExtractResources( Class<?> cl, String resourcePath )
        throws IOException
    {
        if ( !resourcePath.startsWith( "/" ) )
        {
            resourcePath = "/" + resourcePath;
        }
        String tempDirPath = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File tempDir = new File( tempDirPath, cl.getSimpleName() );
        File testDir = new File( tempDir, resourcePath );
        FileUtils.deleteDirectory( testDir );

        return ResourceExtractor.extractResourcePath( cl, resourcePath, tempDir, true );
    }

    public void reset()
    {
        goals.clear();
        goals.addAll( getInitialGoals() );
        cliOptions.clear();
    }

    public SurefireLauncher getSubProjectLauncher( String subProject )
        throws VerificationException
    {
        final File subFile = surefireVerifier.getSubFile( subProject );
        return new SurefireLauncher( new Verifier( subFile.getAbsolutePath() ) );
    }

    public OutputValidator getSubProjectValidator( String subProject )
        throws VerificationException
    {
        final File subFile = surefireVerifier.getSubFile( subProject );
        return new OutputValidator( new Verifier( subFile.getAbsolutePath() ) );
    }

    public SurefireLauncher addEnvVar( String key, String value )
    {
        envvars.put( key, value );
        return this;
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

    // Todo remove duplication between this and getInitialGoals
    public SurefireLauncher resetInitialGoals( String testNgVersion )
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

        this.goals.clear();
        this.goals.addAll( goals );
        return this;
    }


    public SurefireLauncher assertNotPresent( String subFile )
    {
        verifier.assertFileNotPresent( surefireVerifier.getSubFile( subFile ).getAbsolutePath() );
        return this;
    }

    public SurefireLauncher showErrorStackTraces()
    {
        cliOptions.add( "-e" );
        return this;
    }

    public SurefireLauncher debugLogging()
    {
        cliOptions.add( "-X" );
        return this;
    }

    public SurefireLauncher failNever()
    {
        cliOptions.add( "-fn" );
        return this;

    }

    public SurefireLauncher skipClean()
    {
        goals.add( "-Dclean.skip=true" );
        return this;
    }

    public SurefireLauncher groups( String groups )
    {
        goals.add( "-Dgroups=" + groups );
        return this;
    }


    public SurefireLauncher addGoal( String goal )
    {
        goals.add( goal );
        return this;
    }

    public OutputValidator executeTest()
    {
        return execute( "test" );
    }

    public OutputValidator executeInstall()
        throws VerificationException
    {
        return execute( "install" );
    }

    public OutputValidator executeTestWithFailure()
    {
        try
        {
            execute( "test" );
        }
        catch ( SurefireVerifierException ignore )
        {
            return surefireVerifier;
        }
        throw new RuntimeException( "Expecting build failure, got none!" );
    }

    public OutputValidator executeVerifyWithFailure()
    {
        try
        {
            executeVerify();
        }
        catch ( SurefireVerifierException ignore )
        {
            return surefireVerifier;
        }
        throw new RuntimeException( "Expecting build failure, got none!" );
    }


    public FailsafeOutputValidator executeVerify()
    {
        OutputValidator verify = execute( "verify" );
        return new FailsafeOutputValidator( verify.getVerifier());
    }

    public OutputValidator execute( String goal )
    {
        addGoal( goal );
        return executeCurrentGoals();
    }

    public OutputValidator executeCurrentGoals()
    {

        String userLocalRepo = System.getProperty( "user.localRepository" );
        String testBuildDirectory = System.getProperty( "testBuildDirectory" );
        boolean useInterpolatedSettings = Boolean.getBoolean( "useInterpolatedSettings" );

        try
        {
            if ( useInterpolatedSettings )
            {
                File interpolatedSettings = new File( testBuildDirectory, "interpolated-settings" );

                if ( !interpolatedSettings.exists() )
                {
                    // hack "a la" invoker plugin to download dependencies from local repo
                    // and not download from central

                    Map<String, String> values = new HashMap<String, String>( 1 );
                    values.put( "localRepositoryUrl", toUrl( userLocalRepo ) );
                    StrSubstitutor strSubstitutor = new StrSubstitutor( values );

                    String fileContent = FileUtils.fileRead( new File( testBuildDirectory, "settings.xml" ) );

                    String filtered = strSubstitutor.replace( fileContent );

                    FileUtils.fileWrite( interpolatedSettings.getAbsolutePath(), filtered );

                }

                cliOptions.add( "-s " + interpolatedSettings.getCanonicalPath() );
            }
            verifier.setCliOptions( cliOptions );

            verifier.executeGoals( goals, envvars );
            return surefireVerifier;
        }
        catch ( IOException e )
        {
            throw new SurefireVerifierException( e.getMessage(), e );
        }
        catch ( VerificationException e )
        {
            throw new SurefireVerifierException( e.getMessage(), e );
        }
        finally
        {
            verifier.resetStreams();
        }
    }

    private static String toUrl( String filename )
    {
        /*
         * NOTE: Maven fails to properly handle percent-encoded "file:" URLs (WAGON-111) so don't use File.toURI() here
         * as-is but use the decoded path component in the URL.
         */
        String url = "file://" + new File( filename ).toURI().getPath();
        if ( url.endsWith( "/" ) )
        {
            url = url.substring( 0, url.length() - 1 );
        }
        return url;
    }


    public SurefireLauncher printSummary( boolean printsummary )
    {
        return addGoal( "-DprintSummary=" + printsummary );
    }

    public SurefireLauncher redirectToFileReally( boolean redirect )
    {
        return addGoal( "-Dmaven.test.redirectTestOutputToFile=" + redirect );
    }

    public SurefireLauncher redirectToFile( boolean redirect )
    {
        return redirectToFileReally( redirect );
        //addGoal( "-Dredirect.to.file=" + redirect );
    }

    public SurefireLauncher forkOnce()
    {
        return forkMode( "once" );
    }

    public SurefireLauncher forkNever()
    {
        return forkMode( "never" );
    }

    public SurefireLauncher forkAlways()
    {
        return forkMode( "always" );
    }

    public SurefireLauncher forkPerTest()
    {
        return forkMode( "pertest" );
    }

    public SurefireLauncher forkMode( String forkMode )
    {
        return addGoal( "-DforkMode=" + forkMode );
    }

    public SurefireLauncher runOrder( String runOrder )
    {
        return addGoal( "-DrunOrder=" + runOrder );
    }

    public SurefireLauncher failIfNoTests( boolean fail )
    {
        this.failIfNoTests = fail;
        return addGoal( "-DfailIfNoTests=" + fail );
    }

    public SurefireLauncher failIfNoSpecifiedTests( boolean fail )
    {
        this.failIfNoTests = fail;
        return addGoal( "-Dsurefire.failIfNoSpecifiedTests=" + fail );
    }

    public SurefireLauncher useSystemClassLoader( boolean useSystemClassLoader )
    {
        return addGoal( "-DuseSystemClassLoader=" + useSystemClassLoader );
    }

    public SurefireLauncher activateProfile( String profile )
    {
        return addGoal( "-P" + profile );
    }


    protected String getSurefireVersion()
    {
        return surefireVersion;
    }

    public SurefireLauncher parallel( String parallel )
    {
        return addD( "parallel", parallel );
    }


    public SurefireLauncher parallelClasses()
    {
        return parallel( "classes" );
    }

    public SurefireLauncher parallelMethods()
    {
        return parallel( "methods" );
    }


    public SurefireLauncher addD( String variable, String value )
    {
        return addGoal( "-D" + variable + "=" + value );
    }

    public SurefireLauncher setJUnitVersion( String version )
    {
        addD( "junit.version", version );
        return this;
    }

    public SurefireLauncher setGroups( String groups )
    {
        return addD( "groups", groups );
    }

    public SurefireLauncher setExcludedGroups( String excludedGroups )
    {
        return addD( "excludedGroups", excludedGroups );
    }

    public SurefireLauncher setEOption()
    {
        cliOptions.add( "-e" );
        return this;
    }


    public boolean isFailIfNoTests()
    {
        return failIfNoTests;
    }

    public File getUnpackLocation()
    {
        return new File( verifier.getBasedir() );
    }

    public SurefireLauncher addFailsafeReportOnlyGoal()
    {
        goals.add(
            "org.apache.maven.plugins:maven-surefire-report-plugin:" + getSurefireVersion() + ":failsafe-report-only" );
        return this;
    }

    public SurefireLauncher addSurefireReportGoal()
    {
        goals.add( "org.apache.maven.plugins:maven-surefire-report-plugin:" + getSurefireVersion() + ":report" );
        return this;
    }

    public SurefireLauncher addSurefireReportOnlyGoal()
    {
        goals.add( "org.apache.maven.plugins:maven-surefire-report-plugin:" + getSurefireVersion() + ":report-only" );
        return this;
    }


    public SurefireLauncher deleteSiteDir()
    {
        try
        {
            FileUtils.deleteDirectory( surefireVerifier.getSubFile( "site" ) );
        }
        catch ( IOException e )
        {
            throw new SurefireVerifierException( e );
        }
        return this;
    }

    public SurefireLauncher setTestToRun( String basicTest )
    {
        addD( "test", basicTest );
        return this;
    }

    public OutputValidator getSurefireVerifier()
    {
        return surefireVerifier;
    }
}
