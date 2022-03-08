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

import org.apache.maven.plugin.surefire.StartupReportConfiguration;
import org.apache.maven.plugin.surefire.SurefireProperties;
import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.AbstractCommandReader;
import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.Commandline;
import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.TestLessInputStream;
import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.TestLessInputStream.TestLessInputStreamBuilder;
import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.TestProvidingInputStream;
import org.apache.maven.plugin.surefire.booterclient.output.ForkClient;
import org.apache.maven.plugin.surefire.extensions.LegacyForkNodeFactory;
import org.apache.maven.plugin.surefire.extensions.SurefireConsoleOutputReporter;
import org.apache.maven.plugin.surefire.extensions.SurefireStatelessReporter;
import org.apache.maven.plugin.surefire.extensions.SurefireStatelessTestsetInfoReporter;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.report.DefaultReporterFactory;
import org.apache.maven.surefire.booter.AbstractPathConfiguration;
import org.apache.maven.surefire.booter.ClassLoaderConfiguration;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.PropertiesWrapper;
import org.apache.maven.surefire.booter.ProviderConfiguration;
import org.apache.maven.surefire.api.booter.Shutdown;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.SurefireBooterForkException;
import org.apache.maven.surefire.extensions.ForkNodeFactory;
import org.apache.maven.surefire.api.report.ReporterConfiguration;
import org.apache.maven.surefire.shared.compress.archivers.zip.Zip64Mode;
import org.apache.maven.surefire.shared.compress.archivers.zip.ZipArchiveEntry;
import org.apache.maven.surefire.shared.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.jar.Manifest;
import java.util.zip.Deflater;

import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.reflect.Whitebox.invokeMethod;

/**
 *
 */
public class ForkStarterTest
{
    private static String baseDir = System.getProperty( "user.dir" );
    private static File tmp;

    @Rule
    public final ExpectedException e = ExpectedException.none();

    @BeforeClass
    public static void prepareFiles() throws IOException
    {
        File target = new File( baseDir, "target" );
        tmp = new File( target, "tmp" );
        tmp.mkdirs();
        File booter = new File( tmp, "surefirebooter.jar" );
        booter.createNewFile();

        try ( ZipArchiveOutputStream zos = new ZipArchiveOutputStream( new FileOutputStream( booter ) ) )
        {
            zos.setUseZip64( Zip64Mode.Never );
            zos.setLevel( Deflater.NO_COMPRESSION );

            ZipArchiveEntry ze = new ZipArchiveEntry( "META-INF/MANIFEST.MF" );
            zos.putArchiveEntry( ze );

            Manifest man = new Manifest();

            man.getMainAttributes().putValue( "Manifest-Version", "1.0" );
            man.getMainAttributes().putValue( "Main-Class", MainClass.class.getName() );

            man.write( zos );

            zos.closeArchiveEntry();

            ze = new ZipArchiveEntry( "org/apache/maven/plugin/surefire/booterclient/MainClass.class" );
            zos.putArchiveEntry( ze );
            String classesDir = Paths.get( target.getPath(), "test-classes" ).toString();
            Path cls = Paths.get( classesDir, "org", "apache", "maven", "plugin", "surefire", "booterclient",
                "MainClass.class" );
            zos.write( Files.readAllBytes( cls ) );
            zos.closeArchiveEntry();
        }
    }

    @AfterClass
    public static void deleteTmp()
    {
        deleteQuietly( tmp );
    }

    @Test
    public void processShouldExitWithoutSayingGoodBye() throws Exception
    {
        ReporterConfiguration reporterConfiguration = new ReporterConfiguration( tmp, true );

        ProviderConfiguration providerConfiguration = mock( ProviderConfiguration.class );
        when( providerConfiguration.getReporterConfiguration() )
            .thenReturn( reporterConfiguration );
        when( providerConfiguration.getShutdown() )
            .thenReturn( Shutdown.EXIT );

        StartupConfiguration startupConfiguration = mock( StartupConfiguration.class );
        when( startupConfiguration.getClasspathConfiguration() )
            .thenReturn( new ClasspathConfig() );
        when( startupConfiguration.getClassLoaderConfiguration() )
            .thenReturn( new ClassLoaderConfiguration( false, false ) );
        when( startupConfiguration.getProviderClassName() )
            .thenReturn( MainClass.class.getName() );

        ForkConfiguration forkConfiguration = mock( ForkConfiguration.class );
        when( forkConfiguration.getWorkingDirectory() )
            .thenReturn( tmp );
        when( forkConfiguration.getTempDirectory() )
            .thenReturn( tmp );
        when( forkConfiguration.getPluginPlatform() )
            .thenReturn( new Platform() );
        Commandline cli = new Commandline();
        cli.setWorkingDirectory( tmp );
        cli.setExecutable( System.getProperty( "java.home" ) + "/bin/java" );
        cli.createArg().setLine( "-jar" );
        cli.createArg().setLine( "surefirebooter.jar" );
        cli.createArg().setLine( "fail" );
        when( forkConfiguration.createCommandLine( eq( startupConfiguration ), eq( 1 ), eq( tmp ) ) )
            .thenReturn( cli );

        SurefireStatelessTestsetInfoReporter statelessTestsetInfoReporter = new SurefireStatelessTestsetInfoReporter();
        SurefireConsoleOutputReporter outputReporter = new SurefireConsoleOutputReporter();
        SurefireStatelessReporter xmlReporter = new SurefireStatelessReporter( true, "3" );

        StartupReportConfiguration startupReportConfiguration = new StartupReportConfiguration( true, true, null,
            false, tmp, true, "", null, false, 0, null, null, true,
            xmlReporter, outputReporter, statelessTestsetInfoReporter );

        ConsoleLogger logger = mock( ConsoleLogger.class );

        ForkStarter forkStarter = new ForkStarter( providerConfiguration, startupConfiguration, forkConfiguration,
            0, startupReportConfiguration, logger );

        DefaultReporterFactory reporterFactory =
            new DefaultReporterFactory( startupReportConfiguration, logger, 1 );

        e.expect( SurefireBooterForkException.class );
        e.expectMessage( containsString( "Process Exit Code: 1" ) );
        e.expectMessage( containsString( "The forked VM terminated without properly saying goodbye." ) );
        e.expectMessage( containsString( "VM crash or System.exit called?" ) );

        Class<?>[] types = {Object.class, PropertiesWrapper.class, ForkClient.class, SurefireProperties.class,
            int.class, AbstractCommandReader.class, ForkNodeFactory.class, boolean.class};
        TestProvidingInputStream testProvidingInputStream = new TestProvidingInputStream( new ArrayDeque<String>() );
        invokeMethod( forkStarter, "fork", types, null,
            new PropertiesWrapper( Collections.<String, String>emptyMap() ),
            new ForkClient( reporterFactory, null, 1 ),
            new SurefireProperties(), 1, testProvidingInputStream, new LegacyForkNodeFactory(), true );
        testProvidingInputStream.close();
    }

