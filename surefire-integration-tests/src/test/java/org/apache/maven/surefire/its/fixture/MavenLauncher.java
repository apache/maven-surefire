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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.shared.utils.io.FileUtils;

/**
 * Encapsulate all needed features to start a maven run
 * <p/>
 *
 * @author Kristian Rosenvold
 */
public class MavenLauncher
{
    private final List<String> cliOptions = new ArrayList<String>();

    private final List<String> goals = new ArrayList<String>();

    private final Map<String, String> envvars = new HashMap<String, String>();

    private File unpackedAt;

    private Verifier verifier;

    private OutputValidator validator;

    private final Class testCaseBeingRun;

    private final String resourceName;

    private final String suffix;

    private final String[] cli;

    private boolean expectFailure;

    public MavenLauncher( Class testClass, String resourceName, String suffix, String[] cli )
    {
        this.testCaseBeingRun = testClass;
        this.resourceName = resourceName;
        this.suffix = suffix != null ? suffix : "";
        this.cli = cli == null ? null : cli.clone();
        resetGoals();
        resetCliOptions();
    }

    public MavenLauncher( Class testClass, String resourceName, String suffix )
    {
        this( testClass, resourceName, suffix, null );
    }

    public File getUnpackedAt()
    {
        return ensureUnpacked();
    }

    private File ensureUnpacked()
    {
        if ( unpackedAt == null )
        {
            unpackedAt = simpleExtractResources( testCaseBeingRun, resourceName );
        }
        return unpackedAt;
    }

    public void moveUnpackTo( File dest )
        throws IOException
    {
        FileUtils.deleteDirectory( dest );
        //noinspection ResultOfMethodCallIgnored
        getUnpackedAt().renameTo( dest );
        unpackedAt = dest;
    }

    public void resetGoals()
    {
        goals.clear();
    }

    void addCliOption( String cliOption )
    {
        cliOptions.add( cliOption );
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
        resetGoals();
        resetCliOptions();
    }

    private void resetCliOptions()
    {
        cliOptions.clear();
    }

    public MavenLauncher getSubProjectLauncher( String subProject )
        throws VerificationException
    {
        MavenLauncher mavenLauncher =
            new MavenLauncher( testCaseBeingRun, resourceName + File.separator + subProject, suffix, cli );
        mavenLauncher.unpackedAt = new File( ensureUnpacked(), subProject );
        return mavenLauncher;
    }

    public OutputValidator getSubProjectValidator( String subProject )
        throws VerificationException
    {
        final File subFile = getValidator().getSubFile( subProject );
        return new OutputValidator( new Verifier( subFile.getAbsolutePath() ) );
    }


    public MavenLauncher addEnvVar( String key, String value )
    {
        envvars.put( key, value );
        return this;
    }

    public MavenLauncher assertNotPresent( String subFile )
    {
        getVerifier().assertFileNotPresent( getValidator().getSubFile( subFile ).getAbsolutePath() );
        return this;
    }

    public MavenLauncher showErrorStackTraces()
    {
        addCliOption( "-e" );
        return this;
    }

    public MavenLauncher debugLogging()
    {
        addCliOption( "-X" );
        return this;
    }

    public MavenLauncher failNever()
    {
        addCliOption( "-fn" );
        return this;
    }

    public MavenLauncher offline()
    {
        addCliOption( "-o" );
        return this;
    }

    public MavenLauncher skipClean()
    {
        goals.add( "-Dclean.skip=true" );
        return this;
    }

    public MavenLauncher addGoal( String goal )
    {
        goals.add( goal );
        return this;
    }

    public FailsafeOutputValidator executeVerify()
    {
        return new FailsafeOutputValidator( conditionalExec( "verify" ) );
    }

    public OutputValidator executeTest()
    {
        return conditionalExec( "test" );
    }

