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
import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 * This interface contains all the common parameters that have different implementations in Surefire vs IntegrationTest
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

    boolean isPrintSummary();

    void setPrintSummary( boolean printSummary );

    String getReportFormat();

    void setReportFormat( String reportFormat );

    boolean isUseFile();

    void setUseFile( boolean useFile );

    String getDebugForkedProcess();

    void setDebugForkedProcess( String debugForkedProcess );

    int getForkedProcessTimeoutInSeconds();

    void setForkedProcessTimeoutInSeconds( int forkedProcessTimeoutInSeconds );

    double getParallelTestsTimeoutInSeconds();

    void setParallelTestsTimeoutInSeconds( double parallelTestsTimeoutInSeconds );

    double getParallelTestsTimeoutForcedInSeconds();

    void setParallelTestsTimeoutForcedInSeconds( double parallelTestsTimeoutForcedInSeconds );

    boolean isUseSystemClassLoader();

    void setUseSystemClassLoader( boolean useSystemClassLoader );

    boolean isUseManifestOnlyJar();

    void setUseManifestOnlyJar( boolean useManifestOnlyJar );

    Boolean getFailIfNoSpecifiedTests();

    void setFailIfNoSpecifiedTests( Boolean failIfNoSpecifiedTests );
}
