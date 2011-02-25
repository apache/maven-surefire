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

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.ToolchainManager;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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

    List getClasspathDependencyExcludes();

    void setClasspathDependencyExcludes( List classpathDependencyExcludes );

    String getClasspathDependencyScopeExclude();

    void setClasspathDependencyScopeExclude( String classpathDependencyScopeExclude );

    List getAdditionalClasspathElements();

    void setAdditionalClasspathElements( List additionalClasspathElements );

    File getReportsDirectory();

    void setReportsDirectory( File reportsDirectory );

    File getTestSourceDirectory();

    void setTestSourceDirectory( File testSourceDirectory );

    String getTest();
    
    String getTestMethod();

    void setTest( String test );

    List getIncludes();

    void setIncludes( List includes );

    List getExcludes();

    void setExcludes( List excludes );

    ArtifactRepository getLocalRepository();

    void setLocalRepository( ArtifactRepository localRepository );

    Properties getSystemProperties();

    void setSystemProperties( Properties systemProperties );

    Map getSystemPropertyVariables();

    void setSystemPropertyVariables( Map systemPropertyVariables );

    Properties getProperties();

    void setProperties( Properties properties );

    Map getPluginArtifactMap();

    void setPluginArtifactMap( Map pluginArtifactMap );

    Map getProjectArtifactMap();

    void setProjectArtifactMap( Map projectArtifactMap );

    boolean isPrintSummary();

    void setPrintSummary( boolean printSummary );

    String getReportFormat();

    void setReportFormat( String reportFormat );

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

    Map getEnvironmentVariables();

    void setEnvironmentVariables( Map environmentVariables );

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

    List getRemoteRepositories();

    void setRemoteRepositories( List remoteRepositories );

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

    Boolean getFailIfNoTests();

    void setFailIfNoTests( Boolean failIfNoTests );

    boolean isMavenParallel();

    void setRunOrder( String runOrder );

    String getRunOrder();

}
