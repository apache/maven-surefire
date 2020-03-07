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

import org.apache.maven.surefire.shared.io.FileUtils;
import org.apache.maven.surefire.shared.lang3.SystemUtils;
import org.apache.maven.plugin.surefire.JdkAttributes;
import org.apache.maven.plugin.surefire.log.api.NullConsoleLogger;
import org.apache.maven.surefire.shared.utils.StringUtils;
import org.apache.maven.surefire.shared.utils.cli.Commandline;
import org.apache.maven.surefire.booter.ClassLoaderConfiguration;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.ClasspathConfiguration;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.SurefireBooterForkException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static java.util.Collections.singletonList;
import static org.apache.maven.surefire.booter.Classpath.emptyClasspath;
import static org.apache.maven.surefire.booter.ProcessCheckerType.ALL;
import static org.fest.util.Files.temporaryFolder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 */
public class ForkConfigurationTest
{
    private static final StartupConfiguration STARTUP_CONFIG = new StartupConfiguration( "",
            new ClasspathConfiguration( true, true ),
            new ClassLoaderConfiguration( true, true ),
            false,
            false,
            ALL );

    private static int idx = 0;

    private File basedir;

    @Before
    public void setupDirectories() throws IOException
    {
        File target = new File( System.getProperty( "user.dir" ), "target" );
        basedir = new File( target, "SUREFIRE-1136-" + ++idx );
        FileUtils.deleteDirectory( basedir );
        assertTrue( basedir.mkdirs() );
    }

    @After
    public void deleteDirectories() throws IOException
    {
        FileUtils.deleteDirectory( basedir );
    }

    @Test
    @SuppressWarnings( { "checkstyle:methodname", "checkstyle:magicnumber" } )
    public void testCreateCommandLine_UseSystemClassLoaderForkOnce_ShouldConstructManifestOnlyJar()
        throws IOException, SurefireBooterForkException
    {
        ForkConfiguration config = getForkConfiguration( basedir, null );
        File cpElement = getTempClasspathFile();

        List<String> cp = singletonList( cpElement.getAbsolutePath() );
        ClasspathConfiguration cpConfig = new ClasspathConfiguration( new Classpath( cp ), emptyClasspath(),
                emptyClasspath(), true, true );
        ClassLoaderConfiguration clc = new ClassLoaderConfiguration( true, true );
        StartupConfiguration startup = new StartupConfiguration( "", cpConfig, clc, false, false, ALL );

        Commandline cli = config.createCommandLine( startup, 1, temporaryFolder() );

        String line = StringUtils.join( cli.getCommandline(), " " );
        assertTrue( line.contains( "-jar" ) );
    }

    @Test
    public void testArglineWithNewline()
        throws IOException, SurefireBooterForkException
    {
        // SUREFIRE-657
        ForkConfiguration config = getForkConfiguration( basedir, "abc\ndef" );
        File cpElement = getTempClasspathFile();

        List<String> cp = singletonList( cpElement.getAbsolutePath() );
        ClasspathConfiguration cpConfig = new ClasspathConfiguration( new Classpath( cp ), emptyClasspath(),
                emptyClasspath(), true, true );
        ClassLoaderConfiguration clc = new ClassLoaderConfiguration( true, true );
        StartupConfiguration startup = new StartupConfiguration( "", cpConfig, clc, false, false, ALL );

        Commandline commandLine = config.createCommandLine( startup, 1, temporaryFolder() );
        assertTrue( commandLine.toString().contains( "abc def" ) );
    }

    @Test
    public void testCurrentWorkingDirectoryPropagationIncludingForkNumberExpansion()
        throws IOException, SurefireBooterForkException
    {
        File cwd = new File( basedir, "fork_${surefire.forkNumber}" );

        ClasspathConfiguration cpConfig = new ClasspathConfiguration( emptyClasspath(), emptyClasspath(),
                emptyClasspath(), true, true );
        ClassLoaderConfiguration clc = new ClassLoaderConfiguration( true, true );
        StartupConfiguration startup = new StartupConfiguration( "", cpConfig, clc, false, false, ALL );
        ForkConfiguration config = getForkConfiguration( cwd.getCanonicalFile() );
        Commandline commandLine = config.createCommandLine( startup, 1, temporaryFolder() );

        File forkDirectory = new File( basedir, "fork_1" );

        String shellWorkDir = commandLine.getShell().getWorkingDirectory().getCanonicalPath();
        assertEquals( shellWorkDir,  forkDirectory.getCanonicalPath() );
    }

