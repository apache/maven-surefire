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
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.it.VerificationException;

/**
 * Encapsulate all needed features to start a surefire run
 * <p/>
 * Also includes thread-safe access to the extracted resource
 * files, which AbstractSurefireIntegrationTestClass does not.
 * Thread safe only for running in "classes" mode.
 *
 * @author Kristian Rosenvold                                 -
 */
public class SurefireLauncher
{

    private final MavenLauncher mavenLauncher;

    private final String testNgVersion = System.getProperty( "testng.version" );

    private final String surefireVersion = System.getProperty( "surefire.version" );

    private boolean failIfNoTests;

    public SurefireLauncher( MavenLauncher mavenLauncher )
    {
        this.mavenLauncher = mavenLauncher;
        reset();
    }

    public MavenLauncher maven()
    {
        return mavenLauncher;
    }

    private void addCliOption( String cliOption )
    {
        mavenLauncher.addCliOption( cliOption );
    }

    String getTestMethodName()
    {
        return mavenLauncher.getTestMethodName();
    }

    public void reset()
    {
        mavenLauncher.reset();
        for ( String s : getInitialGoals( testNgVersion ) )
        {
            mavenLauncher.addGoal( s );
        }
    }


    public SurefireLauncher getSubProjectLauncher( String subProject )
        throws VerificationException
    {
        return new SurefireLauncher( mavenLauncher.getSubProjectLauncher( subProject ) );
    }

    public OutputValidator getSubProjectValidator( String subProject )
        throws VerificationException
    {
        return mavenLauncher.getSubProjectValidator( subProject );
    }

    public SurefireLauncher addEnvVar( String key, String value )
    {
        mavenLauncher.addEnvVar( key, value );
        return this;
    }

    private List<String> getInitialGoals( String testNgVersion )
    {
        List<String> goals1 = new ArrayList<String>();
        goals1.add( "-Dsurefire.version=" + surefireVersion );

        if ( this.testNgVersion != null )
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

    public SurefireLauncher resetInitialGoals( String testNgVersion )
    {
        mavenLauncher.resetGoals();
        for ( String s : getInitialGoals( testNgVersion ) )
        {
            mavenLauncher.addGoal( s );
        }
        return this;
    }


    public SurefireLauncher assertNotPresent( String subFile )
    {
        mavenLauncher.assertNotPresent( subFile );
        return this;
    }

    public SurefireLauncher showErrorStackTraces()
    {
        mavenLauncher.showErrorStackTraces();
        return this;
    }

    public SurefireLauncher debugLogging()
    {
        mavenLauncher.debugLogging();
        return this;
    }

    public SurefireLauncher debugSurefireFork()
    {
        mavenLauncher.addD( "maven.surefire.debug", "true" );
        return this;
    }

    public SurefireLauncher failNever()
    {
        mavenLauncher.failNever();
        return this;
    }

    public SurefireLauncher offline()
    {
        mavenLauncher.offline();
        return this;
    }

    public SurefireLauncher groups( String groups )
    {
        mavenLauncher.addD( "groups", groups );
        return this;
    }


    public SurefireLauncher addGoal( String goal )
    {
        mavenLauncher.addGoal( goal );
        return this;
    }

    public OutputValidator executeTest()
    {
        mavenLauncher.addCliOption( "-o" );
        return mavenLauncher.execute( "test" );
    }

    public OutputValidator executeInstall()
        throws VerificationException
    {
        return mavenLauncher.execute( "install" );
    }

    public OutputValidator executeTestWithFailure()
    {
        try
        {
            execute( "test" );
        }
        catch ( SurefireVerifierException ignore )
        {
            return mavenLauncher.getValidator();
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
            return mavenLauncher.getValidator();
        }
        throw new RuntimeException( "Expecting build failure, got none!" );
    }


    public FailsafeOutputValidator executeVerify()
    {
        OutputValidator verify = execute( "verify" );
        return new FailsafeOutputValidator( verify );
    }

    public OutputValidator execute( String goal )
    {
        return mavenLauncher.execute( goal );
    }

    public OutputValidator executeCurrentGoals()
    {
        return mavenLauncher.executeCurrentGoals();
    }


    public SurefireLauncher printSummary( boolean printsummary )
    {
        mavenLauncher.addD( "printSummary", printsummary );
        return this;
    }

    public SurefireLauncher redirectToFile( boolean redirect )
    {
        mavenLauncher.addD( "maven.test.redirectTestOutputToFile", redirect );
        return this;
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
        mavenLauncher.addD( "threadCount", threadCount );
        return this;
    }

    public SurefireLauncher forkMode( String forkMode )
    {
        mavenLauncher.addD( "forkMode", forkMode );
        return this;
    }

    public SurefireLauncher runOrder( String runOrder )
    {
        mavenLauncher.addD( "runOrder", runOrder );
        return this;
    }

    public SurefireLauncher failIfNoTests( boolean fail )
    {
        this.failIfNoTests = fail;
        mavenLauncher.addD( "failIfNoTests", fail );
        return this;
    }


    public SurefireLauncher mavenTestFailureIgnore( boolean fail )
    {
        mavenLauncher.addD( "maven.test.failure.ignore", fail );
        return this;
    }

    public SurefireLauncher failIfNoSpecifiedTests( boolean fail )
    {
        this.failIfNoTests = fail;
        mavenLauncher.addD( "surefire.failIfNoSpecifiedTests", fail );
        return this;
    }

    public SurefireLauncher useSystemClassLoader( boolean useSystemClassLoader )
    {
        mavenLauncher.addD( "useSystemClassLoader", useSystemClassLoader );
        return this;
    }

    public SurefireLauncher activateProfile( String profile )
    {
        mavenLauncher.activateProfile( profile );
        return this;
    }


    protected String getSurefireVersion()
    {
        return surefireVersion;
    }

    public SurefireLauncher parallel( String parallel )
    {

        mavenLauncher.addD( "parallel", parallel );
        return this;
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
        mavenLauncher.addD( variable, value );
        return this;
    }

    public SurefireLauncher setJUnitVersion( String version )
    {
        mavenLauncher.addD( "junit.version", version );
        return this;
    }

    public SurefireLauncher setGroups( String groups )
    {
        mavenLauncher.addD( "groups", groups );
        return this;
    }

    public SurefireLauncher setExcludedGroups( String excludedGroups )
    {
        mavenLauncher.addD( "excludedGroups", excludedGroups );
        return this;
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

    public File getUnpackedAt()
    {
        return mavenLauncher.getUnpackedAt();
    }

    public SurefireLauncher addFailsafeReportOnlyGoal()
    {
        mavenLauncher.addGoal( getReportPluginGoal( ":failsafe-report-only" ) );
        return this;
    }

    public SurefireLauncher addSurefireReportGoal()
    {
        mavenLauncher.addGoal( getReportPluginGoal( "report" ) );
        return this;
    }

    public SurefireLauncher addSurefireReportOnlyGoal()
    {
        mavenLauncher.addGoal( getReportPluginGoal( "report-only" ) );
        return this;
    }

    private String getReportPluginGoal( String goal )
    {
        return "org.apache.maven.plugins:maven-surefire-report-plugin:" + getSurefireVersion() + ":" + goal;
    }


    public SurefireLauncher deleteSiteDir()
    {
        mavenLauncher.deleteSiteDir();
        return this;
    }

    public SurefireLauncher setTestToRun( String basicTest )
    {
        mavenLauncher.addD( "test", basicTest );
        return this;
    }
}
