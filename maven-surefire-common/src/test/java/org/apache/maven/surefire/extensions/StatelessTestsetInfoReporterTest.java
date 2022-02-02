package org.apache.maven.surefire.extensions;

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

import org.apache.maven.plugin.surefire.extensions.SurefireStatelessTestsetInfoReporter;
import org.apache.maven.plugin.surefire.extensions.junit5.JUnit5StatelessTestsetInfoReporter;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.report.ConsoleReporter;
import org.apache.maven.plugin.surefire.report.FileReporter;
import org.apache.maven.plugin.surefire.report.TestSetStats;
import org.apache.maven.plugin.surefire.report.WrappedReportEntry;
import org.apache.maven.surefire.api.report.TestSetReportEntry;
import org.apache.maven.surefire.shared.utils.logging.MessageUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.reflect.Whitebox.getInternalState;

/**
 * tests for {@link SurefireStatelessTestsetInfoReporter} and {@link JUnit5StatelessTestsetInfoReporter}.
 */
@RunWith( PowerMockRunner.class )
@PowerMockIgnore( { "org.jacoco.agent.rt.*", "com.vladium.emma.rt.*" } )
public class StatelessTestsetInfoReporterTest
{
    @Mock
    private ConsoleLogger consoleLogger;

    @Mock
    private TestSetReportEntry eventTestsetStarting;

    @Mock
    private WrappedReportEntry eventTestsetFinished;

    @Test
    public void shouldCloneReporter()
    {
        SurefireStatelessTestsetInfoReporter extension = new SurefireStatelessTestsetInfoReporter();
        extension.setDisable( true );
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        Object clone = extension.clone( classLoader );
        assertThat( clone )
                .isNotSameAs( extension );
        assertThat( clone )
                .isInstanceOf( SurefireStatelessTestsetInfoReporter.class );
        assertThat( clone.toString() )
                .isEqualTo( "SurefireStatelessTestsetInfoReporter{disable=true}" );
        assertThat( ( (SurefireStatelessTestsetInfoReporter) clone ).isDisable() )
                .isTrue();
    }

    @Test
    public void shouldAssertToStringReporter()
    {
        SurefireStatelessTestsetInfoReporter extension = new SurefireStatelessTestsetInfoReporter();
        assertThat( extension.toString() )
                .isEqualTo( "SurefireStatelessTestsetInfoReporter{disable=false}" );
    }

    @Test
    public void shouldCreateFileReporterListener()
    {
        File target = new File( System.getProperty( "user.dir" ), "target" );
        File reportsDirectory = new File( target, "surefire-reports" );
        String reportNameSuffix = "suffix";
        Charset encoding = StandardCharsets.UTF_8;
        SurefireStatelessTestsetInfoReporter extension = new SurefireStatelessTestsetInfoReporter();

        assertThat( extension.isDisable() )
                .isFalse();
        extension.setDisable( true );
        assertThat( extension.isDisable() )
                .isTrue();

        StatelessTestsetInfoFileReportEventListener<WrappedReportEntry, TestSetStats> listener =
                extension.createListener( reportsDirectory, reportNameSuffix, encoding );
        assertThat( listener )
                .isInstanceOf( FileReporter.class );
        assertThat( listener.getReportsDirectory() )
                .isSameAs( reportsDirectory );
        assertThat( listener.getReportNameSuffix() )
                .isSameAs( reportNameSuffix );
        assertThat( listener.getEncoding() )
                .isSameAs( encoding );
        assertThat( (boolean) getInternalState( listener, "usePhrasedFileName" ) )
                .isEqualTo( false );
        assertThat( (boolean) getInternalState( listener, "usePhrasedClassNameInRunning" ) )
                .isEqualTo( false );
        assertThat( (boolean) getInternalState( listener, "usePhrasedClassNameInTestCaseSummary" ) )
                .isEqualTo( false );
    }

    @Test
    public void shouldCreateConsoleReporterListener()
    {
        SurefireStatelessTestsetInfoReporter extension = new SurefireStatelessTestsetInfoReporter();

        assertThat( extension.isDisable() )
                .isFalse();
        extension.setDisable( true );
        assertThat( extension.isDisable() )
                .isTrue();

        StatelessTestsetInfoConsoleReportEventListener<WrappedReportEntry, TestSetStats> listener =
                extension.createListener( consoleLogger );
        assertThat( listener )
                .isInstanceOf( ConsoleReporter.class );
        assertThat( listener.getConsoleLogger() )
                .isSameAs( consoleLogger );
        assertThat( (boolean) getInternalState( listener, "usePhrasedClassNameInRunning" ) )
                .isEqualTo( false );
        assertThat( (boolean) getInternalState( listener, "usePhrasedClassNameInTestCaseSummary" ) )
                .isEqualTo( false );
    }

