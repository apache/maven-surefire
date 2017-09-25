package org.apache.maven.plugin.surefire.booterclient;

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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.surefire.JdkAttributes;
import org.apache.maven.shared.utils.StringUtils;
import org.apache.maven.shared.utils.cli.Commandline;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.SurefireBooterForkException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ForkConfigurationTest
{
    @Test
    public void testCreateCommandLine_UseSystemClassLoaderForkOnce_ShouldConstructManifestOnlyJar()
        throws IOException, SurefireBooterForkException
    {
        ForkConfiguration config = getForkConfiguration( (String) null );
        File cpElement = getTempClasspathFile();

        Commandline cli =
            config.createCommandLine( Collections.singletonList( cpElement.getAbsolutePath() ), true, false, null, 1 );

        String line = StringUtils.join( cli.getCommandline(), " " );
        assertTrue( line.contains( "-jar" ) );
    }

    @Test
    public void testArglineWithNewline()
        throws IOException, SurefireBooterForkException
    {
        // SUREFIRE-657
        File cpElement = getTempClasspathFile();
        ForkConfiguration forkConfiguration = getForkConfiguration( "abc\ndef" );

        final Commandline commandLine =
            forkConfiguration.createCommandLine( Collections.singletonList( cpElement.getAbsolutePath() ), false, false,
                                                 null, 1 );
        assertTrue( commandLine.toString().contains( "abc def" ) );
    }

    @Test
    public void testCurrentWorkingDirectoryPropagationIncludingForkNumberExpansion()
        throws IOException, SurefireBooterForkException
    {
        // SUREFIRE-1136
        File baseDir =
            new File( FileUtils.getTempDirectory(), "SUREFIRE-1136-" + RandomStringUtils.randomAlphabetic( 3 ) );
        assertTrue( baseDir.mkdirs() );
        baseDir.deleteOnExit();

        File cwd = new File( baseDir, "fork_${surefire.forkNumber}" );

        ForkConfiguration config = getForkConfiguration( null, cwd.getCanonicalFile() );
        Commandline commandLine = config.createCommandLine( Collections.<String>emptyList(), true, false, null, 1 );

        File forkDirectory = new File( baseDir, "fork_1" );
        forkDirectory.deleteOnExit();
        assertTrue( forkDirectory.getCanonicalPath().equals(
            commandLine.getShell().getWorkingDirectory().getCanonicalPath() ) );
    }

    @Test
    public void testExceptionWhenCurrentDirectoryIsNotRealDirectory()
        throws IOException, SurefireBooterForkException
    {
        // SUREFIRE-1136
        File baseDir =
            new File( FileUtils.getTempDirectory(), "SUREFIRE-1136-" + RandomStringUtils.randomAlphabetic( 3 ) );
        assertTrue( baseDir.mkdirs() );
        baseDir.deleteOnExit();

        File cwd = new File( baseDir, "cwd.txt" );
        FileUtils.touch( cwd );
        cwd.deleteOnExit();

        ForkConfiguration config = getForkConfiguration( null, cwd.getCanonicalFile() );

        try
        {
            config.createCommandLine( Collections.<String>emptyList(), true, false, null, 1 );
        }
        catch ( SurefireBooterForkException sbfe )
        {
            // To handle issue with ~ expansion on Windows
            String absolutePath = cwd.getCanonicalPath();
            assertEquals( "WorkingDirectory " + absolutePath + " exists and is not a directory", sbfe.getMessage() );
            return;
        }

        fail();
    }

    @Test
    public void testExceptionWhenCurrentDirectoryCannotBeCreated()
        throws IOException, SurefireBooterForkException
    {
        // SUREFIRE-1136
        File baseDir =
            new File( FileUtils.getTempDirectory(), "SUREFIRE-1136-" + RandomStringUtils.randomAlphabetic( 3 ) );
        assertTrue( baseDir.mkdirs() );
        baseDir.deleteOnExit();

        // NULL is invalid for JDK starting from 1.7.60 - https://github.com/openjdk-mirror/jdk/commit/e5389115f3634d25d101e2dcc71f120d4fd9f72f
        // ? character is invalid on Windows, seems to be imposable to create invalid directory using Java on Linux
        File cwd = new File( baseDir, "?\u0000InvalidDirectoryName" );
        ForkConfiguration config = getForkConfiguration( null, cwd.getAbsoluteFile() );

        try
        {
            config.createCommandLine( Collections.<String>emptyList(), true, false, null, 1 );
        }
        catch ( SurefireBooterForkException sbfe )
        {
            assertEquals( "Cannot create workingDirectory " + cwd.getAbsolutePath(), sbfe.getMessage() );
            return;
        }

        if ( SystemUtils.IS_OS_WINDOWS || isJavaVersionAtLeast( 7, 60 ) )
        {
            fail();
        }
    }

    private File getTempClasspathFile()
        throws IOException
    {
        File cpElement = File.createTempFile( "ForkConfigurationTest.", ".file" );
        cpElement.deleteOnExit();
        return cpElement;
    }

    public static ForkConfiguration getForkConfiguration( File javaExec )
            throws IOException
    {
        return getForkConfiguration( null, javaExec.getAbsolutePath(), new File( "." ).getCanonicalFile() );
    }

    public static ForkConfiguration getForkConfiguration( String argLine )
        throws IOException
    {
        File jvm = new File( new File( System.getProperty( "java.home" ), "bin" ), "java" );
        return getForkConfiguration( argLine, jvm.getAbsolutePath(), new File( "." ).getCanonicalFile() );
    }

    public static ForkConfiguration getForkConfiguration( String argLine, File cwd )
            throws IOException
    {
        File jvm = new File( new File( System.getProperty( "java.home" ), "bin" ), "java" );
        return getForkConfiguration( argLine, jvm.getAbsolutePath(), cwd );
    }

    private static ForkConfiguration getForkConfiguration( String argLine, String jvm, File cwd )
        throws IOException
    {
        return new ForkConfiguration( Classpath.emptyClasspath(), null, null, new JdkAttributes( jvm, false ),
                                            cwd, new Properties(), argLine, null, false, 1, false, new Platform() );
    }

    // based on http://stackoverflow.com/questions/2591083/getting-version-of-java-in-runtime
    private boolean isJavaVersionAtLeast( int major, int update )
    {
        String[] javaVersionElements = System.getProperty( "java.runtime.version" ).split( "\\.|_|-b" );
        return Integer.valueOf( javaVersionElements[1] ) >= major
            && Integer.valueOf( javaVersionElements[3] ) >= update;
    }
}
