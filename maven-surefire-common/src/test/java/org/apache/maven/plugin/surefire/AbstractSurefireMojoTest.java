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
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.surefire.log.PluginConsoleLogger;
import org.apache.maven.project.MavenProject;
import org.apache.maven.surefire.booter.ClassLoaderConfiguration;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.suite.RunResult;
import org.codehaus.plexus.logging.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.io.File.separatorChar;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.apache.maven.artifact.versioning.VersionRange.createFromVersion;
import static org.apache.maven.artifact.versioning.VersionRange.createFromVersionSpec;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;
import static org.powermock.reflect.Whitebox.invokeMethod;

/**
 * Test for {@link AbstractSurefireMojo}.
 */
@RunWith( PowerMockRunner.class )
@PrepareForTest( AbstractSurefireMojo.class )
public class AbstractSurefireMojoTest
{
    @Mock
    private ArtifactHandler handler;

    private final Mojo mojo = new Mojo();

    @Test
    public void shouldRetainInPluginArtifacts() throws Exception
    {
        Artifact provider = new DefaultArtifact( "g", "a", createFromVersionSpec( "1" ), "compile", "jar", "", null );
        Artifact common = new DefaultArtifact( "g", "c", createFromVersionSpec( "1" ), "compile", "jar", "", null );
        Artifact api = new DefaultArtifact( "g", "a", createFromVersionSpec( "1" ), "compile", "jar", "", null );

        Set<Artifact> providerArtifacts = singleton( provider );
        Artifact[] inPluginArtifacts = { common, api };
        Set<Artifact> inPluginClasspath = invokeMethod( AbstractSurefireMojo.class,
                "retainInProcArtifactsUnique", providerArtifacts, inPluginArtifacts );

        assertThat( inPluginClasspath )
                .containsOnly( common );
    }

    @Test
    public void shouldRetainInProcArtifactsUnique() throws Exception
    {
        Artifact provider = new DefaultArtifact( "g", "p", createFromVersionSpec( "1" ), "compile", "jar", "", null );
        Artifact common = new DefaultArtifact( "g", "c", createFromVersionSpec( "1" ), "compile", "jar", "", null );
        Artifact api = new DefaultArtifact( "g", "a", createFromVersionSpec( "1" ), "compile", "jar", "", null );

        Set<Artifact> providerArtifacts = singleton( provider );
        Artifact[] inPluginArtifacts = { common, api };
        Set<Artifact> inPluginClasspath = invokeMethod( AbstractSurefireMojo.class,
                "retainInProcArtifactsUnique", providerArtifacts, inPluginArtifacts );

        assertThat( inPluginClasspath )
                .containsOnly( common, api );
    }

    @Test
    public void shouldCreateInProcClasspath() throws Exception
    {
        Artifact provider = new DefaultArtifact( "g", "p", createFromVersionSpec( "1" ), "compile", "jar", "", null );
        provider.setFile( mockFile( "provider.jar" ) );

        Artifact common = new DefaultArtifact( "g", "c", createFromVersionSpec( "1" ), "compile", "jar", "", null );
        common.setFile( mockFile( "maven-surefire-common.jar" ) );

        Artifact api = new DefaultArtifact( "g", "a", createFromVersionSpec( "1" ), "compile", "jar", "", null );
        api.setFile( mockFile( "surefire-api.jar" ) );

        Set<Artifact> newArtifacts = new LinkedHashSet<>();
        newArtifacts.add( common );
        newArtifacts.add( api );

        Classpath providerClasspath = new Classpath( singleton( provider.getFile().getAbsolutePath() ) );

        Classpath inPluginClasspath = invokeMethod( AbstractSurefireMojo.class,
                "createInProcClasspath", providerClasspath, newArtifacts );

        Classpath expectedClasspath =
                new Classpath( asList( provider.getFile().getAbsolutePath(),
                                       common.getFile().getAbsolutePath(),
                                       api.getFile().getAbsolutePath() ) );

        assertThat( (Object ) inPluginClasspath )
                .isEqualTo( expectedClasspath );

    }

