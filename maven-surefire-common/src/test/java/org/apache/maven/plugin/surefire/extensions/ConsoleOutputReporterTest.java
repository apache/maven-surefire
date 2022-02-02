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

import java.io.File;
import java.io.PrintStream;

import org.apache.maven.plugin.surefire.extensions.junit5.JUnit5ConsoleOutputReporter;
import org.apache.maven.plugin.surefire.report.ConsoleOutputFileReporter;
import org.apache.maven.plugin.surefire.report.DirectConsoleOutput;
import org.apache.maven.surefire.extensions.ConsoleOutputReportEventListener;
import org.apache.maven.surefire.extensions.ConsoleOutputReporter;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.reflect.Whitebox.getInternalState;

/**
 * tests for {@link SurefireConsoleOutputReporter} and {@link JUnit5ConsoleOutputReporter}.
 */
public class ConsoleOutputReporterTest
{
    @Test
    public void shouldCloneConsoleReporter()
    {
        SurefireConsoleOutputReporter extension = new SurefireConsoleOutputReporter();
        extension.setDisable( true );
        extension.setEncoding( "ISO-8859-1" );
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Object clone = extension.clone( classLoader );
        assertThat( clone )
                .isNotSameAs( extension );
        assertThat( clone )
                .isInstanceOf( SurefireConsoleOutputReporter.class );
        assertThat( clone.toString() )
                .isEqualTo( "SurefireConsoleOutputReporter{disable=true, encoding=ISO-8859-1}" );
        assertThat( ( (SurefireConsoleOutputReporter) clone ).isDisable() )
                .isTrue();
        assertThat( ( (SurefireConsoleOutputReporter) clone ).getEncoding() )
                .isEqualTo( "ISO-8859-1" );
    }

    @Test
    public void shouldAssertToStringConsoleReporter()
    {
        SurefireConsoleOutputReporter extension = new SurefireConsoleOutputReporter();
        assertThat( extension.toString() )
                .isEqualTo( "SurefireConsoleOutputReporter{disable=false, encoding=UTF-8}" );
    }

    @Test
    public void shouldCreateConsoleListener()
    {
        ConsoleOutputReporter extension = new SurefireConsoleOutputReporter();

        ConsoleOutputReportEventListener listener1 = extension.createListener( System.out, System.err );
        assertThat( listener1 )
                .isInstanceOf( DirectConsoleOutput.class );
        assertThat( (PrintStream) getInternalState( listener1, "out" ) )
                .isSameAs( System.out );
        assertThat( (PrintStream) getInternalState( listener1, "err" ) )
                .isSameAs( System.err );

        File target = new File( System.getProperty( "user.dir" ), "target" );
        File reportsDirectory = new File( target, "surefire-reports" );
        String reportNameSuffix = "suffix";
        boolean usePhrasedFileName = false;
        Integer forkNumber = 1;
        String encoding = "ISO-8859-2";
        extension.setEncoding( encoding );
        ConsoleOutputReportEventListener listener2 =
                extension.createListener( reportsDirectory, reportNameSuffix, forkNumber );
        assertThat( listener2 )
                .isInstanceOf( ConsoleOutputFileReporter.class );
        assertThat( (File) getInternalState( listener2, "reportsDirectory" ) )
                .isSameAs( reportsDirectory );
        assertThat( (String) getInternalState( listener2, "reportNameSuffix" ) )
                .isSameAs( reportNameSuffix );
        assertThat( (boolean) getInternalState( listener2, "usePhrasedFileName" ) )
                .isEqualTo( usePhrasedFileName );
        assertThat( (Integer) getInternalState( listener2, "forkNumber" ) )
                .isSameAs( forkNumber );
        assertThat( (String) getInternalState( listener2, "encoding" ) )
                .isSameAs( encoding );
        assertThat( (String) getInternalState( listener2, "reportEntryName" ) )
                .isNull();
    }

    @Test
    public void shouldCloneJUnit5ConsoleReporter()
    {
        JUnit5ConsoleOutputReporter extension = new JUnit5ConsoleOutputReporter();
        extension.setDisable( true );
        extension.setEncoding( "ISO-8859-1" );
        extension.setUsePhrasedFileName( true );
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Object clone = extension.clone( classLoader );
        assertThat( clone )
                .isNotSameAs( extension );
        assertThat( clone )
                .isInstanceOf( JUnit5ConsoleOutputReporter.class );
        assertThat( clone.toString() ).isEqualTo(
                "JUnit5ConsoleOutputReporter{disable=true, encoding=ISO-8859-1, usePhrasedFileName=true}" );
        assertThat( ( (JUnit5ConsoleOutputReporter) clone ).isDisable() )
                .isTrue();
        assertThat( ( (JUnit5ConsoleOutputReporter) clone ).getEncoding() )
                .isEqualTo( "ISO-8859-1" );
        assertThat( ( (JUnit5ConsoleOutputReporter) clone ).isUsePhrasedFileName() )
                .isTrue();
    }

    @Test
    public void shouldAssertToStringJUnit5ConsoleReporter()
    {
        JUnit5ConsoleOutputReporter extension = new JUnit5ConsoleOutputReporter();
        assertThat( extension.toString() )
                .isEqualTo( "JUnit5ConsoleOutputReporter{disable=false, encoding=UTF-8, usePhrasedFileName=false}" );
    }

    @Test
    public void shouldCreateJUnit5ConsoleListener()
    {
        JUnit5ConsoleOutputReporter extension = new JUnit5ConsoleOutputReporter();

        ConsoleOutputReportEventListener listener1 = extension.createListener( System.out, System.err );
        assertThat( listener1 )
                .isInstanceOf( DirectConsoleOutput.class );
        assertThat( (PrintStream) getInternalState( listener1, "out" ) )
                .isSameAs( System.out );
        assertThat( (PrintStream) getInternalState( listener1, "err" ) )
                .isSameAs( System.err );

        File target = new File( System.getProperty( "user.dir" ), "target" );
        File reportsDirectory = new File( target, "surefire-reports" );
        String reportNameSuffix = "suffix";
        boolean usePhrasedFileName = true;
        Integer forkNumber = 1;
        String encoding = "ISO-8859-1";
        extension.setEncoding( encoding );
        extension.setUsePhrasedFileName( usePhrasedFileName );
        ConsoleOutputReportEventListener listener2 =
                extension.createListener( reportsDirectory, reportNameSuffix, forkNumber );
        assertThat( listener2 )
                .isInstanceOf( ConsoleOutputFileReporter.class );
        assertThat( (File) getInternalState( listener2, "reportsDirectory" ) )
                .isSameAs( reportsDirectory );
        assertThat( (String) getInternalState( listener2, "reportNameSuffix" ) )
                .isSameAs( reportNameSuffix );
        assertThat( (boolean) getInternalState( listener2, "usePhrasedFileName" ) )
                .isEqualTo( usePhrasedFileName );
        assertThat( (Integer) getInternalState( listener2, "forkNumber" ) )
                .isSameAs( forkNumber );
        assertThat( (String) getInternalState( listener2, "encoding" ) )
                .isSameAs( encoding );
        assertThat( (String) getInternalState( listener2, "reportEntryName" ) )
                .isNull();
    }
}
