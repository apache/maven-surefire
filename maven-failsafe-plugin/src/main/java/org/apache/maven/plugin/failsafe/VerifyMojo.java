package org.apache.maven.plugin.failsafe;

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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.surefire.AbstractSurefireMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.surefire.booter.SurefireBooter;
import org.apache.maven.surefire.failsafe.model.FailsafeSummary;
import org.apache.maven.surefire.failsafe.model.io.xpp3.FailsafeSummaryXpp3Reader;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Verify integration tests ran using Surefire.
 *
 * @author Stephen Connolly
 * @author Jason van Zyl
 * @requiresProject true
 * @goal verify
 * @phase verify
 */
public class VerifyMojo
    extends AbstractSurefireMojo
{

    /**
     * Set this to 'true' to skip running tests, but still compile them. Its use is NOT RECOMMENDED, but quite
     * convenient on occasion.
     *
     * @parameter expression="${skipTests}"
     * @since 2.4
     */
    private boolean skipTests;

    /**
     * Set this to 'true' to skip running integration tests, but still compile them. Its use is NOT RECOMMENDED, but quite
     * convenient on occasion.
     *
     * @parameter expression="${skipITs}"
     * @since 2.4.3-alpha-2
     */
    private boolean skipITs;

    /**
     * This old parameter is just like skipTests, but bound to the old property maven.test.skip.exec.
     *
     * @parameter expression="${maven.test.skip.exec}"
     * @since 2.3
     * @deprecated Use -DskipTests instead.
     */
    private boolean skipExec;

    /**
     * Set this to 'true' to bypass unit tests entirely. Its use is NOT RECOMMENDED, especially if you
     * enable it using the "maven.test.skip" property, because maven.test.skip disables both running the
     * tests and compiling the tests.  Consider using the skipTests parameter instead.
     *
     * @parameter default-value="false" expression="${maven.test.skip}"
     */
    private boolean skip;

    /**
     * Set this to true to ignore a failure during testing. Its use is NOT RECOMMENDED, but quite convenient on
     * occasion.
     *
     * @parameter default-value="false" expression="${maven.test.failure.ignore}"
     */
    private boolean testFailureIgnore;

    /**
     * The base directory of the project being tested. This can be obtained in your unit test by
     * System.getProperty("basedir").
     *
     * @parameter default-value="${basedir}"
     */
    private File basedir;

    /**
     * The directory containing generated test classes of the project being tested.
     * This will be included at the beginning the test classpath.
     *
     * @parameter default-value="${project.build.testOutputDirectory}"
     */
    private File testClassesDirectory;

    /**
     * Base directory where all reports are written to.
     *
     * @parameter default-value="${project.build.directory}/failsafe-reports"
     */
    private File reportsDirectory;

    /**
     * The summary file to write integration test results to.
     *
     * @parameter expression="${project.build.directory}/failsafe-reports/failsafe-summary.xml"
     * @required
     */
    private File summaryFile;

    /**
     * Set this to "true" to cause a failure if there are no tests to run.
     *
     * @parameter expression="${failIfNoTests}"
     * @since 2.4
     */
    private Boolean failIfNoTests;

    /**
     * The character encoding scheme to be applied.
     *
     * @parameter expression="${encoding}" default-value="${project.reporting.outputEncoding}"
     */
    private String encoding;


    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( verifyParameters() )
        {
            getLog().info(
                StringUtils.capitalizeFirstLetter( getPluginName() ) + " report directory: " + getReportsDirectory() );

            int result;
            try
            {
                String encoding;
                if ( StringUtils.isEmpty( this.getEncoding() ) )
                {
                    getLog().warn(
                        "File encoding has not been set, using platform encoding " + ReaderFactory.FILE_ENCODING
                            + ", i.e. build is platform dependent!" );
                    encoding = ReaderFactory.FILE_ENCODING;
                }
                else
                {
                    encoding = this.getEncoding();
                }

                FileInputStream fos = new FileInputStream( getSummaryFile() );
                BufferedInputStream bos = new BufferedInputStream( fos );
                Reader w = new InputStreamReader( bos, encoding );
                FailsafeSummaryXpp3Reader reader = new FailsafeSummaryXpp3Reader();
                final FailsafeSummary summary = reader.read( w );
                result = summary.getResult();
                w.close();
                bos.close();
                fos.close();
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( e.getMessage(), e );
            }
            catch ( XmlPullParserException e )
            {
                throw new MojoExecutionException( e.getMessage(), e );
            }

            if ( result == 0 )
            {
                return;
            }

            String msg;

            if ( result == SurefireBooter.NO_TESTS_EXIT_CODE )
            {
                if ( ( getFailIfNoTests() == null ) || !getFailIfNoTests().booleanValue() )
                {
                    return;
                }
                // TODO: i18n
                throw new MojoFailureException(
                    "No tests were executed!  (Set -DfailIfNoTests=false to ignore this error.)" );
            }
            else
            {
                // TODO: i18n
                msg = "There are test failures.\n\nPlease refer to " + getReportsDirectory()
                    + " for the individual test results.";

            }

            if ( isTestFailureIgnore() )
            {
                getLog().error( msg );
            }
            else
            {
                throw new MojoFailureException( msg );
            }
        }
    }

    protected boolean isTestsSkipped()
    {
        return isSkip() || isSkipTests() || isSkipITs() || isSkipExec();
    }

    protected String getPluginName()
    {
        return "failsafe";
    }

    protected String[] getDefaultIncludes()
    {
        return null;
    }

    public boolean isSkipTests()
    {
        return skipTests;
    }

    public void setSkipTests( boolean skipTests )
    {
        this.skipTests = skipTests;
    }

    public boolean isSkipITs()
    {
        return skipITs;
    }

    public void setSkipITs( boolean skipITs )
    {
        this.skipITs = skipITs;
    }

    public boolean isSkipExec()
    {
        return skipExec;
    }

    public void setSkipExec( boolean skipExec )
    {
        this.skipExec = skipExec;
    }

    public boolean isSkip()
    {
        return skip;
    }

    public void setSkip( boolean skip )
    {
        this.skip = skip;
    }

    public boolean isTestFailureIgnore()
    {
        return testFailureIgnore;
    }

    public void setTestFailureIgnore( boolean testFailureIgnore )
    {
        this.testFailureIgnore = testFailureIgnore;
    }

    public File getBasedir()
    {
        return basedir;
    }

    public void setBasedir( File basedir )
    {
        this.basedir = basedir;
    }

    public File getTestClassesDirectory()
    {
        return testClassesDirectory;
    }

    public void setTestClassesDirectory( File testClassesDirectory )
    {
        this.testClassesDirectory = testClassesDirectory;
    }

    public File getReportsDirectory()
    {
        return reportsDirectory;
    }

    public void setReportsDirectory( File reportsDirectory )
    {
        this.reportsDirectory = reportsDirectory;
    }

    public File getSummaryFile()
    {
        return summaryFile;
    }

    public void setSummaryFile( File summaryFile )
    {
        this.summaryFile = summaryFile;
    }

    public Boolean getFailIfNoTests()
    {
        return failIfNoTests;
    }

    public void setFailIfNoTests( Boolean failIfNoTests )
    {
        this.failIfNoTests = failIfNoTests;
    }

    public String getEncoding()
    {
        return encoding;
    }

    public void setEncoding( String encoding )
    {
        this.encoding = encoding;
    }

    // the following will be refactored out once the common code is all in one place

    public File getClassesDirectory()
    {
        return null; // ignore
    }

    public void setClassesDirectory( File classesDirectory )
    {
        // ignore
    }

    public MavenProject getProject()
    {
        return null; // ignore
    }

    public void setProject( MavenProject project )
    {
        // ignore
    }

    public String getIgnoreClasspathElements()
    {
        return null; // ignore
    }

    public void setIgnoreClasspathElements( String ignoreClasspathElements )
    {
        // ignore
    }

    public List getAdditionalClasspathElements()
    {
        return null; // ignore
    }

    public void setAdditionalClasspathElements( List additionalClasspathElements )
    {
        // ignore
    }

    public File getTestSourceDirectory()
    {
        return null; // ignore
    }

    public void setTestSourceDirectory( File testSourceDirectory )
    {
        // ignore
    }

    public String getTest()
    {
        return null; // ignore
    }

    public void setTest( String test )
    {
        // ignore
    }

    public List getIncludes()
    {
        return null; // ignore
    }

    public void setIncludes( List includes )
    {
        // ignore
    }

    public List getExcludes()
    {
        return null; // ignore
    }

    public void setExcludes( List excludes )
    {
        // ignore
    }

    public ArtifactRepository getLocalRepository()
    {
        return null; // ignore
    }

    public void setLocalRepository( ArtifactRepository localRepository )
    {
        // ignore
    }

    public Properties getSystemProperties()
    {
        return null; // ignore
    }

    public void setSystemProperties( Properties systemProperties )
    {
        // ignore
    }

    public Map getSystemPropertyVariables()
    {
        return null; // ignore
    }

    public void setSystemPropertyVariables( Map systemPropertyVariables )
    {
        // ignore
    }

    public Properties getProperties()
    {
        return null; // ignore
    }

    public void setProperties( Properties properties )
    {
        // ignore
    }

    public Map getPluginArtifactMap()
    {
        return null; // ignore
    }

    public void setPluginArtifactMap( Map pluginArtifactMap )
    {
        // ignore
    }

    public Map getProjectArtifactMap()
    {
        return null; // ignore
    }

    public void setProjectArtifactMap( Map projectArtifactMap )
    {
        // ignore
    }

    public boolean isPrintSummary()
    {
        return false; // ignore
    }

    public void setPrintSummary( boolean printSummary )
    {
        // ignore
    }

    public String getReportFormat()
    {
        return null; // ignore
    }

    public void setReportFormat( String reportFormat )
    {
        // ignore
    }

    public boolean isUseFile()
    {
        return false; // ignore  
    }

    public void setUseFile( boolean useFile )
    {
        // ignore
    }

    public boolean isRedirectTestOutputToFile()
    {
        return false; // ignore
    }

    public void setRedirectTestOutputToFile( boolean redirectTestOutputToFile )
    {
        // ignore
    }

    public String getForkMode()
    {
        return null; // ignore
    }

    public void setForkMode( String forkMode )
    {
        // ignore
    }

    public String getJvm()
    {
        return null; // ignore
    }

    public void setJvm( String jvm )
    {
        // ignore
    }

    public String getArgLine()
    {
        return null; // ignore
    }

    public void setArgLine( String argLine )
    {
        // ignore
    }

    public String getDebugForkedProcess()
    {
        return null; // ignore
    }

    public void setDebugForkedProcess( String debugForkedProcess )
    {
        // ignore
    }

    public int getForkedProcessTimeoutInSeconds()
    {
        return 0;  // ignore
    }

    public void setForkedProcessTimeoutInSeconds( int forkedProcessTimeoutInSeconds )
    {
        // ignore
    }

    public Map getEnvironmentVariables()
    {
        return null;  // ignore
    }

    public void setEnvironmentVariables( Map environmentVariables )
    {
        // ignore
    }

    public File getWorkingDirectory()
    {
        return null;  // ignore
    }

    public void setWorkingDirectory( File workingDirectory )
    {
        // ignore
    }

    public boolean isChildDelegation()
    {
        return false;  // ignore
    }

    public void setChildDelegation( boolean childDelegation )
    {
        // ignore
    }

    public String getGroups()
    {
        return null;  // ignore
    }

    public void setGroups( String groups )
    {
        // ignore
    }

    public String getExcludedGroups()
    {
        return null;  // ignore
    }

    public void setExcludedGroups( String excludedGroups )
    {
        // ignore
    }

    public File[] getSuiteXmlFiles()
    {
        return new File[0];  // ignore
    }

    public void setSuiteXmlFiles( File[] suiteXmlFiles )
    {
        // ignore
    }

    public String getJunitArtifactName()
    {
        return null;  // ignore
    }

    public void setJunitArtifactName( String junitArtifactName )
    {
        // ignore
    }

    public String getTestNGArtifactName()
    {
        return null;  // ignore
    }

    public void setTestNGArtifactName( String testNGArtifactName )
    {
        // ignore
    }

    public int getThreadCount()
    {
        return 0;  // ignore
    }

    public void setThreadCount( int threadCount )
    {
        // ignore
    }

    public String getPerCoreThreadCount()
    {
        return null;  // ignore
    }

    public void setPerCoreThreadCount( String perCoreThreadCount )
    {
        // ignore
    }

    public String getUseUnlimitedThreads()
    {
        return null;  // ignore
    }

    public void setUseUnlimitedThreads( String useUnlimitedThreads )
    {
        // ignore
    }

    public String getParallel()
    {
        return null;  // ignore
    }

    public void setParallel( String parallel )
    {
        // ignore
    }

    public boolean isTrimStackTrace()
    {
        return false;  // ignore
    }

    public void setTrimStackTrace( boolean trimStackTrace )
    {
        // ignore
    }

    public ArtifactResolver getArtifactResolver()
    {
        return null;  // ignore
    }

    public void setArtifactResolver( ArtifactResolver artifactResolver )
    {
        // ignore
    }

    public ArtifactFactory getArtifactFactory()
    {
        return null;  // ignore
    }

    public void setArtifactFactory( ArtifactFactory artifactFactory )
    {
        // ignore
    }

    public List getRemoteRepositories()
    {
        return null;  // ignore
    }

    public void setRemoteRepositories( List remoteRepositories )
    {
        // ignore
    }

    public ArtifactMetadataSource getMetadataSource()
    {
        return null;  // ignore
    }

    public void setMetadataSource( ArtifactMetadataSource metadataSource )
    {
        // ignore
    }

    public Properties getOriginalSystemProperties()
    {
        return null;  // ignore
    }

    public void setOriginalSystemProperties( Properties originalSystemProperties )
    {
        // ignore
    }

    public Properties getInternalSystemProperties()
    {
        return null;  // ignore
    }

    public void setInternalSystemProperties( Properties internalSystemProperties )
    {
        // ignore
    }

    public boolean isDisableXmlReport()
    {
        return false;  // ignore
    }

    public void setDisableXmlReport( boolean disableXmlReport )
    {
        // ignore
    }

    public Boolean getUseSystemClassLoader()
    {
        return null;  // ignore
    }

    public void setUseSystemClassLoader( Boolean useSystemClassLoader )
    {
        // ignore
    }

    public boolean isUseManifestOnlyJar()
    {
        return false;  // ignore
    }

    public void setUseManifestOnlyJar( boolean useManifestOnlyJar )
    {
        // ignore
    }

    public boolean isEnableAssertions()
    {
        return false;  // ignore
    }

    public void setEnableAssertions( boolean enableAssertions )
    {
        // ignore
    }

    public MavenSession getSession()
    {
        return null;  // ignore
    }

    public void setSession( MavenSession session )
    {
        // ignore
    }

    public String getObjectFactory()
    {
        return null;  // ignore
    }

    public void setObjectFactory( String objectFactory )
    {
        // ignore
    }

    public ToolchainManager getToolchainManager()
    {
        return null;  // ignore
    }

    public void setToolchainManager( ToolchainManager toolchainManager )
    {
        // ignore
    }
}