    @Test
    public void shouldGenerateTestClasspath() throws Exception
    {
        AbstractSurefireMojo mojo = spy( this.mojo );

        when( mojo.getClassesDirectory() ).thenReturn( new File( "target" + separatorChar + "classes" ) );
        when( mojo.getTestClassesDirectory() ).thenReturn( new File( "target" + separatorChar + "test-classes" ) );
        when( mojo.getClasspathDependencyScopeExclude() ).thenReturn( "runtime" );
        when( mojo.getClasspathDependencyExcludes() ).thenReturn( new String[]{ "g3:a3" } );
        doReturn( mock( Artifact.class ) ).when( mojo, "getTestNgArtifact" );

        Set<Artifact> artifacts = new HashSet<>();

        Artifact a1 = mock( Artifact.class );
        when( a1.getGroupId() ).thenReturn( "g1" );
        when( a1.getArtifactId() ).thenReturn( "a1" );
        when( a1.getVersion() ).thenReturn( "1" );
        when( a1.getScope() ).thenReturn( "runtime" );
        when( a1.getDependencyConflictId() ).thenReturn( "g1:a1:jar" );
        when( a1.getId() ).thenReturn( "g1:a1:jar:1" );
        artifacts.add( a1 );

        ArtifactHandler artifactHandler = mock( ArtifactHandler.class );
        when( artifactHandler.isAddedToClasspath() ).thenReturn( true );

        Artifact a2 = mock( Artifact.class );
        when( a2.getGroupId() ).thenReturn( "g2" );
        when( a2.getArtifactId() ).thenReturn( "a2" );
        when( a2.getVersion() ).thenReturn( "2" );
        when( a2.getScope() ).thenReturn( "test" );
        when( a2.getDependencyConflictId() ).thenReturn( "g2:a2:jar" );
        when( a2.getId() ).thenReturn( "g2:a2:jar:2" );
        when( a2.getFile() ).thenReturn( new File( "a2-2.jar" ) );
        when( a2.getArtifactHandler() ).thenReturn( artifactHandler );
        artifacts.add( a2 );

        Artifact a3 = mock( Artifact.class );
        when( a3.getGroupId() ).thenReturn( "g3" );
        when( a3.getArtifactId() ).thenReturn( "a3" );
        when( a3.getVersion() ).thenReturn( "3" );
        when( a3.getScope() ).thenReturn( "test" );
        when( a3.getDependencyConflictId() ).thenReturn( "g3:a3:jar" );
        when( a3.getId() ).thenReturn( "g3:a3:jar:3" );
        when( a3.getFile() ).thenReturn( new File( "a3-3.jar" ) );
        when( a3.getArtifactHandler() ).thenReturn( artifactHandler );
        artifacts.add( a3 );

        MavenProject project = mock( MavenProject.class );
        when( project.getArtifacts() ).thenReturn( artifacts );
        when( mojo.getProject() ).thenReturn( project );

        TestClassPath cp = invokeMethod( mojo, "generateTestClasspath" );

        verifyPrivate( mojo, times( 1 ) ).invoke( "generateTestClasspath" );
        verify( mojo, times( 1 ) ).getClassesDirectory();
        verify( mojo, times( 1 ) ).getTestClassesDirectory();
        verify( mojo, times( 3 ) ).getClasspathDependencyScopeExclude();
        verify( mojo, times( 2 ) ).getClasspathDependencyExcludes();
        verify( mojo, times( 1 ) ).getAdditionalClasspathElements();

        assertThat( cp.toClasspath().getClassPath() ).hasSize( 3 );
        assertThat( cp.toClasspath().getClassPath().get( 0 ) ).endsWith( "test-classes" );
        assertThat( cp.toClasspath().getClassPath().get( 1 ) ).endsWith( "classes" );
        assertThat( cp.toClasspath().getClassPath().get( 2 ) ).endsWith( "a2-2.jar" );
    }

