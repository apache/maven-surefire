package org.apache.maven.plugin.surefire;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.surefire.extensions.SurefireConsoleOutputReporter;
import org.apache.maven.plugin.surefire.extensions.SurefireStatelessReporter;
import org.apache.maven.plugin.surefire.extensions.SurefireStatelessTestsetInfoReporter;
import org.apache.maven.surefire.extensions.ForkNodeFactory;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.api.util.DefaultScanResult;
import org.apache.maven.toolchain.Toolchain;
import org.codehaus.plexus.logging.Logger;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.powermock.reflect.Whitebox.invokeMethod;
import static org.powermock.reflect.Whitebox.setInternalState;

/**
 *
 */
public class MojoMocklessTest
{
    @Test
    public void testGetStartupReportConfiguration() throws Exception
    {
        AbstractSurefireMojo surefirePlugin = new Mojo( null, null );
        StartupReportConfiguration config = invokeMethod( surefirePlugin, "getStartupReportConfiguration", "", false );

        assertThat( config.getXmlReporter() )
                .isNotNull()
                .isInstanceOf( SurefireStatelessReporter.class );

        assertThat( config.getConsoleOutputReporter() )
                .isNotNull()
                .isInstanceOf( SurefireConsoleOutputReporter.class );

        assertThat( config.getTestsetReporter() )
                .isNotNull()
                .isInstanceOf( SurefireStatelessTestsetInfoReporter.class );
    }

    @Test
    public void testGetStartupReportConfiguration2() throws Exception
    {
        AbstractSurefireMojo surefirePlugin = new Mojo( null, null );
        SurefireStatelessReporter xmlReporter = new SurefireStatelessReporter( false, "3.0" );
        SurefireConsoleOutputReporter consoleReporter = new SurefireConsoleOutputReporter();
        SurefireStatelessTestsetInfoReporter testsetInfoReporter = new SurefireStatelessTestsetInfoReporter();
        setInternalState( surefirePlugin, "statelessTestsetReporter", xmlReporter );
        setInternalState( surefirePlugin, "consoleOutputReporter", consoleReporter );
        setInternalState( surefirePlugin, "statelessTestsetInfoReporter", testsetInfoReporter );

        StartupReportConfiguration config = invokeMethod( surefirePlugin, "getStartupReportConfiguration", "", false );

        assertThat( config.getXmlReporter() )
                .isNotNull()
                .isSameAs( xmlReporter );

        assertThat( config.getConsoleOutputReporter() )
                .isNotNull()
                .isSameAs( consoleReporter );

        assertThat( config.getTestsetReporter() )
                .isNotNull()
                .isSameAs( testsetInfoReporter );
    }

    @Test
    public void testForkMode()
    {
        AbstractSurefireMojo surefirePlugin = new Mojo( null, null );
        setInternalState( surefirePlugin, "toolchain", new MyToolChain() );
        setInternalState( surefirePlugin, "forkMode", "never" );
        assertThat( surefirePlugin.getEffectiveForkMode() )
                .isEqualTo( "once" );
    }

    @Test
    @SuppressWarnings( "checkstyle:magicnumber" )
    public void testForkCountComputation()
    {
        AbstractSurefireMojo surefirePlugin = new Mojo( null, null );
        assertConversionFails( surefirePlugin, "nothing" );

        assertConversionFails( surefirePlugin, "5,0" );
        assertConversionFails( surefirePlugin, "5.0" );
        assertConversionFails( surefirePlugin, "5,0C" );
        assertConversionFails( surefirePlugin, "5.0CC" );

        assertForkCount( surefirePlugin, 5, "5" );

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        assertForkCount( surefirePlugin, 3 * availableProcessors, "3C" );
        assertForkCount( surefirePlugin, (int) ( 2.5 * availableProcessors ), "2.5C" );
        assertForkCount( surefirePlugin, availableProcessors, "1.0001 C" );
        assertForkCount( surefirePlugin, 1, 1d / ( (double) availableProcessors + 1 ) + "C" );
        assertForkCount( surefirePlugin, 0, "0 C" );
    }

