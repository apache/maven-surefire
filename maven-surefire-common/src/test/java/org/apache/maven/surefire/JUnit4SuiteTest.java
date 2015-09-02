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
import org.apache.maven.plugin.surefire.SurefirePropertiesTest;
import org.apache.maven.plugin.surefire.booterclient.BooterDeserializerProviderConfigurationTest;
import org.apache.maven.plugin.surefire.booterclient.BooterDeserializerStartupConfigurationTest;
import org.apache.maven.plugin.surefire.booterclient.ForkConfigurationTest;
import org.apache.maven.plugin.surefire.booterclient.ForkingRunListenerTest;
import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.TestLessInputStreamBuilderTest;
import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.TestProvidingInputStreamTest;
import org.apache.maven.plugin.surefire.report.DefaultReporterFactoryTest;
import org.apache.maven.plugin.surefire.report.StatelessXmlReporterTest;
import org.apache.maven.plugin.surefire.report.WrappedReportEntryTest;
import org.apache.maven.plugin.surefire.runorder.RunEntryStatisticsMapTest;
import org.apache.maven.plugin.surefire.util.DependenciesScannerTest;
import org.apache.maven.plugin.surefire.util.DirectoryScannerTest;
import org.apache.maven.plugin.surefire.util.SpecificFileFilterTest;
import org.apache.maven.surefire.report.ConsoleOutputFileReporterTest;
import org.apache.maven.surefire.report.FileReporterTest;
import org.apache.maven.surefire.report.RunStatisticsTest;
import org.apache.maven.surefire.util.RelocatorTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Adapt the JUnit4 tests which use only annotations to the JUnit3 test suite.
 *
 * @author Tibor Digana (tibor17)
 * @since 2.19
 */
@Suite.SuiteClasses( {
    RelocatorTest.class,
    RunStatisticsTest.class,
    FileReporterTest.class,
    ConsoleOutputFileReporterTest.class,
    SurefirePropertiesTest.class,
    SpecificFileFilterTest.class,
    DirectoryScannerTest.class,
    DependenciesScannerTest.class,
    RunEntryStatisticsMapTest.class,
    WrappedReportEntryTest.class,
    StatelessXmlReporterTest.class,
    DefaultReporterFactoryTest.class,
    ForkingRunListenerTest.class,
    ForkConfigurationTest.class,
    BooterDeserializerStartupConfigurationTest.class,
    BooterDeserializerProviderConfigurationTest.class,
    TestProvidingInputStreamTest.class,
    TestLessInputStreamBuilderTest.class
} )
@RunWith( Suite.class )
public class JUnit4SuiteTest
{
    public static Test suite()
    {
        return new JUnit4TestAdapter( JUnit4SuiteTest.class );
    }
}