    private OutputValidator conditionalExec(String goal)
    {
        OutputValidator verify;
        try
        {
            verify = execute( goal );
        }
        catch ( SurefireVerifierException exc )
        {
            if ( expectFailure )
            {
                return getValidator();
            }
            else
            {
                throw exc;
            }
        }
        if ( expectFailure )
        {
            throw new RuntimeException( "Expecting build failure, got none!" );
        }
        return verify;

    }

    public MavenLauncher withFailure()
    {
        this.expectFailure = true;
        return this;
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

    public MavenLauncher activateProfile( String profile )
    {
        return addGoal( "-P" + profile );
    }

    public MavenLauncher sysProp( String variable, String value )
    {
        return addGoal( "-D" + variable + "=" + value );
    }

    public MavenLauncher sysProp( Map<String, String> properties )
    {
        for ( Map.Entry<String, String> property : properties.entrySet() )
        {
            sysProp( property.getKey(), property.getValue() );
        }
        return this;
    }

    public MavenLauncher sysProp( String variable, boolean value )
    {
        return addGoal( "-D" + variable + "=" + value );
    }

    public MavenLauncher sysProp( String variable, int value )
    {
        return addGoal( "-D" + variable + "=" + value );
    }

    public MavenLauncher sysProp( String variable, double value )
    {
        return addGoal( "-D" + variable + "=" + value );
    }

    public MavenLauncher showExceptionMessages()
    {
        addCliOption( "-e" );
        return this;
    }

    public MavenLauncher deleteSiteDir()
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

    public OutputValidator getValidator()
    {
        if ( validator == null )
        {
            this.validator = new OutputValidator( getVerifier() );
        }
        return validator;
    }

    public void setForkJvm( boolean forkJvm ) {
        getVerifier().setForkJvm( forkJvm );
    }

    private Verifier getVerifier()
    {
        if ( verifier == null )
        {
            try
            {
                verifier =
                    cli == null
                    ? new Verifier( ensureUnpacked().getAbsolutePath(), null, false )
                    : new Verifier( ensureUnpacked().getAbsolutePath(), null, false, cli );
            }
            catch ( VerificationException e )
            {
                throw new RuntimeException( e );
            }
        }
        return verifier;
    }
    
    private File simpleExtractResources( Class<?> cl, String resourcePath )
    {
        if ( !resourcePath.startsWith( "/" ) )
        {
            resourcePath = "/" + resourcePath;
        }
        File tempDir = getUnpackDir();
        File testDir = new File( tempDir, resourcePath );
        try
        {
            File parentPom = new File( tempDir.getParentFile(), "pom.xml" );
            if (!parentPom.exists()){
                URL resource = cl.getResource( "/pom.xml" );
                FileUtils.copyURLToFile( resource, parentPom );
            }

            FileUtils.deleteDirectory( testDir );
            File file = ResourceExtractor.extractResourceToDestination( cl, resourcePath, tempDir, true );
            return file.getCanonicalFile();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }

    }

    File getUnpackDir()
    {
        String tempDirPath = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        return new File( tempDirPath,
                         testCaseBeingRun.getSimpleName() + "_" + getTestMethodName() + suffix );
    }

    public File getArtifactPath( String gid, String aid, String version, String ext )
    {
        return new File( verifier.getArtifactPath( gid, aid, version, ext ) );
    }

    String getTestMethodName()
    {
        // dirty. Im sure we can use junit4 rules to attach testname to thread instead
        StackTraceElement[] stackTrace = getStackTraceElements();
        StackTraceElement topInTestClass;
        topInTestClass = findTopElemenent( stackTrace, testCaseBeingRun );
        if ( topInTestClass == null )
        {
            // Look in superclass...
            topInTestClass = findTopElemenent( stackTrace, testCaseBeingRun.getSuperclass() );
        }
        if ( topInTestClass != null )
        {
            return topInTestClass.getMethodName();
        }
        throw new IllegalStateException( "Cannot find " + testCaseBeingRun.getName() + "in stacktrace" );
    }
}
