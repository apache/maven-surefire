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

import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.OutputStreamFlushableCommandline;
import org.apache.maven.plugin.surefire.log.api.NullConsoleLogger;
import org.apache.maven.surefire.booter.ClassLoaderConfiguration;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.ForkedBooter;
import org.apache.maven.surefire.booter.ModularClasspath;
import org.apache.maven.surefire.booter.ModularClasspathConfiguration;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import static java.io.File.createTempFile;
import static java.io.File.separator;
import static java.io.File.pathSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllLines;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.fest.assertions.Assertions.assertThat;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.21.0.Jigsaw
 */
public class ModularClasspathForkConfigurationTest
{
    @Test
    @SuppressWarnings( "ResultOfMethodCallIgnored" )
    public void shouldCreateModularArgsFile() throws Exception
    {
        Classpath booter = new Classpath( asList( "booter.jar", "non-modular.jar" ) );
        File target = new File( "target" ).getCanonicalFile();
        File tmp = new File( target, "surefire" );
        tmp.mkdirs();
        File pwd = new File( "." ).getCanonicalFile();

        ModularClasspathForkConfiguration config = new ModularClasspathForkConfiguration( booter, tmp, "", pwd,
                new Properties(), "", new HashMap<String, String>(), true, 1, true, new Platform(),
                new NullConsoleLogger() )
        {
            @Nonnull
            @Override
            String toModuleName( @Nonnull File moduleDescriptor )
            {
                return "abc";
            }
        };

        File patchFile = new File( "target" + separator + "test-classes" );
        File descriptor = new File( tmp, "module-info.class" );
        descriptor.createNewFile();
        List<String> modulePath = asList( "modular.jar", "target/classes" );
        List<String> classPath = asList( "booter.jar", "non-modular.jar", patchFile.getPath() );
        Collection<String> packages = singleton( "org.apache.abc" );
        String startClassName = ForkedBooter.class.getName();

        File jigsawArgsFile =
                config.createArgsFile( descriptor, modulePath, classPath, packages, patchFile, startClassName );

        assertThat( jigsawArgsFile ).isNotNull();
        List<String> argsFileLines = readAllLines( jigsawArgsFile.toPath(), UTF_8 );
        assertThat( argsFileLines ).hasSize( 13 );
        assertThat( argsFileLines.get( 0 ) ).isEqualTo( "--module-path" );
        assertThat( argsFileLines.get( 1 ) ).isEqualTo( "modular.jar" + pathSeparator + "target/classes" );
        assertThat( argsFileLines.get( 2 ) ).isEqualTo( "--class-path" );
        assertThat( argsFileLines.get( 3 ) )
                .isEqualTo( "booter.jar" + pathSeparator + "non-modular.jar" + pathSeparator + patchFile.getPath() );
        assertThat( argsFileLines.get( 4 ) ).isEqualTo( "--patch-module" );
        assertThat( argsFileLines.get( 5 ) ).isEqualTo( "abc=" + patchFile.getPath() );
        assertThat( argsFileLines.get( 6 ) ).isEqualTo( "--add-exports" );
        assertThat( argsFileLines.get( 7 ) ).isEqualTo( "abc/org.apache.abc=ALL-UNNAMED" );
        assertThat( argsFileLines.get( 8 ) ).isEqualTo( "--add-modules" );
        assertThat( argsFileLines.get( 9 ) ).isEqualTo( "abc" );
        assertThat( argsFileLines.get( 10 ) ).isEqualTo( "--add-reads" );
        assertThat( argsFileLines.get( 11 ) ).isEqualTo( "abc=ALL-UNNAMED" );
        assertThat( argsFileLines.get( 12 ) ).isEqualTo( ForkedBooter.class.getName() );

        ModularClasspath modularClasspath = new ModularClasspath( descriptor, modulePath, packages, patchFile );
        Classpath testClasspathUrls = new Classpath( singleton( "target" + separator + "test-classes" ) );
        Classpath surefireClasspathUrls = Classpath.emptyClasspath();
        ModularClasspathConfiguration modularClasspathConfiguration =
                new ModularClasspathConfiguration( modularClasspath, testClasspathUrls, surefireClasspathUrls,
                        true, true );
        ClassLoaderConfiguration clc = new ClassLoaderConfiguration( true, true );
        StartupConfiguration startupConfiguration =
                new StartupConfiguration( "JUnitCoreProvider", modularClasspathConfiguration, clc, true, true );
        OutputStreamFlushableCommandline cli = new OutputStreamFlushableCommandline();
        config.resolveClasspath( cli, ForkedBooter.class.getName(), startupConfiguration,
                createTempFile( "surefire", "surefire-reports" ) );

        assertThat( cli.getArguments() ).isNotNull();
        assertThat( cli.getArguments() ).hasSize( 1 );
        assertThat( cli.getArguments()[0] ).startsWith( "@" );
        File argFile = new File( cli.getArguments()[0].substring( 1 ) );
        assertThat( argFile ).isFile();
        List<String> argsFileLines2 = readAllLines( argFile.toPath(), UTF_8 );
        assertThat( argsFileLines2 ).hasSize( 13 );
        for ( int i = 0; i < argsFileLines2.size(); i++ )
        {
            String line = argsFileLines2.get( i );
            assertThat( line ).isEqualTo( argsFileLines.get( i ) );
        }
    }
}