    @Test
    public void shouldHaveStartupConfigForNonModularClasspath()
            throws Exception
    {
        AbstractSurefireMojo mojo = spy( this.mojo );

        Artifact common = new DefaultArtifact( "org.apache.maven.surefire", "maven-surefire-common",
                createFromVersion( "1" ), "runtime", "jar", "", handler );
        common.setFile( mockFile( "maven-surefire-common.jar" ) );

        Artifact api = new DefaultArtifact( "org.apache.maven.surefire", "surefire-api",
                createFromVersion( "1" ), "runtime", "jar", "", handler );
        api.setFile( mockFile( "surefire-api.jar" ) );

        Artifact loggerApi = new DefaultArtifact( "org.apache.maven.surefire", "surefire-logger-api",
                createFromVersion( "1" ), "runtime", "jar", "", handler );
        loggerApi.setFile( mockFile( "surefire-logger-api.jar" ) );

        Map<String, Artifact> providerArtifactsMap = new HashMap<>();
        providerArtifactsMap.put( "org.apache.maven.surefire:maven-surefire-common", common );
        providerArtifactsMap.put( "org.apache.maven.surefire:surefire-api", api );
        providerArtifactsMap.put( "org.apache.maven.surefire:surefire-logger-api", loggerApi );

        when( mojo.getPluginArtifactMap() )
                .thenReturn( providerArtifactsMap );

        when( handler.isAddedToClasspath() ).thenReturn( true );

        VersionRange v1 = createFromVersion( "4.12" );
        Artifact junit = new DefaultArtifact( "junit", "junit", v1, "test", "jar", "", handler );
        junit.setFile( mockFile( "junit.jar" ) );

        VersionRange v2 = createFromVersion( "1.3.0" );
        Artifact hamcrest = new DefaultArtifact( "org.hamcrest", "hamcrest-core", v2, "test", "jar", "", handler );
        hamcrest.setFile( mockFile( "hamcrest.jar" ) );

        File classesDir = mockFile( "classes" );
        File testClassesDir = mockFile( "test-classes" );
        TestClassPath testClasspath =
                new TestClassPath( asList( junit, hamcrest ), classesDir, testClassesDir, null, null );

        doReturn( testClasspath ).when( mojo, "generateTestClasspath" );
        doReturn( 1 ).when( mojo, "getEffectiveForkCount" );
        doReturn( true ).when( mojo, "effectiveIsEnableAssertions" );
        when( mojo.isChildDelegation() ).thenReturn( false );

        ClassLoaderConfiguration classLoaderConfiguration = new ClassLoaderConfiguration( false, true );

        VersionRange v3 = createFromVersion( "1" );
        Artifact provider = new DefaultArtifact( "x", "surefire-provider", v3, "runtime", "jar", "", handler );
        provider.setFile( mockFile( "surefire-provider.jar" ) );
        Set<Artifact> providerArtifacts = singleton( provider );

        Logger logger = mock( Logger.class );
        when( logger.isDebugEnabled() ).thenReturn( true );
        doNothing().when( logger ).debug( anyString() );
        when( mojo.getConsoleLogger() ).thenReturn( new PluginConsoleLogger( logger ) );

        StartupConfiguration conf = invokeMethod( mojo, "newStartupConfigWithClasspath",
                classLoaderConfiguration, providerArtifacts, "org.asf.Provider" );

        verify( mojo, times( 1 ) ).effectiveIsEnableAssertions();
        verify( mojo, times( 1 ) ).isChildDelegation();
        verifyPrivate( mojo, times( 1 ) ).invoke( "generateTestClasspath" );
        verify( mojo, times( 1 ) ).getEffectiveForkCount();
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass( String.class );
        verify( logger, times( 6 ) ).debug( argument.capture() );
        assertThat( argument.getAllValues() )
                .containsExactly( "test classpath:  test-classes  classes  junit.jar  hamcrest.jar",
                "provider classpath:  surefire-provider.jar",
                "test(compact) classpath:  test-classes  classes  junit.jar  hamcrest.jar",
                "provider(compact) classpath:  surefire-provider.jar",
                "in-process classpath:  surefire-provider.jar  maven-surefire-common.jar  surefire-api.jar  surefire-logger-api.jar",
                "in-process(compact) classpath:  surefire-provider.jar  maven-surefire-common.jar  surefire-api.jar  surefire-logger-api.jar"
                );

        assertThat( conf.getClassLoaderConfiguration() )
                .isSameAs( classLoaderConfiguration );

        assertThat( ( Object ) conf.getClasspathConfiguration().getTestClasspath() )
                .isEqualTo( testClasspath.toClasspath() );

        Collection<String> files = new ArrayList<>();
        for ( Artifact providerArtifact : providerArtifacts )
        {
            files.add( providerArtifact.getFile().getAbsolutePath() );
        }
        assertThat( ( Object ) conf.getClasspathConfiguration().getProviderClasspath() )
                .isEqualTo( new Classpath( files ) );

        assertThat( ( Object ) conf.getClasspathConfiguration().isClassPathConfig() )
                .isEqualTo( true );

        assertThat( ( Object ) conf.getClasspathConfiguration().isModularPathConfig() )
                .isEqualTo( false );

        assertThat( ( Object ) conf.getClasspathConfiguration().isEnableAssertions() )
                .isEqualTo( true );

        assertThat( conf.getProviderClassName() )
                .isEqualTo( "org.asf.Provider" );
    }

