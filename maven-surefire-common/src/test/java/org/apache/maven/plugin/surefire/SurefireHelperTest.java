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

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.surefire.AbstractSurefireMojoTest.Mojo;
import org.apache.maven.surefire.suite.RunResult;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.addAll;
import static java.util.Collections.singleton;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.apache.maven.plugin.surefire.SurefireHelper.escapeToPlatformPath;
import static org.apache.maven.plugin.surefire.SurefireHelper.reportExecution;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * Test of {@link SurefireHelper}.
 */
public class SurefireHelperTest
{
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

        assertThat( String.format( SurefireHelper.DUMPSTREAM_FILENAME_FORMATTER, 5) )
                .endsWith( "-jvmRun5.dumpstream" );
    }

    @Test
    public void shouldEscapeWindowsPath()
    {
        assumeTrue( IS_OS_WINDOWS );
        String root = "X:\\path\\to\\project\\";
        String pathToJar = "target\\surefire\\surefirebooter4942721306300108667.jar";
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
    public void shouldHandleFailIfNoTests() throws Exception
    {
        RunResult summary = new RunResult( 0, 0, 0, 0 );
        try
        {
            Mojo plugin = new Mojo();
            plugin.setFailIfNoTests( true );
            reportExecution( plugin, summary, null, null );
        }
        catch ( MojoFailureException e )
        {
            assertThat( e.getLocalizedMessage() )
                    .isEqualTo( "No tests were executed!  (Set -DfailIfNoTests=false to ignore this error.)" );
            return;
        }
        fail( "Expected MojoFailureException with message "
                + "'No tests were executed!  (Set -DfailIfNoTests=false to ignore this error.)'" );
    }

    @Test
    public void shouldHandleTestFailure() throws Exception
    {
        RunResult summary = new RunResult( 1, 0, 1, 0 );
        try
        {
            reportExecution( new Mojo(), summary, null, null );
        }
        catch ( MojoFailureException e )
        {
            assertThat( e.getLocalizedMessage() )
                    .isEqualTo( "There are test failures.\n\nPlease refer to null "
                            + "for the individual test results.\nPlease refer to dump files (if any exist) "
                            + "[date].dump, [date]-jvmRun[N].dump and [date].dumpstream." );
            return;
        }
        fail( "Expected MojoFailureException with message "
                + "'There are test failures.\n\nPlease refer to null "
                + "for the individual test results.\nPlease refer to dump files (if any exist) "
                + "[date].dump, [date]-jvmRun[N].dump and [date].dumpstream.'");
    }
}
