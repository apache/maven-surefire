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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.ToolchainManager;

/**
 * The parameters required to execute surefire.
 *
 * @author Stephen Connolly
 * @noinspection UnusedDeclaration, UnusedDeclaration
 */
public interface SurefireExecutionParameters
{
    boolean isSkipTests();

    void setSkipTests( boolean skipTests );

    boolean isSkipExec();

    void setSkipExec( boolean skipExec );

    boolean isSkip();

    void setSkip( boolean skip );

    File getBasedir();

    void setBasedir( File basedir );

    File getTestClassesDirectory();

    void setTestClassesDirectory( File testClassesDirectory );

    File getClassesDirectory();

    void setClassesDirectory( File classesDirectory );

    MavenProject getProject();

    void setProject( MavenProject project );

    List<String> getClasspathDependencyExcludes();

    void setClasspathDependencyExcludes( List<String> classpathDependencyExcludes );

    String getClasspathDependencyScopeExclude();

    void setClasspathDependencyScopeExclude( String classpathDependencyScopeExclude );

    List<String> getAdditionalClasspathElements();

    void setAdditionalClasspathElements( List<String> additionalClasspathElements );

    File getReportsDirectory();

    void setReportsDirectory( File reportsDirectory );

    File getTestSourceDirectory();

    void setTestSourceDirectory( File testSourceDirectory );

    String getTest();

    String getTestMethod();

    void setTest( String test );

    List<String> getIncludes();

    void setIncludes( List<String> includes );

    List<String> getExcludes();

    void setExcludes( List<String> excludes );

    ArtifactRepository getLocalRepository();

    void setLocalRepository( ArtifactRepository localRepository );

    Properties getSystemProperties();

    void setSystemProperties( Properties systemProperties );

    Map<String, String> getSystemPropertyVariables();

    void setSystemPropertyVariables( Map<String, String> systemPropertyVariables );

    File getSystemPropertiesFile();

    void setSystemPropertiesFile( File systemPropertiesFile );

    Properties getProperties();

    void setProperties( Properties properties );

    PluginDescriptor getPluginDescriptor();

    Map<String, Artifact> getPluginArtifactMap();

    void setPluginArtifactMap( Map<String, Artifact> pluginArtifactMap );

    Map<String, Artifact> getProjectArtifactMap();

    void setProjectArtifactMap( Map<String, Artifact> projectArtifactMap );

    boolean isPrintSummary();

    void setPrintSummary( boolean printSummary );

    String getReportFormat();

    void setReportFormat( String reportFormat );

    String getReportNameSuffix();

    void setReportNameSuffix( String reportNameSuffix );

    boolean isUseFile();

    void setUseFile( boolean useFile );

    boolean isRedirectTestOutputToFile();

    void setRedirectTestOutputToFile( boolean redirectTestOutputToFile );

    String getForkMode();

    void setForkMode( String forkMode );

    String getJvm();

    void setJvm( String jvm );

    String getArgLine();

    void setArgLine( String argLine );

    String getDebugForkedProcess();

    void setDebugForkedProcess( String debugForkedProcess );

    int getForkedProcessTimeoutInSeconds();

    void setForkedProcessTimeoutInSeconds( int forkedProcessTimeoutInSeconds );

    Map<String, String> getEnvironmentVariables();

    void setEnvironmentVariables( Map<String, String> environmentVariables );

    File getWorkingDirectory();

    void setWorkingDirectory( File workingDirectory );

    boolean isChildDelegation();

    void setChildDelegation( boolean childDelegation );

    String getGroups();

    void setGroups( String groups );

    String getExcludedGroups();

    void setExcludedGroups( String excludedGroups );

    File[] getSuiteXmlFiles();

    void setSuiteXmlFiles( File[] suiteXmlFiles );

    String getJunitArtifactName();

    void setJunitArtifactName( String junitArtifactName );

    String getTestNGArtifactName();

    void setTestNGArtifactName( String testNGArtifactName );

    int getThreadCount();

    void setThreadCount( int threadCount );

    boolean getPerCoreThreadCount();

    void setPerCoreThreadCount( boolean perCoreThreadCount );

    boolean getUseUnlimitedThreads();

    void setUseUnlimitedThreads( boolean useUnlimitedThreads );

    String getParallel();

    void setParallel( String parallel );

    boolean isTrimStackTrace();

    void setTrimStackTrace( boolean trimStackTrace );

    ArtifactResolver getArtifactResolver();

    void setArtifactResolver( ArtifactResolver artifactResolver );

    ArtifactFactory getArtifactFactory();

    void setArtifactFactory( ArtifactFactory artifactFactory );

    List<ArtifactRepository> getRemoteRepositories();

    void setRemoteRepositories( List<ArtifactRepository> remoteRepositories );

    ArtifactMetadataSource getMetadataSource();

    void setMetadataSource( ArtifactMetadataSource metadataSource );

    Properties getOriginalSystemProperties();

    void setOriginalSystemProperties( Properties originalSystemProperties );

    Properties getInternalSystemProperties();

    void setInternalSystemProperties( Properties internalSystemProperties );

    boolean isDisableXmlReport();

    void setDisableXmlReport( boolean disableXmlReport );

    boolean isUseSystemClassLoader();

    void setUseSystemClassLoader( boolean useSystemClassLoader );

    boolean isUseManifestOnlyJar();

    void setUseManifestOnlyJar( boolean useManifestOnlyJar );

    boolean isEnableAssertions();

    void setEnableAssertions( boolean enableAssertions );

    MavenSession getSession();

    void setSession( MavenSession session );

    String getObjectFactory();

    void setObjectFactory( String objectFactory );

    ToolchainManager getToolchainManager();

    void setToolchainManager( ToolchainManager toolchainManager );

    Boolean getFailIfNoSpecifiedTests();

    void setFailIfNoSpecifiedTests( Boolean failIfNoSpecifiedTests );

    Boolean getFailIfNoTests();

    void setFailIfNoTests( Boolean failIfNoTests );

    boolean isMavenParallel();

    void setRunOrder( String runOrder );

    String getRunOrder();

}