    private static void assertForkCount( AbstractSurefireMojo surefirePlugin, int expected, String value )
    {
        assertThat( surefirePlugin.convertWithCoreCount( value ) )
                .isEqualTo( expected );
    }

    private static void assertConversionFails( AbstractSurefireMojo surefirePlugin, String value )
    {
        try
        {
            surefirePlugin.convertWithCoreCount( value );
        }
        catch ( NumberFormatException e )
        {
            return;
        }
        fail( "Expected NumberFormatException when converting " + value );
    }

    private static class MyToolChain implements Toolchain
    {
        @Override
        public String getType()
        {
            return null;
        }

        @Override
        public String findTool( String s )
        {
            return null;
        }
    }

    @Test
    public void scanDependenciesShouldReturnNull()
            throws MojoFailureException
    {
        Mojo mojo = new Mojo( null, null );
        DefaultScanResult result = mojo.scanDependencies();
        assertThat( result )
                .isNull();
    }

    @Test
    public void scanDependenciesShouldReturnNullAfterMissingBuildArtifact()
            throws MojoFailureException
    {
        VersionRange version = VersionRange.createFromVersion( "1.0" );
        ArtifactHandler handler = new DefaultArtifactHandler();
        Artifact testDeps = new DefaultArtifact( "g", "a", version, "compile", "jar", null, handler );
        List<Artifact> projectTestArtifacts = singletonList( testDeps );
        String[] dependenciesToScan = { "g:a" };
        Mojo mojo = new Mojo( projectTestArtifacts, dependenciesToScan );
        DefaultScanResult result = mojo.scanDependencies();
        assertThat( result )
                .isNull();
    }

    @Test
    public void scanDependenciesShouldReturnNullWithWAR()
            throws MojoFailureException
    {
        VersionRange version = VersionRange.createFromVersion( "1.0" );
        ArtifactHandler handler = new DefaultArtifactHandler();
        Artifact testDeps = new DefaultArtifact( "g", "a", version, "compile", "war", null, handler );
        testDeps.setFile( new File( new File( "target" ), "a-1.0.war" ) );
        List<Artifact> projectTestArtifacts = singletonList( testDeps );
        String[] dependenciesToScan = { "g:a" };
        Mojo mojo = new Mojo( projectTestArtifacts, dependenciesToScan );
        DefaultScanResult result = mojo.scanDependencies();
        assertThat( result )
                .isNull();
    }

    @Test
    public void scanDependenciesShouldReturnNullWithExistingWAR()
            throws Exception
    {
        VersionRange version = VersionRange.createFromVersion( "1.0" );
        ArtifactHandler handler = new DefaultArtifactHandler();
        Artifact testDeps = new DefaultArtifact( "g", "a", version, "compile", "war", null, handler );
        File artifactFile = File.createTempFile( "surefire", ".war" );
        testDeps.setFile( artifactFile );
        List<Artifact> projectTestArtifacts = singletonList( testDeps );
        String[] dependenciesToScan = { "g:a" };
        Mojo mojo = new Mojo( projectTestArtifacts, dependenciesToScan );
        DefaultScanResult result = mojo.scanDependencies();
        assertThat( result )
                .isNull();
    }

    @Test
    public void scanDependenciesShouldReturnClassWithExistingTestJAR()
            throws Exception
    {
        VersionRange version = VersionRange.createFromVersion( "1.0" );
        ArtifactHandler handler = new DefaultArtifactHandler();
        Artifact testDeps = new DefaultArtifact( "g", "a", version, "compile", "test-jar", null, handler );

        File artifactFile = File.createTempFile( "surefire", ".jar" );
        testDeps.setFile( artifactFile );
        try ( ZipOutputStream os = new ZipOutputStream( new FileOutputStream( artifactFile ) ) )
        {
            os.putNextEntry( new ZipEntry( "pkg/" ) );
            os.closeEntry();
            os.putNextEntry( new ZipEntry( "pkg/MyTest.class" ) );
            os.closeEntry();
            os.finish();
        }

        List<Artifact> projectTestArtifacts = singletonList( testDeps );
        String[] dependenciesToScan = { "g:a" };
        Mojo mojo = new Mojo( projectTestArtifacts, dependenciesToScan );
        mojo.setLogger( mock( Logger.class ) );
        DefaultScanResult result = mojo.scanDependencies();

        assertThat( result )
                .isNotNull();

        assertThat( result.isEmpty() )
                .isFalse();

        assertThat( result.getClasses() )
                .contains( "pkg.MyTest" );
    }