    @Test
    public void processShouldWaitForAck() throws Exception
    {
        ReporterConfiguration reporterConfiguration = new ReporterConfiguration( tmp, true );

        ProviderConfiguration providerConfiguration = mock( ProviderConfiguration.class );
        when( providerConfiguration.getReporterConfiguration() )
            .thenReturn( reporterConfiguration );
        when( providerConfiguration.getShutdown() )
            .thenReturn( Shutdown.EXIT );

        StartupConfiguration startupConfiguration = mock( StartupConfiguration.class );
        when( startupConfiguration.getClasspathConfiguration() )
            .thenReturn( new ClasspathConfig() );
        when( startupConfiguration.getClassLoaderConfiguration() )
            .thenReturn( new ClassLoaderConfiguration( false, false ) );
        when( startupConfiguration.getProviderClassName() )
            .thenReturn( MainClass.class.getName() );

        ForkConfiguration forkConfiguration = mock( ForkConfiguration.class );
        when( forkConfiguration.getWorkingDirectory() )
            .thenReturn( tmp );
        when( forkConfiguration.getTempDirectory() )
            .thenReturn( tmp );
        when( forkConfiguration.getPluginPlatform() )
            .thenReturn( new Platform() );
        Commandline cli = new Commandline();
        cli.setWorkingDirectory( tmp );
        cli.setExecutable( System.getProperty( "java.home" ) + "/bin/java" );
        cli.createArg().setLine( "-jar" );
        cli.createArg().setLine( "surefirebooter.jar" );
        when( forkConfiguration.createCommandLine( eq( startupConfiguration ), eq( 1 ), eq( tmp ) ) )
            .thenReturn( cli );

        SurefireStatelessTestsetInfoReporter statelessTestsetInfoReporter = new SurefireStatelessTestsetInfoReporter();
        SurefireConsoleOutputReporter outputReporter = new SurefireConsoleOutputReporter();
        SurefireStatelessReporter xmlReporter = new SurefireStatelessReporter( true, "3" );

        StartupReportConfiguration startupReportConfiguration = new StartupReportConfiguration( true, true, null,
            false, tmp, true, "", null, false, 0, null, null, true,
            xmlReporter, outputReporter, statelessTestsetInfoReporter );

        ConsoleLogger logger = mock( ConsoleLogger.class );

        ForkStarter forkStarter = new ForkStarter( providerConfiguration, startupConfiguration, forkConfiguration,
            0, startupReportConfiguration, logger );

        DefaultReporterFactory reporterFactory =
            new DefaultReporterFactory( startupReportConfiguration, logger, 1 );

        Class<?>[] types = {Object.class, PropertiesWrapper.class, ForkClient.class, SurefireProperties.class,
            int.class, AbstractCommandReader.class, ForkNodeFactory.class, boolean.class};
        TestLessInputStream testLessInputStream = new TestLessInputStreamBuilder().build();
        invokeMethod( forkStarter, "fork", types, null,
            new PropertiesWrapper( Collections.<String, String>emptyMap() ),
            new ForkClient( reporterFactory, testLessInputStream, 1 ),
            new SurefireProperties(), 1, testLessInputStream, new LegacyForkNodeFactory(), true );
        testLessInputStream.close();
    }

    private static class ClasspathConfig extends AbstractPathConfiguration
    {
        ClasspathConfig()
        {
            this( Classpath.emptyClasspath(), false, false );
        }

        private ClasspathConfig( Classpath surefireClasspathUrls, boolean enableAssertions, boolean childDelegation )
        {
            super( surefireClasspathUrls, enableAssertions, childDelegation );
        }

        @Override
        public Classpath getTestClasspath()
        {
            return Classpath.emptyClasspath();
        }

        @Override
        public boolean isModularPathConfig()
        {
            return false;
        }

        @Override
        public boolean isClassPathConfig()
        {
            return true;
        }

        @Override
        protected Classpath getInprocClasspath()
        {
            return Classpath.emptyClasspath();
        }
    }
}