    @Test
    public void testExceptionWhenCurrentDirectoryIsNotRealDirectory()
        throws IOException
    {
        File cwd = new File( basedir, "cwd.txt" );
        FileUtils.touch( cwd );

        try
        {
            ForkConfiguration config = getForkConfiguration( cwd.getCanonicalFile() );
            config.createCommandLine( STARTUP_CONFIG, 1, temporaryFolder() );
        }
        catch ( SurefireBooterForkException e )
        {
            // To handle issue with ~ expansion on Windows
            String absolutePath = cwd.getCanonicalPath();
            assertEquals( "WorkingDirectory " + absolutePath + " exists and is not a directory", e.getMessage() );
            return;
        }
        finally
        {
            assertTrue( cwd.delete() );
        }

        fail();
    }

    @Test
    public void testExceptionWhenCurrentDirectoryCannotBeCreated()
        throws IOException
    {
        // NULL is invalid for JDK starting from 1.7.60
        // - https://github.com/openjdk-mirror/jdk/commit/e5389115f3634d25d101e2dcc71f120d4fd9f72f
        // ? character is invalid on Windows, seems to be imposable to create invalid directory using Java on Linux
        File cwd = new File( basedir, "?\u0000InvalidDirectoryName" );

        try
        {
            ForkConfiguration config = getForkConfiguration( cwd.getAbsoluteFile() );
            config.createCommandLine( STARTUP_CONFIG, 1, temporaryFolder() );
        }
        catch ( SurefireBooterForkException sbfe )
        {
            assertEquals( "Cannot create workingDirectory " + cwd.getAbsolutePath(), sbfe.getMessage() );
            return;
        }
        finally
        {
            FileUtils.deleteDirectory( cwd );
        }

        if ( SystemUtils.IS_OS_WINDOWS || isJavaVersionAtLeast7u60() )
        {
            fail();
        }
    }

    private File getTempClasspathFile()
        throws IOException
    {
        File cpElement = new File( basedir, "ForkConfigurationTest." + idx + ".file" );
        FileUtils.deleteDirectory( cpElement );
        return cpElement;
    }

    static ForkConfiguration getForkConfiguration( File basedir, String argLine )
        throws IOException
    {
        File jvm = new File( new File( System.getProperty( "java.home" ), "bin" ), "java" );
        return getForkConfiguration( basedir, argLine, jvm.getAbsolutePath(), new File( "." ).getCanonicalFile() );
    }

    private ForkConfiguration getForkConfiguration( File cwd )
            throws IOException
    {
        File jvm = new File( new File( System.getProperty( "java.home" ), "bin" ), "java" );
        return getForkConfiguration( basedir, null, jvm.getAbsolutePath(), cwd );
    }

    private static ForkConfiguration getForkConfiguration( File basedir, String argLine, String jvm, File cwd )
        throws IOException
    {
        Platform platform = new Platform().withJdkExecAttributesForTests( new JdkAttributes( jvm, false ) );
        File tmpDir = new File( new File( basedir, "target" ), "surefire" );
        FileUtils.deleteDirectory( tmpDir );
        assertTrue( tmpDir.mkdirs() );
        return new JarManifestForkConfiguration( emptyClasspath(), tmpDir, null,
                cwd, new Properties(), argLine,
                Collections.<String, String>emptyMap(), new String[0], false, 1, false,
                platform, new NullConsoleLogger() );
    }

    // based on http://stackoverflow.com/questions/2591083/getting-version-of-java-in-runtime
    @SuppressWarnings( "checkstyle:magicnumber" )
    private static boolean isJavaVersionAtLeast7u60()
    {
        String[] javaVersionElements = System.getProperty( "java.runtime.version" ).split( "\\.|_|-b" );
        return Integer.valueOf( javaVersionElements[1] ) >= 7 && Integer.valueOf( javaVersionElements[3] ) >= 60;
    }
}
