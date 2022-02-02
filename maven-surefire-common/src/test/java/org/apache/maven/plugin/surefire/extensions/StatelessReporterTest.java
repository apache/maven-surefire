package org.apache.maven.plugin.surefire.extensions;

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

import org.apache.maven.surefire.extensions.StatelessReportEventListener;
import org.apache.maven.plugin.surefire.extensions.junit5.JUnit5Xml30StatelessReporter;
import org.apache.maven.plugin.surefire.report.StatelessXmlReporter;
import org.apache.maven.plugin.surefire.report.TestSetStats;
import org.apache.maven.plugin.surefire.report.WrappedReportEntry;
import org.junit.Test;

import java.io.File;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.reflect.Whitebox.getInternalState;

/**
 * tests for {@link SurefireStatelessReporter} and {@link JUnit5Xml30StatelessReporter}.
 */
public class StatelessReporterTest
{
    @Test
    public void shouldCloneXmlReporter()
    {
        SurefireStatelessReporter extension = new SurefireStatelessReporter();
        extension.setDisable( true );
        extension.setVersion( "V1" );
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        Object clone = extension.clone( classLoader );
        assertThat( clone )
                .isNotSameAs( extension );
        assertThat( clone )
                .isInstanceOf( SurefireStatelessReporter.class );
        assertThat( clone.toString() )
                .isEqualTo( "SurefireStatelessReporter{version=V1, disable=true}" );
        assertThat( ( (SurefireStatelessReporter) clone ).isDisable() )
                .isTrue();
        assertThat( ( (SurefireStatelessReporter) clone ).getVersion() )
                .isEqualTo( "V1" );
    }

    @Test
    public void shouldAssertToStringXmlReporter()
    {
        SurefireStatelessReporter extension = new SurefireStatelessReporter();
        assertThat( extension.toString() )
                .isEqualTo( "SurefireStatelessReporter{version=3.0, disable=false}" );
    }

    @Test
    public void shouldCreateConsoleListener()
    {
        File target = new File( System.getProperty( "user.dir" ), "target" );
        File reportsDirectory = new File( target, "surefire-reports" );
        String reportNameSuffix = "suffix";
        String schema = "https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report-3.0.xsd";
        Map<String, Deque<WrappedReportEntry>> testClassMethodRunHistory = new HashMap<>();
        DefaultStatelessReportMojoConfiguration config =
                new DefaultStatelessReportMojoConfiguration( reportsDirectory, reportNameSuffix, true, 5, schema,
                        testClassMethodRunHistory );
        SurefireStatelessReporter extension = new SurefireStatelessReporter();

        assertThat( extension.getVersion() )
                .isEqualTo( "3.0" );
        extension.setVersion( "V3" );
        assertThat( extension.getVersion() )
                .isEqualTo( "V3" );

        assertThat( extension.isDisable() )
                .isFalse();
        extension.setDisable( true );
        assertThat( extension.isDisable() )
                .isTrue();

        StatelessReportEventListener<WrappedReportEntry, TestSetStats> listener = extension.createListener( config );
        assertThat( listener )
                .isInstanceOf( StatelessXmlReporter.class );
        assertThat( (File) getInternalState( listener, "reportsDirectory" ) )
                .isSameAs( reportsDirectory );
        assertThat( (String) getInternalState( listener, "reportNameSuffix" ) )
                .isSameAs( reportNameSuffix );
        assertThat( (boolean) getInternalState( listener, "trimStackTrace" ) )
                .isEqualTo( true );
        assertThat( (Integer) getInternalState( listener, "rerunFailingTestsCount" ) )
                .isEqualTo( 5 );
        assertThat( (String) getInternalState( listener, "xsdSchemaLocation" ) )
                .isSameAs( schema );
        assertThat( (String) getInternalState( listener, "xsdVersion" ) )
                .isEqualTo( "V3" );
        assertThat( (Map<?, ?>) getInternalState( listener, "testClassMethodRunHistoryMap" ) )
                .isSameAs( testClassMethodRunHistory );
        assertThat( (boolean) getInternalState( listener, "phrasedFileName" ) )
                .isEqualTo( false );
        assertThat( (boolean) getInternalState( listener, "phrasedSuiteName" ) )
                .isEqualTo( false );
        assertThat( (boolean) getInternalState( listener, "phrasedClassName" ) )
                .isEqualTo( false );
        assertThat( (boolean) getInternalState( listener, "phrasedMethodName" ) )
                .isEqualTo( false );
    }

