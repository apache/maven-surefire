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

import org.apache.maven.plugin.surefire.JdkAttributes;
import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.Commandline;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.log.api.NullConsoleLogger;
import org.apache.maven.surefire.booter.ClassLoaderConfiguration;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.ClasspathConfiguration;
import org.apache.maven.surefire.booter.ModularClasspath;
import org.apache.maven.surefire.booter.ModularClasspathConfiguration;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.SurefireBooterForkException;
import org.apache.maven.surefire.extensions.ForkNodeFactory;
import org.apache.maven.surefire.shared.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.lang.String.join;
import static java.nio.file.Files.readAllBytes;
import static java.util.Collections.singletonList;
import static org.apache.maven.surefire.api.util.internal.StringUtils.NL;
import static org.apache.maven.surefire.booter.Classpath.emptyClasspath;
import static org.apache.maven.surefire.booter.ProcessCheckerType.ALL;
import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.apache.commons.io.FileUtils.getTempDirectory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 *
 */
public class ForkConfigurationTest
{
    private static final StartupConfiguration STARTUP_CONFIG = new StartupConfiguration( "",
            new ClasspathConfiguration( true, true ),
            new ClassLoaderConfiguration( true, true ), ALL, Collections.<String[]>emptyList() );

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
    public void testEnv() throws Exception
    {
        Map<String, String> env = new HashMap<>();
        env.put( "key1", "val1" );
        env.put( "key2", "val2" );
        env.put( "key3", "val3" );
        String[] exclEnv = {"PATH"};

        String jvm = new File( new File( System.getProperty( "java.home" ), "bin" ), "java" ).getCanonicalPath();
        Platform platform = new Platform().withJdkExecAttributesForTests( new JdkAttributes( jvm, false ) );

        ForkConfiguration config = new DefaultForkConfiguration( emptyClasspath(), basedir, "", basedir,
            new Properties(), "", env, exclEnv, false, 1, true,
            platform, new NullConsoleLogger(), mock( ForkNodeFactory.class ) )
        {

            @Override
            protected void resolveClasspath( @Nonnull Commandline cli,
                                             @Nonnull String booterThatHasMainMethod,
                                             @Nonnull StartupConfiguration config,
                                             @Nonnull File dumpLogDirectory )
            {

            }
        };

        List<String[]> providerJpmsArgs = new ArrayList<>();
        providerJpmsArgs.add( new String[]{ "arg2", "arg3" } );

        File cpElement = getTempClasspathFile();
        List<String> cp = singletonList( cpElement.getAbsolutePath() );

        ClasspathConfiguration cpConfig = new ClasspathConfiguration( new Classpath( cp ), emptyClasspath(),
            emptyClasspath(), true, true );
        ClassLoaderConfiguration clc = new ClassLoaderConfiguration( true, true );
        StartupConfiguration startup = new StartupConfiguration( "cls", cpConfig, clc, ALL, providerJpmsArgs );

        org.apache.maven.surefire.shared.utils.cli.Commandline
                cli = config.createCommandLine( startup, 1, getTempDirectory() );

        assertThat( cli.getEnvironmentVariables() )
            .contains( "key1=val1", "key2=val2", "key3=val3" )
            .doesNotContain( "PATH=" )
            .doesNotHaveDuplicates();
    }

    @Test
    public void testCliArgs() throws Exception
    {
        String jvm = new File( new File( System.getProperty( "java.home" ), "bin" ), "java" ).getCanonicalPath();
        Platform platform = new Platform().withJdkExecAttributesForTests( new JdkAttributes( jvm, false ) );

        ModularClasspathForkConfiguration config = new ModularClasspathForkConfiguration( emptyClasspath(), basedir,
            "", basedir, new Properties(), "arg1", Collections.<String, String>emptyMap(), new String[0], false, 1,
            true, platform, new NullConsoleLogger(), mock( ForkNodeFactory.class ) );

        assertThat( config.isDebug() ).isFalse();

        List<String[]> providerJpmsArgs = new ArrayList<>();
        providerJpmsArgs.add( new String[]{ "arg2", "arg3" } );

        ModularClasspath modulepath = new ModularClasspath( "test.module", Collections.<String>emptyList(),
            Collections.<String>emptyList(), null, false );
        ModularClasspathConfiguration cpConfig = new ModularClasspathConfiguration( modulepath, emptyClasspath(),
            emptyClasspath(), emptyClasspath(), false, true );
        ClassLoaderConfiguration clc = new ClassLoaderConfiguration( true, true );
        StartupConfiguration startup = new StartupConfiguration( "cls", cpConfig, clc, ALL, providerJpmsArgs );

        org.apache.maven.surefire.shared.utils.cli.Commandline
                cli = config.createCommandLine( startup, 1, getTempDirectory() );
        String cliAsString = cli.toString();

        assertThat( cliAsString )
            .contains( "arg1" );

        // "/path/to/java arg1 @/path/to/argfile"
        int beginOfFileArg = cliAsString.indexOf( '@', cliAsString.lastIndexOf( "arg1" ) );
        assertThat( beginOfFileArg ).isPositive();
        int endOfFileArg = cliAsString.indexOf( IS_OS_WINDOWS ? '"' : '\'', beginOfFileArg );
        assertThat( endOfFileArg ).isPositive();
        Path argFile = Paths.get( cliAsString.substring( beginOfFileArg + 1, endOfFileArg ) );
        String argFileText = new String( readAllBytes( argFile ) );
        assertThat( argFileText )
            .contains( "arg2" )
            .contains( "arg3" )
            .contains( "--add-modules" + NL + "test.module" );
    }

