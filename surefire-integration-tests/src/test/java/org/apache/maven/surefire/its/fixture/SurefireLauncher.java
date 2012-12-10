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
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.shared.utils.io.FileUtils;

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

    private final String testNgVersion = System.getProperty( "testng.version" );

    private final String surefireVersion = System.getProperty( "surefire.version" );

    private final List<String> goals = getInitialGoals( testNgVersion );

    private final Map<String, String> envvars = new HashMap<String, String>();

    private Verifier verifier;

    private OutputValidator validator;

    private boolean failIfNoTests;

    private final Class testClass;

    private final String resourceName;

    private final String suffix;


    public SurefireLauncher( Class testClass, String resourceName, String suffix )
        throws VerificationException, IOException
    {
        this.testClass = testClass;
        this.resourceName = resourceName;
        this.suffix = suffix != null ? suffix : "";
        goals.clear();
        goals.addAll( getInitialGoals( testNgVersion ) );
        resetCliOptions();
    }

    public SurefireLauncher( Verifier verifier )
    {
        this.testClass = null;
        this.resourceName = null;
        this.suffix = "";
        this.verifier = verifier;
        goals.clear();
        goals.addAll( getInitialGoals( testNgVersion ) );
        resetCliOptions();
    }

    private void addCliOption( String cliOption )
    {
        cliOptions.add( cliOption );
    }

    private Verifier createVerifier( Class testClass, String resourceName )
        throws IOException, VerificationException
    {
        return new Verifier( simpleExtractResources( testClass, resourceName ).getAbsolutePath() );
    }

    private File simpleExtractResources( Class<?> cl, String resourcePath )
        throws IOException
    {
        if ( !resourcePath.startsWith( "/" ) )
        {
            resourcePath = "/" + resourcePath;
        }
        File tempDir = getUnpackDir();
        File testDir = new File( tempDir, resourcePath );
        FileUtils.deleteDirectory( testDir );

        File file = ResourceExtractor.extractResourceToDestination( cl, resourcePath, tempDir, true );
        return file.getCanonicalFile();
    }

    private File getUnpackDir()
    {
        String tempDirPath = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        return new File( tempDirPath, testClass.getSimpleName() + File.separator + getTestMethodName() + suffix );
    }

    String getTestMethodName()
    {
        // dirty. Im sure we can use junit4 rules to attach testname to thread instead
        StackTraceElement[] stackTrace = getStackTraceElements();
        StackTraceElement topInTestClass;
        topInTestClass = findTopElemenent( stackTrace, testClass );
        if ( topInTestClass == null )
        {
            // Look in superclass...
            topInTestClass = findTopElemenent( stackTrace, testClass.getSuperclass() );
        }
        if ( topInTestClass != null )
        {
            return topInTestClass.getMethodName();
        }
        throw new IllegalStateException( "Cannot find " + testClass.getName() + "in stacktrace" );
    }

    private StackTraceElement findTopElemenent( StackTraceElement[] stackTrace, Class testClassToLookFor )
    {
        StackTraceElement bestmatch = null;
        for ( StackTraceElement stackTraceElement : stackTrace )
        {
            if ( stackTraceElement.getClassName().equals( testClassToLookFor.getName() ) )
            {
                bestmatch = stackTraceElement;
            }
        }
        return bestmatch;
    }

    StackTraceElement[] getStackTraceElements()
    {
        try
        {
            throw new RuntimeException();
        }
        catch ( RuntimeException e )
        {
            return e.getStackTrace();
        }
    }

    public void reset()
    {
        goals.clear();
        goals.addAll( getInitialGoals( testNgVersion ) );
        resetCliOptions();
    }

    private void resetCliOptions()
    {
        cliOptions.clear();
        offline();
    }

    public SurefireLauncher getSubProjectLauncher( String subProject )
        throws VerificationException
    {
        final File subFile = getValidator().getSubFile( subProject );
        return new SurefireLauncher( new Verifier( subFile.getAbsolutePath() ) );
    }

    public OutputValidator getSubProjectValidator( String subProject )
        throws VerificationException
    {
        final File subFile = getValidator().getSubFile( subProject );
        return new OutputValidator( new Verifier( subFile.getAbsolutePath() ) );
    }

    public SurefireLauncher addEnvVar( String key, String value )
    {
        envvars.put( key, value );
        return this;
    }

    private List<String> getInitialGoals( String testNgVersion1 )
    {
        List<String> goals1 = new ArrayList<String>();
        goals1.add( "-Dsurefire.version=" + surefireVersion );

        if ( testNgVersion != null )
        {
            goals1.add( "-DtestNgVersion=" + testNgVersion1 );

            ArtifactVersion v = new DefaultArtifactVersion( testNgVersion1 );
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

    public SurefireLauncher resetInitialGoals( String testNgVersion )
    {
        this.goals.clear();
        this.goals.addAll( getInitialGoals( testNgVersion ) );
        return this;
    }


    public SurefireLauncher assertNotPresent( String subFile )
    {
        getVerifier().assertFileNotPresent( getValidator().getSubFile( subFile ).getAbsolutePath() );
        return this;
    }

    public SurefireLauncher showErrorStackTraces()
    {
        addCliOption( "-e" );
        return this;
    }

    public SurefireLauncher debugLogging()
    {
        addCliOption( "-X" );
        return this;
    }

    public SurefireLauncher debugSurefireFork()
    {
        addD( "maven.surefire.debug", "true" );
        return this;
    }

    public SurefireLauncher failNever()
    {
        addCliOption( "-fn" );
        return this;
    }

    public SurefireLauncher offline()
    {
        addCliOption( "-o" );
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
        addCliOption( "-o" );
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
            return getValidator();
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
            return getValidator();
        }
        throw new RuntimeException( "Expecting build failure, got none!" );
    }


    public FailsafeOutputValidator executeVerify()
    {
        OutputValidator verify = execute( "verify" );
        return new FailsafeOutputValidator( verify.getVerifier() );
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

                addCliOption( "-s " + interpolatedSettings.getCanonicalPath() );
            }
            getVerifier().setCliOptions( cliOptions );

            getVerifier().executeGoals( goals, envvars );
            return getValidator();
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
            getVerifier().resetStreams();
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

    public SurefireLauncher forkPerThread()
    {
        return forkMode( "perthread" );
    }

    public SurefireLauncher forkOncePerThread()
    {
        return forkMode( "onceperthread" );
    }

    public SurefireLauncher threadCount( int threadCount )
    {
        return addGoal( "-DthreadCount=" + threadCount );
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
        addCliOption( "-e" );
        return this;
    }


    public boolean isFailIfNoTests()
    {
        return failIfNoTests;
    }

    public File getUnpackLocation()
    {
        getVerifier(); // Make sure we have unpacked
        return getUnpackDir();
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
            FileUtils.deleteDirectory( getValidator().getSubFile( "site" ) );
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

    public OutputValidator getValidator()
    {
        if ( validator == null )
        {
            this.validator = new OutputValidator( getVerifier() );
        }
        return validator;
    }

    private Verifier getVerifier()
    {
        if ( verifier == null )
        {
            try
            {
                this.verifier = createVerifier( testClass, resourceName );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
            catch ( VerificationException e )
            {
                throw new RuntimeException( e );
            }
        }
        return verifier;
    }
}
