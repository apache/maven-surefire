package org.apache.maven.surefire.booter;

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

import junit.framework.TestCase;
import org.apache.maven.surefire.providerapi.DirectoryScannerParametersAware;
import org.apache.maven.surefire.providerapi.ProviderPropertiesAware;
import org.apache.maven.surefire.providerapi.ReporterConfigurationAware;
import org.apache.maven.surefire.providerapi.TestArtifactInfoAware;
import org.apache.maven.surefire.providerapi.TestClassLoaderAware;
import org.apache.maven.surefire.providerapi.TestSuiteDefinitionAware;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestSuiteDefinition;

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

/**
 * @author Kristian Rosenvold
 */
public class SurefireReflectorTest
    extends TestCase
{
    public void testSetDirectoryScannerParameters()
        throws Exception
    {
        SurefireReflector surefireReflector = getReflector();
        Foo foo = new Foo();

        DirectoryScannerParameters directoryScannerParameters =
            new DirectoryScannerParameters( new File( "ABC" ), new ArrayList(), new ArrayList(), Boolean.FALSE );
        assertTrue( surefireReflector.isDirectoryScannerParameterAware( foo ) );
        surefireReflector.setDirectoryScannerParameters( foo, directoryScannerParameters );

    }

    public void testTestSuiteDefinition()
        throws Exception
    {
        SurefireReflector surefireReflector = getReflector();
        Foo foo = new Foo();

        TestSuiteDefinition testSuiteDefinition =
            new TestSuiteDefinition( new File[]{ new File( "file1" ), new File( "file2" ) }, "aForkTest",
                                     new File( "TestSOurce" ), "aUserRequestedTest" );
        assertTrue( surefireReflector.isTestSuiteDefinitionAware( foo ) );
        surefireReflector.setTestSuiteDefinition( foo, testSuiteDefinition );
    }

    public void testProviderProperties()
        throws Exception
    {
        SurefireReflector surefireReflector = getReflector();
        Foo foo = new Foo();

        assertTrue( surefireReflector.isProviderPropertiesAware( foo ) );
        surefireReflector.setProviderProperties( foo, new Properties() );
    }

    public void testReporterConfiguration()
        throws Exception
    {
        SurefireReflector surefireReflector = getReflector();
        Foo foo = new Foo();

        ReporterConfiguration reporterConfiguration = new ReporterConfiguration( new File( "CDE" ), Boolean.TRUE );
        assertTrue( surefireReflector.isReporterConfigurationAwareAware( foo ) );
        surefireReflector.setReporterConfigurationAware( foo, reporterConfiguration );
    }

    public void testTestClassLoaderAware()
        throws Exception
    {
        SurefireReflector surefireReflector = getReflector();
        Foo foo = new Foo();

        ReporterConfiguration reporterConfiguration = new ReporterConfiguration( new File( "CDE" ), Boolean.TRUE );
        assertTrue( surefireReflector.isTestClassLoaderAware( foo ) );
        surefireReflector.setTestClassLoader( foo, getClass().getClassLoader() );
    }

    public void testArtifactInfoAware()
        throws Exception
    {
        SurefireReflector surefireReflector = getReflector();
        Foo foo = new Foo();

        TestArtifactInfo testArtifactInfo = new TestArtifactInfo( "12.3", "test" );
        assertTrue( surefireReflector.isTestArtifactInfoAware( foo ) );
        surefireReflector.setTestArtifactInfo( foo, testArtifactInfo );
    }

    private SurefireReflector getReflector()
    {
        return new SurefireReflector( this.getClass().getClassLoader() );
    }


    class Foo
        implements DirectoryScannerParametersAware, TestSuiteDefinitionAware, ProviderPropertiesAware,
        ReporterConfigurationAware, TestClassLoaderAware, TestArtifactInfoAware
    {
        DirectoryScannerParameters directoryScannerParameters;

        TestSuiteDefinition testSuiteDefinition;

        Properties providerProperties;

        ReporterConfiguration reporterConfiguration;

        public void setDirectoryScannerParameters( DirectoryScannerParameters directoryScanner )
        {
            this.directoryScannerParameters = directoryScanner;
        }


        public void setTestSuiteDefinition( TestSuiteDefinition testSuiteDefinition )
        {
            this.testSuiteDefinition = testSuiteDefinition;
        }

        public void setProviderProperties( Properties providerProperties )
        {
            this.providerProperties = providerProperties;
        }

        public void setReporterConfiguration( ReporterConfiguration reporterConfiguration )
        {
            reporterConfiguration = reporterConfiguration;
        }

        public void setTestClassLoader( ClassLoader classLoader )
        {
        }

        public void setTestArtifactInfo( TestArtifactInfo testArtifactInfo )
        {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}
