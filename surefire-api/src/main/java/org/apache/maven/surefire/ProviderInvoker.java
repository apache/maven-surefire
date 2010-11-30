package org.apache.maven.surefire;

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

import org.apache.maven.surefire.booter.SurefireReflector;
import org.apache.maven.surefire.providerapi.ReporterManagerFactoryAware;
import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterManagerFactory;
import org.apache.maven.surefire.report.ReporterManagerFactory2;
import org.apache.maven.surefire.report.RunStatistics;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestRequest;
import org.apache.maven.surefire.testset.TestSetFailedException;

import java.util.List;
import java.util.Properties;

/**
 * Just outside the actual provider, always in the same classloader as the provider
 *
 * @author Jason van Zyl
 * @author Kristian Rosenvold
 * @version $Id$
 */
public class ProviderInvoker
{

    private static final int NO_TESTS = 254;

    public int run( ReporterConfiguration reporterConfiguration, List reportDefinitions,
                    ClassLoader surefireClassLoader, ClassLoader testsClassLoader, Properties results,
                    Boolean failIfNoTests, TestRequest testSuiteDefinition, TestArtifactInfo testArtifactInfo,
                    String providerClassName, DirectoryScannerParameters directoryScannerParameters,
                    Object forkTestSet)
        throws ReporterException, TestSetFailedException
    {
        ReporterManagerFactory reporterManagerFactory =
            new ReporterManagerFactory2( reportDefinitions, surefireClassLoader, reporterConfiguration );

        RunStatistics runStatistics = reporterManagerFactory.getGlobalRunStatistics();
        if ( results != null )
        {
            runStatistics.initResultsFromProperties( results );
        }

        int totalTests = 0;

        SurefireReflector surefireReflector = new SurefireReflector( surefireClassLoader );
        Object o = surefireReflector.newInstance( providerClassName );
        SurefireProvider provider = (SurefireProvider) o;
        surefireReflector.setIfDirScannerAware( o, directoryScannerParameters );
        surefireReflector.setTestSuiteDefinitionAware( o, testSuiteDefinition );
        surefireReflector.setProviderPropertiesAware( o, results );
        surefireReflector.setReporterConfigurationAware( o, reporterConfiguration );
        if ( o instanceof ReporterManagerFactoryAware )
        {
            ( (ReporterManagerFactoryAware) o ).setReporterManagerFactory( reporterManagerFactory );
        }
        surefireReflector.setTestClassLoaderAware( o, testsClassLoader );
        surefireReflector.setTestArtifactInfoAware( o, testArtifactInfo );

        RunResult invoke = provider.invoke(forkTestSet);
        int testCount = invoke.getCompletedCount(); // TODO: Verify that this is correct digit
        if ( testCount > 0 )
        {
            totalTests += testCount;
        }

        if ( totalTests == 0 )
        {
            reporterManagerFactory.createReporterManager().writeMessage( "There are no tests to run." );
        }

        reporterManagerFactory.close();

        if ( results != null )
        {
            runStatistics.updateResultsProperties( results );
        }

        if ( failIfNoTests.booleanValue() )
        {
            if ( runStatistics.getCompletedCount() == 0 )
            {
                return NO_TESTS;
            }
        }

        return runStatistics.getRunResult().getBooterCode();
    }
}
