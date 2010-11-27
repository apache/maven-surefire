package org.apache.maven.surefire.testng;
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

import org.apache.maven.surefire.providerapi.FileScanningProvider;
import org.apache.maven.surefire.providerapi.ProviderPropertiesAware;
import org.apache.maven.surefire.providerapi.ReporterConfigurationAware;
import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.apache.maven.surefire.providerapi.TestArtifactInfoAware;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.suite.SurefireTestSuite;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.testset.TestSuiteDefinition;
import org.apache.maven.surefire.util.DefaultDirectoryScanner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

/**
 * @author Kristian Rosenvold
 * @noinspection UnusedDeclaration
 */
public class TestNGProvider
    extends FileScanningProvider
    implements SurefireProvider, ProviderPropertiesAware, TestArtifactInfoAware, ReporterConfigurationAware
{
    private Properties providerProperties;

    private TestArtifactInfo testArtifactInfo;

    private ReporterConfiguration reporterConfiguration;


    public RunResult invoke()
        throws TestSetFailedException, ReporterException
    {
        SurefireTestSuite suite = getActiveSuite();
        suite.locateTestSets( getTestsClassLoader() );
        if ( getTestSuiteDefinition().getTestForFork() != null )
        {
            suite.execute( getTestSuiteDefinition().getTestForFork(), getReporterManagerFactory(),
                           getTestsClassLoader() );
        }
        else
        {
            suite.execute( getReporterManagerFactory(), getTestsClassLoader() );
        }
        return RunResult.totalCountOnly( suite.getNumTests() );
    }

    boolean isTestNGXmlTestSuite( TestSuiteDefinition testSuiteDefinition )
    {
        return testSuiteDefinition.getSuiteXmlFiles() != null && testSuiteDefinition.getSuiteXmlFiles().length > 0 &&
            testSuiteDefinition.getRequestedTest() == null;

    }

    private DefaultDirectoryScanner getDefaultDirectoryScanner()
    {
        return (DefaultDirectoryScanner) getDirectoryScanner();  // A hack to get hold of parameters
    }

    private TestNGDirectoryTestSuite getDirectorySuite()
    {
        final DefaultDirectoryScanner defaultDirectoryScanner = getDefaultDirectoryScanner();
        return new TestNGDirectoryTestSuite( defaultDirectoryScanner.getBasedir(),
                                             new ArrayList( defaultDirectoryScanner.getIncludes() ),
                                             new ArrayList( defaultDirectoryScanner.getExcludes() ),
                                             getTestSuiteDefinition().getTestSourceDirectory().toString(),
                                             testArtifactInfo.getVersion(), testArtifactInfo.getClassifier(),
                                             providerProperties, reporterConfiguration.getReportsDirectory() );
    }

    private TestNGXmlTestSuite getXmlSuite()
    {
        return new TestNGXmlTestSuite( getTestSuiteDefinition().getSuiteXmlFiles(),
                                       getTestSuiteDefinition().getTestSourceDirectory().toString(),
                                       testArtifactInfo.getVersion(), testArtifactInfo.getClassifier(),
                                       providerProperties, reporterConfiguration.getReportsDirectory() );
    }


    public SurefireTestSuite getActiveSuite()
    {
        return isTestNGXmlTestSuite( getTestSuiteDefinition() )
            ? (SurefireTestSuite) getXmlSuite()
            : getDirectorySuite();
    }

    public Iterator getSuites()
    {
        try
        {
            return getActiveSuite().locateTestSets( getTestsClassLoader() ).keySet().iterator();
        }
        catch ( TestSetFailedException e )
        {
            throw new RuntimeException( e );
        }
    }


    public void setProviderProperties( Properties providerProperties )
    {
        this.providerProperties = providerProperties;
    }

    public void setTestArtifactInfo( TestArtifactInfo testArtifactInfo )
    {
        this.testArtifactInfo = testArtifactInfo;
    }

    public void setReporterConfiguration( ReporterConfiguration reporterConfiguration )
    {
        this.reporterConfiguration = reporterConfiguration;
    }
}
