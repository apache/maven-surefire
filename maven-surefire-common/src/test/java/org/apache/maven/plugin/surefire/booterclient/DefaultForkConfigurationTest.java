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
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.util.Relocator;
import org.apache.maven.surefire.booter.ClassLoaderConfiguration;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.ClasspathConfiguration;
import org.apache.maven.surefire.booter.ForkedBooter;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.SurefireBooterForkException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static java.util.Collections.singleton;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.*;
import static org.powermock.reflect.Whitebox.invokeMethod;

/**
 * Unit tests for {@link DefaultForkConfiguration}.
 *
 * @author Tibor Digana (tibor17)
 * @since 2.21
 */
@RunWith( PowerMockRunner.class )
@PrepareForTest( { DefaultForkConfiguration.class, Relocator.class } )
@PowerMockIgnore( { "org.jacoco.agent.rt.*", "com.vladium.emma.rt.*" } )
public class DefaultForkConfigurationTest
{
    private Classpath booterClasspath;
    private File tempDirectory;
    private String debugLine;
    private File workingDirectory;
    private Properties modelProperties;
    private String argLine;
    private Map<String, String> environmentVariables;
    private boolean debug;
    private int forkCount;
    private boolean reuseForks;
    private Platform pluginPlatform;
    private ConsoleLogger log;

    @Before
    public void setup()
    {
        booterClasspath = new Classpath( singleton( "provider.jar" ) );
        tempDirectory = new File( "target/surefire" );
        debugLine = "";
        workingDirectory = new File( "." );
        modelProperties = new Properties();
        argLine = null;
        environmentVariables = new HashMap<>();
        debug = true;
        forkCount = 2;
        reuseForks = true;
        pluginPlatform = new Platform();
        log = mock( ConsoleLogger.class );
    }

    @Test
    public void shouldBeNullArgLine() throws Exception
    {
        DefaultForkConfiguration config = new DefaultForkConfiguration( booterClasspath, tempDirectory, debugLine,
                workingDirectory, modelProperties, argLine, environmentVariables, debug, forkCount, reuseForks,
                pluginPlatform, log )
        {

            @Override
            protected void resolveClasspath( @Nonnull OutputStreamFlushableCommandline cli,
                                             @Nonnull String booterThatHasMainMethod,
                                             @Nonnull StartupConfiguration config,
                                             @Nonnull File dumpLogDirectory ) throws SurefireBooterForkException
            {

            }
        };

        DefaultForkConfiguration mockedConfig = spy( config );
        String newArgLine = invokeMethod( mockedConfig, "newJvmArgLine", new Class[] { int.class }, 2 );
        verifyPrivate( mockedConfig, times( 1 ) ).invoke( "interpolateArgLineWithPropertyExpressions" );
        verifyPrivate( mockedConfig, times( 1 ) ).invoke( "extendJvmArgLine", eq( "" ) );
        assertThat( newArgLine ).isEmpty();
    }

    @Test
    public void shouldBeEmptyArgLine() throws Exception
    {
        argLine = "";
        DefaultForkConfiguration config = new DefaultForkConfiguration( booterClasspath, tempDirectory, debugLine,
                workingDirectory, modelProperties, argLine, environmentVariables, debug, forkCount, reuseForks,
                pluginPlatform, log )
        {

            @Override
            protected void resolveClasspath( @Nonnull OutputStreamFlushableCommandline cli,
                                             @Nonnull String booterThatHasMainMethod,
                                             @Nonnull StartupConfiguration config,
                                             @Nonnull File dumpLogDirectory ) throws SurefireBooterForkException
            {

            }
        };

        DefaultForkConfiguration mockedConfig = spy( config );
        String newArgLine = invokeMethod( mockedConfig, "newJvmArgLine", new Class[] { int.class }, 2 );
        verifyPrivate( mockedConfig, times( 1 ) ).invoke( "interpolateArgLineWithPropertyExpressions" );
        verifyPrivate( mockedConfig, times( 1 ) ).invoke( "extendJvmArgLine", eq( "" ) );
        assertThat( newArgLine ).isEmpty();
    }

