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

import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.maven.plugin.surefire.AbstractSurefireMojoJava7PlusTest;
import org.apache.maven.plugin.surefire.AbstractSurefireMojoTest;
import org.apache.maven.plugin.surefire.MojoMocklessTest;
import org.apache.maven.plugin.surefire.SurefireHelperTest;
import org.apache.maven.plugin.surefire.SurefireReflectorTest;
import org.apache.maven.plugin.surefire.SurefirePropertiesTest;
import org.apache.maven.plugin.surefire.booterclient.BooterDeserializerProviderConfigurationTest;
import org.apache.maven.plugin.surefire.booterclient.BooterDeserializerStartupConfigurationTest;
import org.apache.maven.plugin.surefire.booterclient.DefaultForkConfigurationTest;
import org.apache.maven.plugin.surefire.booterclient.ForkConfigurationTest;
import org.apache.maven.plugin.surefire.booterclient.ForkingRunListenerTest;
import org.apache.maven.plugin.surefire.booterclient.JarManifestForkConfigurationTest;
import org.apache.maven.plugin.surefire.booterclient.ModularClasspathForkConfigurationTest;
import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.TestLessInputStreamBuilderTest;
import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.TestProvidingInputStreamTest;
import org.apache.maven.plugin.surefire.report.DefaultReporterFactoryTest;
import org.apache.maven.plugin.surefire.report.StatelessXmlReporterTest;
import org.apache.maven.plugin.surefire.report.WrappedReportEntryTest;
import org.apache.maven.plugin.surefire.runorder.RunEntryStatisticsMapTest;
import org.apache.maven.plugin.surefire.util.DependenciesScannerTest;
import org.apache.maven.plugin.surefire.util.DirectoryScannerTest;
import org.apache.maven.plugin.surefire.util.ScannerUtilTest;
import org.apache.maven.plugin.surefire.util.SpecificFileFilterTest;
import org.apache.maven.surefire.report.ConsoleOutputFileReporterTest;
import org.apache.maven.surefire.report.FileReporterTest;
import org.apache.maven.surefire.report.RunStatisticsTest;
import org.apache.maven.surefire.spi.SPITest;
import org.apache.maven.surefire.util.RelocatorTest;

/**
 * Adapt the JUnit4 tests which use only annotations to the JUnit3 test suite.
 *
 * @author Tibor Digana (tibor17)
 * @since 2.19
 */
public class JUnit4SuiteTest extends TestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTestSuite( RelocatorTest.class );
        suite.addTestSuite( RunStatisticsTest.class );
        suite.addTestSuite( FileReporterTest.class );
        suite.addTestSuite( ConsoleOutputFileReporterTest.class );
        suite.addTestSuite( SurefirePropertiesTest.class );
        suite.addTestSuite( SpecificFileFilterTest.class );
        suite.addTest( new JUnit4TestAdapter( DirectoryScannerTest.class ) );
        suite.addTestSuite( DependenciesScannerTest.class );
        suite.addTestSuite( RunEntryStatisticsMapTest.class );
        suite.addTestSuite( WrappedReportEntryTest.class );
        suite.addTestSuite( StatelessXmlReporterTest.class );
        suite.addTestSuite( DefaultReporterFactoryTest.class );
        suite.addTestSuite( ForkingRunListenerTest.class );
        suite.addTest( new JUnit4TestAdapter( ForkConfigurationTest.class ) );
        suite.addTestSuite( BooterDeserializerStartupConfigurationTest.class );
        suite.addTestSuite( BooterDeserializerProviderConfigurationTest.class );
        suite.addTest( new JUnit4TestAdapter( TestProvidingInputStreamTest.class ) );
        suite.addTest( new JUnit4TestAdapter( TestLessInputStreamBuilderTest.class ) );
        suite.addTest( new JUnit4TestAdapter( SPITest.class ) );
        suite.addTest( new JUnit4TestAdapter( SurefireReflectorTest.class ) );
        suite.addTest( new JUnit4TestAdapter( SurefireHelperTest.class ) );
        suite.addTest( new JUnit4TestAdapter( AbstractSurefireMojoTest.class ) );
        suite.addTest( new JUnit4TestAdapter( DefaultForkConfigurationTest.class ) );
        suite.addTest( new JUnit4TestAdapter( JarManifestForkConfigurationTest.class ) );
        suite.addTest( new JUnit4TestAdapter( ModularClasspathForkConfigurationTest.class ) );
        suite.addTest( new JUnit4TestAdapter( AbstractSurefireMojoJava7PlusTest.class ) );
        suite.addTest( new JUnit4TestAdapter( ScannerUtilTest.class ) );
        suite.addTest( new JUnit4TestAdapter( MojoMocklessTest.class ) );
        return suite;
    }
}
