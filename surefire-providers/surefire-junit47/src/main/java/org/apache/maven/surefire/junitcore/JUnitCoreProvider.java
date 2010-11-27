package org.apache.maven.surefire.junitcore;
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
import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.RunStatistics;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;

import java.util.Iterator;
import java.util.Properties;

/**
 * @author Kristian Rosenvold
 */
@SuppressWarnings( { "UnusedDeclaration" } )
public class JUnitCoreProvider
    extends FileScanningProvider
    implements SurefireProvider, ProviderPropertiesAware
{
    private Properties providerProperties;


    @SuppressWarnings( { "UnnecessaryUnboxing" } )
    public RunResult invoke()
        throws TestSetFailedException, ReporterException
    {
        // Todo; Not there quite yet
        JUnitCoreDirectoryTestSuite jUnitCoreDirectoryTestSuite = getSuite();

        RunStatistics runStatistics = getReporterManagerFactory().getGlobalRunStatistics();

        jUnitCoreDirectoryTestSuite.locateTestSets( getTestsClassLoader() );
        int totalTests = 0;

        int testCount = jUnitCoreDirectoryTestSuite.getNumTests();
        if ( testCount > 0 )
        {
            totalTests += testCount;
        }

        if ( totalTests == 0 && getFailifNoTests().booleanValue() )
        {
            getReporterManagerFactory().createReporterManager().writeMessage( "There are no tests to run." );
            return RunResult.No_Tests;
        }

        // getLog().info( "Concurrency config is " + getProperties().toString() );

        if ( getTestSuiteDefinition().getTestForFork() != null )
        {
            jUnitCoreDirectoryTestSuite.execute( getTestSuiteDefinition().getTestForFork(), getReporterManagerFactory(),
                                                 getTestsClassLoader() );
        }
        else
        {
            jUnitCoreDirectoryTestSuite.execute( getReporterManagerFactory(), getTestsClassLoader() );
        }

        jUnitCoreDirectoryTestSuite.execute( getReporterManagerFactory(), getTestsClassLoader() );

        return runStatistics.getRunResult();
    }

    private JUnitCoreDirectoryTestSuite getSuite()
    {
        return new JUnitCoreDirectoryTestSuite( getDirectoryScanner(), new JUnitCoreParameters( providerProperties ),
                                                getReporterManagerFactory() );
    }

    public Iterator getSuites()
    {
        return getSuite().locateTestSetsImpl( getTestsClassLoader() ).entrySet().iterator();
    }


    public void setProviderProperties( Properties providerProperties )
    {
        this.providerProperties = providerProperties;
    }
}
