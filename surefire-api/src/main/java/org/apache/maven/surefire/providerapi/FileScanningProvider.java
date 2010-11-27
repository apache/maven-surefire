package org.apache.maven.surefire.providerapi;
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

import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterManagerFactory;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.testset.TestSuiteDefinition;
import org.apache.maven.surefire.util.DefaultDirectoryScanner;
import org.apache.maven.surefire.util.DirectoryScanner;

/**
 * @author Kristian Rosenvold
 */
public class FileScanningProvider
    implements DirectoryScannerParametersAware, ReportingAware, TestClassLoaderAware, TestSuiteDefinitionAware,ReporterManagerFactoryAware
{
    private DirectoryScannerParameters directoryScannerParameters;

    private ReporterManagerFactory reporterManagerFactory;

    private ClassLoader testsClassLoader;

    private TestSuiteDefinition testSuiteDefinition;



    public void setDirectoryScannerParameters( DirectoryScannerParameters directoryScanner )
    {
        this.directoryScannerParameters = directoryScanner;
    }

    public void setReporterManagerFactory( ReporterManagerFactory reporterManagerFactory )
    {
        this.reporterManagerFactory = reporterManagerFactory;
    }

    public void setTestClassLoader( ClassLoader testsClassLoader )
    {
        this.testsClassLoader = testsClassLoader;
    }


    public RunResult invoke()
        throws TestSetFailedException, ReporterException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    protected DirectoryScanner getDirectoryScanner()
    {
        return new DefaultDirectoryScanner( directoryScannerParameters.getTestClassesDirectory(),
                                            directoryScannerParameters.getIncludes(),
                                            directoryScannerParameters.getExcludes() );
    }

    protected ReporterManagerFactory getReporterManagerFactory()
    {
        return reporterManagerFactory;
    }

    protected ClassLoader getTestsClassLoader()
    {
        return testsClassLoader;
    }

    protected Boolean getFailifNoTests()
    {
        return directoryScannerParameters.isFailIfNoTests();
    }

    protected TestSuiteDefinition getTestSuiteDefinition()
    {
        return testSuiteDefinition;
    }

    public void setTestSuiteDefinition( TestSuiteDefinition testSuiteDefinition )
    {
        this.testSuiteDefinition = testSuiteDefinition;
    }



}
