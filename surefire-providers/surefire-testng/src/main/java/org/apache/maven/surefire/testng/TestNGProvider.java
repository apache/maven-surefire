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

import java.util.Iterator;
import java.util.Properties;
import org.apache.maven.surefire.providerapi.AbstractProvider;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestRequest;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.NestedRuntimeException;
import org.apache.maven.surefire.util.RunOrderCalculator;
import org.apache.maven.surefire.util.ScanResult;
import org.apache.maven.surefire.util.TestsToRun;

/**
 * @author Kristian Rosenvold
 * @noinspection UnusedDeclaration
 */
public class TestNGProvider
    extends AbstractProvider
{
    private final Properties providerProperties;

    private final ReporterConfiguration reporterConfiguration;

    private final ClassLoader testClassLoader;

    private final ScanResult scanResult;

    private final TestRequest testRequest;

    private final ProviderParameters providerParameters;

    private TestsToRun testsToRun;

    private final RunOrderCalculator runOrderCalculator;

    public TestNGProvider( ProviderParameters booterParameters )
    {
        this.providerParameters = booterParameters;
        this.testClassLoader = booterParameters.getTestClassLoader();
        this.runOrderCalculator = booterParameters.getRunOrderCalculator();
        this.providerProperties = booterParameters.getProviderProperties();
        this.testRequest = booterParameters.getTestRequest();
        reporterConfiguration = booterParameters.getReporterConfiguration();
        this.scanResult = booterParameters.getScanResult();
    }

    public Boolean isRunnable()
    {
        return Boolean.TRUE;
    }

    public RunResult invoke( Object forkTestSet )
        throws TestSetFailedException, ReporterException
    {

        final ReporterFactory reporterFactory = providerParameters.getReporterFactory();

        if ( isTestNGXmlTestSuite( testRequest ) )
        {
            TestNGXmlTestSuite testNGXmlTestSuite = getXmlSuite();
            testNGXmlTestSuite.locateTestSets( testClassLoader );
            if ( forkTestSet != null && testRequest == null )
            {
                testNGXmlTestSuite.execute( (String) forkTestSet, reporterFactory );
            }
            else
            {
                testNGXmlTestSuite.execute( reporterFactory );
            }
        }
        else
        {
            if ( testsToRun == null )
            {
                if ( forkTestSet instanceof TestsToRun )
                {
                    testsToRun = (TestsToRun) forkTestSet;
                }
                else if ( forkTestSet instanceof Class )
                {
                    testsToRun = TestsToRun.fromClass( (Class) forkTestSet );
                }
                else
                {
                    testsToRun = scanClassPath();
                }
            }
            TestNGDirectoryTestSuite suite = getDirectorySuite();
            suite.execute( testsToRun, reporterFactory );
        }

        return reporterFactory.close();
    }

    boolean isTestNGXmlTestSuite( TestRequest testSuiteDefinition )
    {
        return testSuiteDefinition.getSuiteXmlFiles() != null && testSuiteDefinition.getSuiteXmlFiles().size() > 0 &&
            testSuiteDefinition.getRequestedTest() == null;

    }


    private TestNGDirectoryTestSuite getDirectorySuite()
    {
        return new TestNGDirectoryTestSuite( testRequest.getTestSourceDirectory().toString(), providerProperties,
                                             reporterConfiguration.getReportsDirectory(),
                                             testRequest.getRequestedTestMethod(), runOrderCalculator, scanResult );
    }

    private TestNGXmlTestSuite getXmlSuite()
    {
        return new TestNGXmlTestSuite( testRequest.getSuiteXmlFiles(), testRequest.getTestSourceDirectory().toString(),
                                       providerProperties,
                                       reporterConfiguration.getReportsDirectory() );
    }


    public Iterator getSuites()
    {
        if ( isTestNGXmlTestSuite( testRequest ) )
        {
            try
            {
                return getXmlSuite().locateTestSets( testClassLoader ).keySet().iterator();
            }
            catch ( TestSetFailedException e )
            {
                throw new NestedRuntimeException( e );
            }
        }
        else
        {
            testsToRun = scanClassPath();
            return testsToRun.iterator();
        }
    }

    private TestsToRun scanClassPath()
    {
        final TestsToRun scanned = scanResult.applyFilter( null, testClassLoader );
        return runOrderCalculator.orderTestClasses( scanned );
    }


}
