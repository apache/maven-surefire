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
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.util.DefaultScanResult;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.fest.assertions.Assertions.assertThat;

public class MojoMocklessTest
{
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

    private final static class Mojo
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
        public File getClassesDirectory()
        {
            return null;
        }

        @Override
        public void setClassesDirectory( File classesDirectory )
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
        public Boolean getFailIfNoSpecifiedTests()
        {
            return null;
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
        protected Artifact getMojoArtifact()
        {
            return null;
        }

        @Override
        List<Artifact> getProjectTestArtifacts()
        {
            return projectTestArtifacts;
        }
    }
}