    @Test
    public void shouldExistTmpDirectory() throws IOException
    {
        String systemTmpDir = System.getProperty( "java.io.tmpdir" );
        String usrDir = new File( System.getProperty( "user.dir" ) ).getCanonicalPath();

        String tmpDir = "surefireX" + System.currentTimeMillis();

        //noinspection ResultOfMethodCallIgnored
        new File( systemTmpDir, tmpDir ).delete();

        File targetDir = new File( usrDir, "target" );
        //noinspection ResultOfMethodCallIgnored
        new File( targetDir, tmpDir ).delete();

        AbstractSurefireMojo mojo = mock( AbstractSurefireMojo.class );
        when( mojo.getTempDir() ).thenReturn( tmpDir );
        when( mojo.getProjectBuildDirectory() ).thenReturn( targetDir );
        when( mojo.createSurefireBootDirectoryInTemp() ).thenCallRealMethod();
        when( mojo.createSurefireBootDirectoryInBuild() ).thenCallRealMethod();
        when( mojo.getSurefireTempDir() ).thenCallRealMethod();

        File bootDir = mojo.createSurefireBootDirectoryInTemp();
        assertThat( bootDir ).isNotNull();
        assertThat( bootDir ).isDirectory();

        assertThat( new File( systemTmpDir, bootDir.getName() ) ).isDirectory();
        assertThat( bootDir.getName() )
                .startsWith( tmpDir );

        File buildTmp = mojo.createSurefireBootDirectoryInBuild();
        assertThat( buildTmp ).isNotNull();
        assertThat( buildTmp ).isDirectory();
        assertThat( buildTmp.getParentFile().getCanonicalFile().getParent() ).isEqualTo( usrDir );
        assertThat( buildTmp.getName() ).isEqualTo( tmpDir );

        File tmp = mojo.getSurefireTempDir();
        assertThat( tmp ).isNotNull();
        assertThat( tmp ).isDirectory();
        assertThat( IS_OS_WINDOWS ? new File( systemTmpDir, bootDir.getName() ) : new File( targetDir, tmpDir ) )
                .isDirectory();
    }

    public static class Mojo
            extends AbstractSurefireMojo
    {
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
    }

    private static File mockFile( String absolutePath )
    {
        File f = mock( File.class );
        when( f.getAbsolutePath() ).thenReturn( absolutePath );
        return f;
    }
}