    @Test
    public void testDebugLine() throws Exception
    {
        String jvm = new File( new File( System.getProperty( "java.home" ), "bin" ), "java" ).getCanonicalPath();
        Platform platform = new Platform().withJdkExecAttributesForTests( new JdkAttributes( jvm, false ) );

        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkNodeFactory forkNodeFactory = mock( ForkNodeFactory.class );

        ForkConfiguration config = new DefaultForkConfiguration( emptyClasspath(), basedir,
            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", basedir, new Properties(), "",
            Collections.<String, String>emptyMap(), new String[0], true, 1, true,
            platform, logger, forkNodeFactory )
        {

            @Override
            protected void resolveClasspath( @Nonnull Commandline cli,
                                             @Nonnull String booterThatHasMainMethod,
                                             @Nonnull StartupConfiguration config,
                                             @Nonnull File dumpLogDirectory )
            {

            }
        };

        assertThat( config.isDebug() )
            .isTrue();

        assertThat( config.getDebugLine() )
            .isEqualTo( "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" );

        assertThat( config.getForkCount() )
            .isEqualTo( 1 );

        assertThat( config.isReuseForks() )
            .isTrue();

        assertThat( config.getForkNodeFactory() )
            .isSameAs( forkNodeFactory );

        File cpElement = getTempClasspathFile();
        List<String> cp = singletonList( cpElement.getAbsolutePath() );

        ClasspathConfiguration cpConfig = new ClasspathConfiguration( new Classpath( cp ), emptyClasspath(),
            emptyClasspath(), true, true );
        ClassLoaderConfiguration clc = new ClassLoaderConfiguration( true, true );
        StartupConfiguration startup = new StartupConfiguration( "org.apache.maven.surefire.JUnitProvider#main",
            cpConfig, clc, ALL, Collections.<String[]>emptyList() );

        assertThat( startup.isProviderMainClass() )
            .isTrue();

        assertThat( startup.getProviderClassName() )
            .isEqualTo( "org.apache.maven.surefire.JUnitProvider#main" );

        assertThat( startup.isShadefire() )
            .isFalse();

        org.apache.maven.surefire.shared.utils.cli.Commandline
                cli = config.createCommandLine( startup, 1, getTempDirectory() );

        assertThat( cli.toString() )
            .contains( "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" );
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
        StartupConfiguration startup =
            new StartupConfiguration( "", cpConfig, clc, ALL, Collections.<String[]>emptyList() );

        org.apache.maven.surefire.shared.utils.cli.Commandline
                cli = config.createCommandLine( startup, 1, getTempDirectory() );

        String line = join( " ", cli.getCommandline() );
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
        StartupConfiguration startup =
            new StartupConfiguration( "", cpConfig, clc, ALL, Collections.<String[]>emptyList() );

        org.apache.maven.surefire.shared.utils.cli.Commandline
                commandLine = config.createCommandLine( startup, 1, getTempDirectory() );
        assertThat( commandLine.toString() ).contains( IS_OS_WINDOWS ? "abc def" : "'abc' 'def'" );
    }

    @Test
    public void testCurrentWorkingDirectoryPropagationIncludingForkNumberExpansion()
        throws IOException, SurefireBooterForkException
    {
        File cwd = new File( basedir, "fork_${surefire.forkNumber}" );

        ClasspathConfiguration cpConfig = new ClasspathConfiguration( emptyClasspath(), emptyClasspath(),
                emptyClasspath(), true, true );
        ClassLoaderConfiguration clc = new ClassLoaderConfiguration( true, true );
        StartupConfiguration startup =
            new StartupConfiguration( "", cpConfig, clc, ALL, Collections.<String[]>emptyList() );
        ForkConfiguration config = getForkConfiguration( cwd.getCanonicalFile() );
        org.apache.maven.surefire.shared.utils.cli.Commandline
                commandLine = config.createCommandLine( startup, 1, getTempDirectory() );

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
            config.createCommandLine( STARTUP_CONFIG, 1, getTempDirectory() );
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
            config.createCommandLine( STARTUP_CONFIG, 1, getTempDirectory() );
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

        if ( IS_OS_WINDOWS || isJavaVersionAtLeast7u60() )
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
                platform, new NullConsoleLogger(), mock( ForkNodeFactory.class ) );
    }

    // based on http://stackoverflow.com/questions/2591083/getting-version-of-java-in-runtime
    @SuppressWarnings( "checkstyle:magicnumber" )
    private static boolean isJavaVersionAtLeast7u60()
    {
        String[] javaVersionElements = System.getProperty( "java.runtime.version" ).split( "\\.|_|-b" );
        return Integer.parseInt( javaVersionElements[1] ) >= 7 && Integer.parseInt( javaVersionElements[3] ) >= 60;
    }
}