    @Test
    public void shouldBeEmptyArgLineInsteadOfNewLines() throws Exception
    {
        argLine = "\n\r";
        DefaultForkConfiguration config = new DefaultForkConfiguration( booterClasspath, tempDirectory, debugLine,
                workingDirectory, modelProperties, argLine, environmentVariables, debug, forkCount, reuseForks,
                pluginPlatform, log )
        {

            @Override
            protected void resolveClasspath( @Nonnull OutputStreamFlushableCommandline cli,
                                             @Nonnull String booterThatHasMainMethod,
                                             @Nonnull StartupConfiguration config,
                                             @Nonnull File dumpLogDirectory ) throws SurefireBooterForkException
            {

            }
        };

        DefaultForkConfiguration mockedConfig = spy( config );
        String newArgLine = invokeMethod( mockedConfig, "newJvmArgLine", new Class[] { int.class }, 2 );
        verifyPrivate( mockedConfig, times( 1 ) ).invoke( "interpolateArgLineWithPropertyExpressions" );
        verifyPrivate( mockedConfig, times( 1 ) ).invoke( "extendJvmArgLine", eq( "" ) );
        assertThat( newArgLine ).isEmpty();
    }

    @Test
    public void shouldBeWithoutEscaping() throws Exception
    {
        argLine = "-Dfile.encoding=UTF-8";
        DefaultForkConfiguration config = new DefaultForkConfiguration( booterClasspath, tempDirectory, debugLine,
                workingDirectory, modelProperties, argLine, environmentVariables, debug, forkCount, reuseForks,
                pluginPlatform, log )
        {

            @Override
            protected void resolveClasspath( @Nonnull OutputStreamFlushableCommandline cli,
                                             @Nonnull String booterThatHasMainMethod,
                                             @Nonnull StartupConfiguration config,
                                             @Nonnull File dumpLogDirectory ) throws SurefireBooterForkException
            {

            }
        };

        DefaultForkConfiguration mockedConfig = spy( config );
        String newArgLine = invokeMethod( mockedConfig, "newJvmArgLine", new Class[] { int.class }, 2 );
        verifyPrivate( mockedConfig, times( 1 ) ).invoke( "interpolateArgLineWithPropertyExpressions" );
        verifyPrivate( mockedConfig, times( 1 ) ).invoke( "extendJvmArgLine", eq( "-Dfile.encoding=UTF-8" ) );
        assertThat( newArgLine ).isEqualTo( "-Dfile.encoding=UTF-8" );
    }

    @Test
    public void shouldBeWithEscaping() throws Exception
    {
        modelProperties.put( "encoding", "UTF-8" );
        argLine = "-Dfile.encoding=@{encoding}";
        DefaultForkConfiguration config = new DefaultForkConfiguration( booterClasspath, tempDirectory, debugLine,
                workingDirectory, modelProperties, argLine, environmentVariables, debug, forkCount, reuseForks,
                pluginPlatform, log )
        {

            @Override
            protected void resolveClasspath( @Nonnull OutputStreamFlushableCommandline cli,
                                             @Nonnull String booterThatHasMainMethod,
                                             @Nonnull StartupConfiguration config,
                                             @Nonnull File dumpLogDirectory ) throws SurefireBooterForkException
            {

            }
        };

        DefaultForkConfiguration mockedConfig = spy( config );
        String newArgLine = invokeMethod( mockedConfig, "newJvmArgLine", new Class[] { int.class }, 2 );
        verifyPrivate( mockedConfig, times( 1 ) ).invoke( "interpolateArgLineWithPropertyExpressions" );
        verifyPrivate( mockedConfig, times( 1 ) ).invoke( "extendJvmArgLine", eq( "-Dfile.encoding=UTF-8" ) );
        assertThat( newArgLine ).isEqualTo( "-Dfile.encoding=UTF-8" );
    }

    @Test
    public void shouldBeWhitespaceInsteadOfNewLines() throws Exception
    {
        argLine = "a\n\rb";
        DefaultForkConfiguration config = new DefaultForkConfiguration( booterClasspath, tempDirectory, debugLine,
                workingDirectory, modelProperties, argLine, environmentVariables, debug, forkCount, reuseForks,
                pluginPlatform, log )
        {

            @Override
            protected void resolveClasspath( @Nonnull OutputStreamFlushableCommandline cli,
                                             @Nonnull String booterThatHasMainMethod,
                                             @Nonnull StartupConfiguration config,
                                             @Nonnull File dumpLogDirectory ) throws SurefireBooterForkException
            {

            }
        };

        DefaultForkConfiguration mockedConfig = spy( config );
        String newArgLine = invokeMethod( mockedConfig, "newJvmArgLine", new Class[] { int.class }, 2 );
        verifyPrivate( mockedConfig, times( 1 ) ).invoke( "interpolateArgLineWithPropertyExpressions" );
        verifyPrivate( mockedConfig, times( 1 ) ).invoke( "extendJvmArgLine", eq( "a  b" ) );
        assertThat( newArgLine ).isEqualTo( "a  b" );
    }

