package org.apache.maven.plugin.surefire;

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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.surefire.AbstractSurefireMojoTest.Mojo;
import org.apache.maven.plugin.surefire.log.PluginConsoleLogger;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.apache.maven.surefire.booter.SurefireBooterForkException;
import org.codehaus.plexus.logging.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.addAll;
import static java.util.Collections.singleton;
import static org.apache.maven.plugin.surefire.SurefireHelper.escapeToPlatformPath;
import static org.apache.maven.plugin.surefire.SurefireHelper.reportExecution;
import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mock;

/**
 * Test of {@link SurefireHelper}.
 */
public class SurefireHelperTest
{

    @Rule
    public ExpectedException e = ExpectedException.none();

    @Test
    public void shouldReplaceForkNumberPath()
    {
        File root = new File( System.getProperty( "user.dir", "" ) );
        File pathWithPlaceholder = new File( root, "${surefire.forkNumber}" );
        File changed = SurefireHelper.replaceForkThreadsInPath( pathWithPlaceholder, 5 );
        assertThat( changed.getPath() )
                .isEqualTo( new File( root, "5" ).getPath() );
    }

    @Test
    public void shouldReplaceLongForkNumberPath()
    {
        File root = new File( System.getProperty( "user.dir", "" ) );
        File subDir = new File( root, "reports-${surefire.forkNumber}" );
        File pathWithPlaceholder = new File( subDir, "subfolder" );
        File changed = SurefireHelper.replaceForkThreadsInPath( pathWithPlaceholder, 5 );
        assertThat( changed.getPath() )
                .isEqualTo( new File( new File( root, "reports-5" ), "subfolder" ).getPath() );
    }

    @Test
    public void shouldBeThreeDumpFiles()
    {
        String[] dumps = SurefireHelper.getDumpFilesToPrint();
        assertThat( dumps ).hasSize( 4 );
        assertThat( dumps ).doesNotHaveDuplicates();
        List<String> onlyStrings = new ArrayList<>();
        addAll( onlyStrings, dumps );
        onlyStrings.removeAll( singleton( (String) null ) );
        assertThat( onlyStrings ).hasSize( 4 );
    }

    @Test
    public void shouldCloneDumpFiles()
    {
        String[] dumps1 = SurefireHelper.getDumpFilesToPrint();
        String[] dumps2 = SurefireHelper.getDumpFilesToPrint();
        assertThat( dumps1 ).isNotSameAs( dumps2 );
    }

    @Test
    public void testConstants()
    {
        assertThat( SurefireHelper.DUMPSTREAM_FILENAME_FORMATTER )
                .isEqualTo( SurefireHelper.DUMP_FILE_PREFIX + "%d.dumpstream" );

        assertThat( String.format( SurefireHelper.DUMPSTREAM_FILENAME_FORMATTER, 5 ) )
                .endsWith( "-jvmRun5.dumpstream" );
    }

    @Test
    public void shouldEscapeWindowsPath()
    {
        assumeTrue( IS_OS_WINDOWS );
        String root = "X:\\path\\to\\project\\";
        String pathToJar = "target\\surefire\\surefirebooter4942721306300108667.jar";
        @SuppressWarnings( "checkstyle:magicnumber" )
        int projectNameLength = 247 - root.length() - pathToJar.length();
        StringBuilder projectFolder = new StringBuilder();
        for ( int i = 0; i < projectNameLength; i++ )
        {
            projectFolder.append( 'x' );
        }
        String path = root + projectFolder + "\\" + pathToJar;
        String escaped = escapeToPlatformPath( path );
        assertThat( escaped ).isEqualTo( "\\\\?\\" + path );

        path = root + "\\" + pathToJar;
        escaped = escapeToPlatformPath( path );
        assertThat( escaped ).isEqualTo( root + "\\" + pathToJar );
    }

    @Test
    public void shouldHandleFailWithoutExitCode() throws Exception
    {
        RunResult summary = new RunResult( 0, 0, 0, 0 );
        Mojo plugin = new Mojo();
        plugin.setTestFailureIgnore( true );

        Logger logger = mock( Logger.class );
        when( logger.isErrorEnabled() ).thenReturn( true );
        doNothing().when( logger ).error( anyString() );
        TestSetFailedException exc = new TestSetFailedException( "failure" );
        reportExecution( plugin, summary, new PluginConsoleLogger( logger ), exc );
        ArgumentCaptor<String> errorMessage = ArgumentCaptor.forClass( String.class );
        verify( logger ).error( errorMessage.capture() );
        assertThat( errorMessage.getValue() ).contains( "failure" );
    }

    @Test
    public void shouldHandleFailIfJvmNonZeroExitCode() throws Exception
    {
        RunResult summary = new RunResult( 0, 0, 0, 0 );
        Mojo plugin = new Mojo();
        plugin.setTestFailureIgnore( true );

        SurefireBooterForkException exc = new SurefireBooterForkException( "Unrecognized option: -Xxxx" );
        e.expect( MojoExecutionException.class );
        e.expectMessage( containsString( "Unrecognized option: -Xxxx" ) );
        reportExecution( plugin, summary, new PluginConsoleLogger( mock( Logger.class ) ), exc );
    }

    @Test
    public void shouldHandleFailIfNoTests() throws Exception
    {
        RunResult summary = new RunResult( 0, 0, 0, 0 );
        Mojo plugin = new Mojo();
        plugin.setFailIfNoTests( true );
        e.expect( MojoFailureException.class );
        e.expectMessage( "No tests were executed!  (Set -DfailIfNoTests=false to ignore this error.)" );
        reportExecution( plugin, summary, null, null );
    }

    @Test
    public void shouldHandleTestFailure() throws Exception
    {
        RunResult summary = new RunResult( 1, 0, 1, 0 );
        e.expect( MojoFailureException.class );
        e.expectMessage( "There are test failures.\n\nPlease refer to null "
            + "for the individual test results.\nPlease refer to dump files (if any exist) "
            + "[date].dump, [date]-jvmRun[N].dump and [date].dumpstream." );
        reportExecution( new Mojo(), summary, null, null );
    }

    @Test
    public void failsIfThereAreTooManyFlakes() throws Exception
    {
        RunResult summary = new RunResult( 1, 0, 0, 0, 1 );
        Mojo reportParameters = new Mojo();
        reportParameters.setFailOnFlakeCount( 1 );
        e.expect( MojoFailureException.class );
        e.expectMessage( "There is 1 flake and failOnFlakeCount is set to 1.\n\nPlease refer to null "
            + "for the individual test results.\nPlease refer to dump files (if any exist) "
            + "[date].dump, [date]-jvmRun[N].dump and [date].dumpstream." );
        reportExecution( reportParameters, summary, null, null );
    }

    @Test
    public void reportsFailuresAndFlakes() throws Exception
    {
        RunResult summary = new RunResult( 1, 0, 1, 0, 2 );
        Mojo reportParameters = new Mojo();
        reportParameters.setFailOnFlakeCount( 1 );
        e.expect( MojoFailureException.class );
        e.expectMessage( "There are test failures.\nThere are 2 flakes and failOnFlakeCount is set to 1."
            + "\n\nPlease refer to null "
            + "for the individual test results.\nPlease refer to dump files (if any exist) "
            + "[date].dump, [date]-jvmRun[N].dump and [date].dumpstream." );
        reportExecution( reportParameters, summary, null, null );
    }

    @Test
    public void passesIfFlakesAreWithinThreshold() throws Exception
    {
        RunResult summary = new RunResult( 1, 0, 0, 0 , 1 );
        reportExecution( new Mojo(), summary, null, null );
    }
}