    @Test
    public void scanDependenciesShouldReturnNullWithEmptyTestJAR()
            throws Exception
    {
        VersionRange version = VersionRange.createFromVersion( "1.0" );
        ArtifactHandler handler = new DefaultArtifactHandler();
        Artifact testDeps = new DefaultArtifact( "g", "a", version, "compile", "jar", null, handler );

        File artifactFile = File.createTempFile( "surefire", ".jar" );
        testDeps.setFile( artifactFile );
        try ( ZipOutputStream os = new ZipOutputStream( new FileOutputStream( artifactFile ) ) )
        {
            os.putNextEntry( new ZipEntry( "pkg/" ) );
            os.closeEntry();
            os.finish();
        }

        List<Artifact> projectTestArtifacts = singletonList( testDeps );
        String[] dependenciesToScan = { "g:a" };
        Mojo mojo = new Mojo( projectTestArtifacts, dependenciesToScan );
        mojo.setLogger( mock( Logger.class ) );
        DefaultScanResult result = mojo.scanDependencies();

        assertThat( result )
                .isNotNull();

        assertThat( result.isEmpty() )
                .isTrue();
    }

    @Test
    public void scanDependenciesShouldReturnClassWithDirectory()
            throws Exception
    {
        VersionRange version = VersionRange.createFromVersion( "1.0" );
        ArtifactHandler handler = new DefaultArtifactHandler();
        Artifact testDeps = new DefaultArtifact( "g", "a", version, "compile", "test-jar", null, handler );

        File artifactFile = File.createTempFile( "surefire", "-classes" );
        String classDir = artifactFile.getCanonicalPath();
        assertThat( artifactFile.delete() ).isTrue();
        File classes = new File( classDir );
        assertThat( classes.mkdir() ).isTrue();

        testDeps.setFile( classes );

        assertThat( new File( classes, "AnotherTest.class" ).createNewFile() )
                .isTrue();

        List<Artifact> projectTestArtifacts = singletonList( testDeps );
        String[] dependenciesToScan = { "g:a" };
        Mojo mojo = new Mojo( projectTestArtifacts, dependenciesToScan );
        mojo.setLogger( mock( Logger.class ) );
        DefaultScanResult result = mojo.scanDependencies();

        assertThat( result )
                .isNotNull();

        assertThat( result.isEmpty() )
                .isFalse();

        assertThat( result.getClasses() )
                .contains( "AnotherTest" );
    }