    @Test
    public void shouldEscapeThreadNumber() throws Exception
    {
        argLine = "-Dthread=${surefire.threadNumber}";
        DefaultForkConfiguration config = new DefaultForkConfiguration( booterClasspath, tempDirectory, debugLine,
                workingDirectory, modelProperties, argLine, environmentVariables, debug, forkCount, reuseForks,
                pluginPlatform, log )
        {

            @Override
            protected void resolveClasspath( @Nonnull OutputStreamFlushableCommandline cli,
                                             @Nonnull String booterThatHasMainMethod,
                                             @Nonnull StartupConfiguration config,
                                             @Nonnull File dumpLogDirectory ) throws SurefireBooterForkException
            {

            }
        };

        DefaultForkConfiguration mockedConfig = spy( config );
        String newArgLine = invokeMethod( mockedConfig, "newJvmArgLine", new Class[] { int.class }, 2 );
        verifyPrivate( mockedConfig, times( 1 ) ).invoke( "interpolateArgLineWithPropertyExpressions" );
        verifyPrivate( mockedConfig, times( 1 ) ).invoke( "extendJvmArgLine", eq( "-Dthread=" + forkCount ) );
        assertThat( newArgLine ).isEqualTo( "-Dthread=" + forkCount );
    }

    @Test
    public void shouldEscapeForkNumber() throws Exception
    {
        argLine = "-Dthread=${surefire.forkNumber}";
        DefaultForkConfiguration config = new DefaultForkConfiguration( booterClasspath, tempDirectory, debugLine,
                workingDirectory, modelProperties, argLine, environmentVariables, debug, forkCount, reuseForks,
                pluginPlatform, log )
        {

            @Override
            protected void resolveClasspath( @Nonnull OutputStreamFlushableCommandline cli,
                                             @Nonnull String booterThatHasMainMethod,
                                             @Nonnull StartupConfiguration config,
                                             @Nonnull File dumpLogDirectory ) throws SurefireBooterForkException
            {

            }
        };

        DefaultForkConfiguration mockedConfig = spy( config );
        String newArgLine = invokeMethod( mockedConfig, "newJvmArgLine", new Class[] { int.class }, 2 );
        verifyPrivate( mockedConfig, times( 1 ) ).invoke( "interpolateArgLineWithPropertyExpressions" );
        verifyPrivate( mockedConfig, times( 1 ) ).invoke( "extendJvmArgLine", eq( "-Dthread=" + forkCount ) );
        assertThat( newArgLine ).isEqualTo( "-Dthread=" + forkCount );
    }

    @Test
    public void shouldRelocateBooterClassWhenShadefire() throws Exception
    {
        ClassLoaderConfiguration clc = new ClassLoaderConfiguration( true, true );
        ClasspathConfiguration cc = new ClasspathConfiguration( true, true );
        StartupConfiguration conf =
                new StartupConfiguration( "org.apache.maven.shadefire.surefire.MyProvider", cc, clc, false, false );
        StartupConfiguration confMock = spy( conf );
        mockStatic( Relocator.class );
        when( Relocator.relocate( anyString() ) ).thenCallRealMethod();

        String cls = invokeMethod( DefaultForkConfiguration.class, "findStartClass", confMock );

        verify( confMock, times( 1 ) ).isShadefire();
        verifyStatic( Relocator.class, times( 1 ) );
        Relocator.relocate( eq( ForkedBooter.class.getName() ) );

        assertThat( cls ).isEqualTo( "org.apache.maven.shadefire.surefire.booter.ForkedBooter" );
        assertThat( confMock.isShadefire() ).isTrue();
    }

    @Test
    public void shouldNotRelocateBooterClass() throws Exception
    {
        ClassLoaderConfiguration clc = new ClassLoaderConfiguration( true, true );
        ClasspathConfiguration cc = new ClasspathConfiguration( true, true );
        StartupConfiguration conf =
                new StartupConfiguration( "org.apache.maven.surefire.MyProvider", cc, clc, false, false );
        StartupConfiguration confMock = spy( conf );
        mockStatic( Relocator.class );
        when( Relocator.relocate( anyString() ) ).thenCallRealMethod();

        String cls = invokeMethod( DefaultForkConfiguration.class, "findStartClass", confMock );

        verify( confMock, times( 1 ) ).isShadefire();
        verifyStatic( Relocator.class, never() );
        Relocator.relocate( eq( ForkedBooter.class.getName() ) );

        assertThat( cls ).isEqualTo( "org.apache.maven.surefire.booter.ForkedBooter" );
        assertThat( confMock.isShadefire() ).isFalse();
    }
}