    @Test
    public void shouldCloneJUnit5XmlReporter()
    {
        JUnit5Xml30StatelessReporter extension = new JUnit5Xml30StatelessReporter();
        extension.setDisable( true );
        extension.setVersion( "V1" );
        extension.setUsePhrasedFileName( true );
        extension.setUsePhrasedTestSuiteClassName( true );
        extension.setUsePhrasedTestCaseClassName( true );
        extension.setUsePhrasedTestCaseMethodName( true );
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        Object clone = extension.clone( classLoader );
        assertThat( clone )
                .isNotSameAs( extension );
        assertThat( clone )
                .isInstanceOf( JUnit5Xml30StatelessReporter.class );
        assertThat( clone.toString() )
                .isEqualTo( "JUnit5Xml30StatelessReporter{version=V1, disable=true, usePhrasedFileName=true, "
                        + "usePhrasedTestSuiteClassName=true, usePhrasedTestCaseClassName=true, "
                        + "usePhrasedTestCaseMethodName=true}" );
        assertThat( ( (JUnit5Xml30StatelessReporter) clone ).isDisable() )
                .isTrue();
        assertThat( ( (JUnit5Xml30StatelessReporter) clone ).getVersion() )
                .isEqualTo( "V1" );
        assertThat( ( (JUnit5Xml30StatelessReporter) clone ).getUsePhrasedFileName() )
                .isTrue();
        assertThat( ( (JUnit5Xml30StatelessReporter) clone ).getUsePhrasedTestCaseClassName() )
                .isTrue();
        assertThat( ( (JUnit5Xml30StatelessReporter) clone ).getUsePhrasedTestCaseMethodName() )
                .isTrue();
        assertThat( ( (JUnit5Xml30StatelessReporter) clone ).getUsePhrasedTestSuiteClassName() )
                .isTrue();
    }

    @Test
    public void shouldAssertToStringJUnit5ConsoleReporter()
    {
        JUnit5Xml30StatelessReporter extension = new JUnit5Xml30StatelessReporter();
        assertThat( extension.toString() )
                .isEqualTo( "JUnit5Xml30StatelessReporter{version=3.0, disable=false, "
                        + "usePhrasedFileName=false, usePhrasedTestSuiteClassName=false, "
                        + "usePhrasedTestCaseClassName=false, usePhrasedTestCaseMethodName=false}" );
    }

    @Test
    public void shouldCreateJUnit5ConsoleListener()
    {
        File target = new File( System.getProperty( "user.dir" ), "target" );
        File reportsDirectory = new File( target, "surefire-reports" );
        String reportNameSuffix = "suffix";
        String schema = "https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report-3.0.xsd";
        Map<String, Deque<WrappedReportEntry>> testClassMethodRunHistory = new HashMap<>();
        DefaultStatelessReportMojoConfiguration config =
                new DefaultStatelessReportMojoConfiguration( reportsDirectory, reportNameSuffix, true, 5, schema,
                        testClassMethodRunHistory );
        JUnit5Xml30StatelessReporter extension = new JUnit5Xml30StatelessReporter();

        assertThat( extension.getVersion() )
                .isEqualTo( "3.0" );
        extension.setVersion( "V3" );
        assertThat( extension.getVersion() )
                .isEqualTo( "V3" );

        assertThat( extension.isDisable() )
                .isFalse();
        extension.setDisable( true );
        assertThat( extension.isDisable() )
                .isTrue();

        assertThat( extension.getUsePhrasedFileName() )
                .isFalse();
        extension.setUsePhrasedFileName( true );
        assertThat( extension.getUsePhrasedFileName() )
                .isTrue();

        assertThat( extension.getUsePhrasedTestSuiteClassName() )
                .isFalse();
        extension.setUsePhrasedTestSuiteClassName( true );
        assertThat( extension.getUsePhrasedTestSuiteClassName() )
                .isTrue();

        assertThat( extension.getUsePhrasedTestCaseClassName() )
                .isFalse();
        extension.setUsePhrasedTestCaseClassName( true );
        assertThat( extension.getUsePhrasedTestSuiteClassName() )
                .isTrue();

        assertThat( extension.getUsePhrasedTestCaseMethodName() )
                .isFalse();
        extension.setUsePhrasedTestCaseMethodName( true );
        assertThat( extension.getUsePhrasedTestCaseMethodName() )
                .isTrue();

        StatelessReportEventListener<WrappedReportEntry, TestSetStats> listener = extension.createListener( config );
        assertThat( listener )
                .isInstanceOf( StatelessXmlReporter.class );
        assertThat( (File) getInternalState( listener, "reportsDirectory" ) )
                .isSameAs( reportsDirectory );
        assertThat( (String) getInternalState( listener, "reportNameSuffix" ) )
                .isSameAs( reportNameSuffix );
        assertThat( (boolean) getInternalState( listener, "trimStackTrace" ) )
                .isEqualTo( true );
        assertThat( (Integer) getInternalState( listener, "rerunFailingTestsCount" ) )
                .isEqualTo( 5 );
        assertThat( (String) getInternalState( listener, "xsdSchemaLocation" ) )
                .isSameAs( schema );
        assertThat( (String) getInternalState( listener, "xsdVersion" ) )
                .isEqualTo( "V3" );
        assertThat( (Map<?, ?>) getInternalState( listener, "testClassMethodRunHistoryMap" ) )
                .isSameAs( testClassMethodRunHistory );
        assertThat( (boolean) getInternalState( listener, "phrasedFileName" ) )
                .isEqualTo( true );
        assertThat( (boolean) getInternalState( listener, "phrasedSuiteName" ) )
                .isEqualTo( true );
        assertThat( (boolean) getInternalState( listener, "phrasedClassName" ) )
                .isEqualTo( true );
        assertThat( (boolean) getInternalState( listener, "phrasedMethodName" ) )
                .isEqualTo( true );
    }
}