    @Test
    public void shouldCloneJUnit5Reporter()
    {
        JUnit5StatelessTestsetInfoReporter extension = new JUnit5StatelessTestsetInfoReporter();
        extension.setDisable( true );
        extension.setUsePhrasedFileName( true );
        extension.setUsePhrasedClassNameInTestCaseSummary( true );
        extension.setUsePhrasedClassNameInRunning( true );
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        Object clone = extension.clone( classLoader );
        assertThat( clone )
                .isNotSameAs( extension );
        assertThat( clone )
                .isInstanceOf( JUnit5StatelessTestsetInfoReporter.class );
        assertThat( clone.toString() )
                .isEqualTo( "JUnit5StatelessTestsetInfoReporter{disable=true, usePhrasedFileName=true, "
                        + "usePhrasedClassNameInRunning=true, usePhrasedClassNameInTestCaseSummary=true}" );
        assertThat( ( (JUnit5StatelessTestsetInfoReporter) clone ).isDisable() )
                .isTrue();
        assertThat( ( (JUnit5StatelessTestsetInfoReporter) clone ).isUsePhrasedFileName() )
                .isTrue();
        assertThat( ( (JUnit5StatelessTestsetInfoReporter) clone ).isUsePhrasedClassNameInTestCaseSummary() )
                .isTrue();
        assertThat( ( (JUnit5StatelessTestsetInfoReporter) clone ).isUsePhrasedClassNameInRunning() )
                .isTrue();
    }

    @Test
    public void shouldAssertToStringJUnit5Reporter()
    {
        JUnit5StatelessTestsetInfoReporter extension = new JUnit5StatelessTestsetInfoReporter();
        assertThat( extension.toString() )
                .isEqualTo( "JUnit5StatelessTestsetInfoReporter{disable=false, usePhrasedFileName=false, "
                                + "usePhrasedClassNameInRunning=false, usePhrasedClassNameInTestCaseSummary=false}" );
    }

    @Test
    public void shouldReportTestsetLifecycle()
    {
        ConsoleReporter consoleReporter = new ConsoleReporter( consoleLogger, false, false );
        MessageUtils.setColorEnabled( false );

        when( eventTestsetStarting.getNameWithGroup() ).thenReturn( "pkg.MyTest" );
        when( eventTestsetFinished.getNameWithGroup() ).thenReturn( "pkg.MyTest" );
        when( eventTestsetFinished.getElapsedTimeVerbose() ).thenReturn( "Time elapsed: 1.03 s" );

        consoleReporter.testSetStarting( eventTestsetStarting );
        ArgumentCaptor<String> logs = ArgumentCaptor.forClass( String.class );
        verify( consoleLogger, times( 1 ) ).info( logs.capture() );
        verifyNoMoreInteractions( consoleLogger );
        assertThat( logs.getAllValues() )
                .hasSize( 1 )
                .contains( "Running pkg.MyTest" );

        TestSetStats testSetStats = new TestSetStats( false, true );
        testSetStats.testStart();
        testSetStats.testFailure( eventTestsetFinished );
        assertThat( testSetStats.getCompletedCount() ).isEqualTo( 1 );
        assertThat( testSetStats.getFailures() ).isEqualTo( 1 );
        assertThat( testSetStats.getErrors() ).isEqualTo( 0 );
        assertThat( testSetStats.getSkipped() ).isEqualTo( 0 );
        reset( consoleLogger );
        consoleReporter.testSetCompleted( eventTestsetFinished, testSetStats, singletonList( "pkg.MyTest failed" ) );
        consoleReporter.reset();
        verify( consoleLogger, never() ).info( anyString() );
        verify( consoleLogger, never() ).warning( anyString() );
        logs = ArgumentCaptor.forClass( String.class );
        verify( consoleLogger, times( 2 ) ).error( logs.capture() );
        List<String> messages = logs.getAllValues();
        assertThat( messages )
                .hasSize( 2 )
                .containsSequence(
                        "Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 1.03 s "
                        + "<<< FAILURE! - in pkg.MyTest", "pkg.MyTest failed" );
        verifyNoMoreInteractions( consoleLogger );
    }
}