    @Test
    public void scanMultipleDependencies()
            throws Exception
    {
        VersionRange version = VersionRange.createFromVersion( "1.0" );
        ArtifactHandler handler = new DefaultArtifactHandler();
        Artifact testDep1 = new DefaultArtifact( "g", "x", version, "compile", "jar", null, handler );

        File artifactFile1 = File.createTempFile( "surefire", "-classes" );
        String classDir = artifactFile1.getCanonicalPath();
        assertThat( artifactFile1.delete() ).isTrue();
        File classes = new File( classDir );
        assertThat( classes.mkdir() ).isTrue();

        testDep1.setFile( classes );

        assertThat( new File( classes, "AnotherTest.class" ).createNewFile() )
                .isTrue();

        Artifact testDep2 = new DefaultArtifact( "g", "a", version, "test", "jar", null, handler );
        File artifactFile2 = File.createTempFile( "surefire", ".jar" );
        testDep2.setFile( artifactFile2 );
        try ( ZipOutputStream os = new ZipOutputStream( new FileOutputStream( artifactFile2 ) ) )
        {
            os.putNextEntry( new ZipEntry( "pkg/" ) );
            os.closeEntry();
            os.putNextEntry( new ZipEntry( "pkg/MyTest.class" ) );
            os.closeEntry();
            os.finish();
        }

        List<Artifact> projectTestArtifacts = asList( testDep1, testDep2 );
        String[] dependenciesToScan = { "g:a" };
        Mojo mojo = new Mojo( projectTestArtifacts, dependenciesToScan );
        mojo.setLogger( mock( Logger.class ) );
        DefaultScanResult result = mojo.scanDependencies();

        assertThat( result )
                .isNotNull();

        assertThat( result.isEmpty() )
                .isFalse();

        assertThat( result.getClasses() )
                .hasSize( 1 );

        assertThat( result.getClasses() )
                .contains( "pkg.MyTest" );
    }

    private static final class Mojo
            extends AbstractSurefireMojo
    {
        private final List<Artifact> projectTestArtifacts;
        private final String[] dependenciesToScan;

        Mojo( List<Artifact> projectTestArtifacts, String[] dependenciesToScan )
        {
            this.projectTestArtifacts = projectTestArtifacts;
            this.dependenciesToScan = dependenciesToScan;
        }

        @Override
        protected String getPluginName()
        {
            return null;
        }

        @Override
        protected int getRerunFailingTestsCount()
        {
            return 0;
        }

        @Override
        public boolean isSkipTests()
        {
            return false;
        }

        @Override
        public void setSkipTests( boolean skipTests )
        {

        }

        @Override
        public boolean isSkipExec()
        {
            return false;
        }

        @Override
        public void setSkipExec( boolean skipExec )
        {

        }

        @Override
        public boolean isSkip()
        {
            return false;
        }

        @Override
        public void setSkip( boolean skip )
        {

        }

        @Override
        public File getBasedir()
        {
            return null;
        }

        @Override
        public void setBasedir( File basedir )
        {

        }

        @Override
        public File getTestClassesDirectory()
        {
            return null;
        }

        @Override
        public void setTestClassesDirectory( File testClassesDirectory )
        {

        }

        @Override
        public File getMainBuildPath()
        {
            return null;
        }

        @Override
        public void setMainBuildPath( File mainBuildPath )
        {

        }

        @Override
        public File getReportsDirectory()
        {
            return null;
        }

        @Override
        public void setReportsDirectory( File reportsDirectory )
        {

        }

        @Override
        public String getTest()
        {
            return null;
        }

        @Override
        public void setTest( String test )
        {

        }

        @Override
        public List<String> getIncludes()
        {
            return null;
        }

        @Override
        public File getIncludesFile()
        {
            return null;
        }

        @Override
        public void setIncludes( List<String> includes )
        {

        }

        @Override
        public List<String> getExcludes()
        {
            return null;
        }

        @Override
        public void setExcludes( List<String> excludes )
        {

        }

        @Override
        public boolean isPrintSummary()
        {
            return false;
        }

        @Override
        public void setPrintSummary( boolean printSummary )
        {

        }

        @Override
        public String getReportFormat()
        {
            return null;
        }

        @Override
        public void setReportFormat( String reportFormat )
        {

        }

        @Override
        public boolean isUseFile()
        {
            return false;
        }

        @Override
        public void setUseFile( boolean useFile )
        {

        }

        @Override
        public String getDebugForkedProcess()
        {
            return null;
        }

        @Override
        public void setDebugForkedProcess( String debugForkedProcess )
        {

        }

        @Override
        public int getForkedProcessTimeoutInSeconds()
        {
            return 0;
        }

        @Override
        public void setForkedProcessTimeoutInSeconds( int forkedProcessTimeoutInSeconds )
        {

        }

        @Override
        public int getForkedProcessExitTimeoutInSeconds()
        {
            return 0;
        }

        @Override
        public void setForkedProcessExitTimeoutInSeconds( int forkedProcessTerminationTimeoutInSeconds )
        {

        }

        @Override
        public double getParallelTestsTimeoutInSeconds()
        {
            return 0;
        }

        @Override
        public void setParallelTestsTimeoutInSeconds( double parallelTestsTimeoutInSeconds )
        {

        }

        @Override
        public double getParallelTestsTimeoutForcedInSeconds()
        {
            return 0;
        }

        @Override
        public void setParallelTestsTimeoutForcedInSeconds( double parallelTestsTimeoutForcedInSeconds )
        {

        }

        @Override
        public boolean isUseSystemClassLoader()
        {
            return false;
        }

        @Override
        public void setUseSystemClassLoader( boolean useSystemClassLoader )
        {

        }

        @Override
        public boolean isUseManifestOnlyJar()
        {
            return false;
        }

        @Override
        public void setUseManifestOnlyJar( boolean useManifestOnlyJar )
        {

        }

        @Override
        public String getEncoding()
        {
            return null;
        }

        @Override
        public void setEncoding( String encoding )
        {

        }

        @Override
        public boolean getFailIfNoSpecifiedTests()
        {
            return false;
        }

        @Override
        public void setFailIfNoSpecifiedTests( boolean failIfNoSpecifiedTests )
        {

        }

        @Override
        public int getSkipAfterFailureCount()
        {
            return 0;
        }

        @Override
        public String getShutdown()
        {
            return null;
        }

        @Override
        public File getExcludesFile()
        {
            return null;
        }

        @Override
        protected List<File> suiteXmlFiles()
        {
            return null;
        }

        @Override
        protected boolean hasSuiteXmlFiles()
        {
            return false;
        }

        @Override
        protected String[] getExcludedEnvironmentVariables()
        {
            return new String[0];
        }

        @Override
        public File[] getSuiteXmlFiles()
        {
            return new File[0];
        }

        @Override
        public void setSuiteXmlFiles( File[] suiteXmlFiles )
        {

        }

        @Override
        public String getRunOrder()
        {
            return null;
        }

        @Override
        public void setRunOrder( String runOrder )
        {

        }

        @Override
        public Long getRunOrderRandomSeed()
        {
            return null;
        }

        @Override
        public void setRunOrderRandomSeed( Long runOrderRandomSeed )
        {

        }

        @Override
        public String[] getDependenciesToScan()
        {
            return dependenciesToScan;
        }

        @Override
        protected void handleSummary( RunResult summary, Exception firstForkException )
        {

        }

        @Override
        protected boolean isSkipExecution()
        {
            return false;
        }

        @Override
        protected String[] getDefaultIncludes()
        {
            return new String[0];
        }

        @Override
        protected String getReportSchemaLocation()
        {
            return null;
        }

        @Override
        protected boolean useModulePath()
        {
            return false;
        }

        @Override
        protected void setUseModulePath( boolean useModulePath )
        {

        }

        @Override
        protected ForkNodeFactory getForkNode()
        {
            return null;
        }

        @Override
        protected String getEnableProcessChecker()
        {
            return null;
        }

        @Override
        protected Artifact getMojoArtifact()
        {
            return null;
        }

        @Override
        List<Artifact> getProjectTestArtifacts()
        {
            return projectTestArtifacts;
        }

        @Override
        public File getSystemPropertiesFile()
        {
            return null;
        }

        @Override
        public void setSystemPropertiesFile( File systemPropertiesFile )
        {

        }

        @Override
        public String[] getIncludeJUnit5Engines()
        {
            return null;
        }

        @Override
        public void setIncludeJUnit5Engines( String[] includeJUnit5Engines )
        {

        }

        @Override
        public String[] getExcludeJUnit5Engines()
        {
            return null;
        }

        @Override
        public void setExcludeJUnit5Engines( String[] excludeJUnit5Engines )
        {

        }
    }
}
