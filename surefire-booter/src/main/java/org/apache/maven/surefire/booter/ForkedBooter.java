package org.apache.maven.surefire.booter;

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
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;

import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.apache.maven.surefire.report.LegacyPojoStackTraceWriter;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.StackTraceWriter;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.ReflectionUtils;

/**
 * The part of the booter that is unique to a forked vm.
 * <p/>
 * Deals with deserialization of the booter wire-level protocol
 * <p/>
 *
 * @author Jason van Zyl
 * @author Emmanuel Venisse
 * @author Kristian Rosenvold
 */
public class ForkedBooter
{

    /**
     * This method is invoked when Surefire is forked - this method parses and organizes the arguments passed to it and
     * then calls the Surefire class' run method. <p/> The system exit code will be 1 if an exception is thrown.
     *
     * @param args Commandline arguments
     * @throws Throwable Upon throwables
     */
    public static void main( String[] args )
        throws Throwable
    {
        final PrintStream originalOut = System.out;
        try
        {
            if ( args.length > 1 )
            {
                SystemPropertyManager.setSystemProperties( new File( args[1] ) );
            }

            File surefirePropertiesFile = new File( args[0] );
            InputStream stream = surefirePropertiesFile.exists() ? new FileInputStream( surefirePropertiesFile ) : null;
            BooterDeserializer booterDeserializer = new BooterDeserializer( stream );
            ProviderConfiguration providerConfiguration = booterDeserializer.deserialize();
            final StartupConfiguration startupConfiguration = booterDeserializer.getProviderConfiguration();

            TypeEncodedValue forkedTestSet = providerConfiguration.getTestForFork();
            boolean readTestsFromInputStream = providerConfiguration.isReadTestsFromInStream();
            boolean readTestsFromExternalSource = providerConfiguration.isTestsFromExternalSource();

            final ClasspathConfiguration classpathConfiguration = startupConfiguration.getClasspathConfiguration();
            if ( startupConfiguration.isManifestOnlyJarRequestedAndUsable() )
            {
                classpathConfiguration.trickClassPathWhenManifestOnlyClasspath();
            }

            Thread.currentThread().getContextClassLoader().setDefaultAssertionStatus(
                classpathConfiguration.isEnableAssertions() );
            startupConfiguration.writeSurefireTestClasspathProperty();

            Object testSet;
            if ( forkedTestSet != null )
            {
                testSet = forkedTestSet.getDecodedValue( Thread.currentThread().getContextClassLoader() );
            }
            else if ( readTestsFromInputStream )
            {
                testSet = new LazyTestsToRun( System.in, originalOut );
            }
            else if ( readTestsFromExternalSource )
            {
                testSet = ExternalTestToRunFactory.createTestToRun( providerConfiguration.getExternalSourceUrl() );
            }
            else
            {
                testSet = null;
            }

            try
            {
                runSuitesInProcess( testSet, startupConfiguration, providerConfiguration, originalOut );
            }
            catch ( InvocationTargetException t )
            {

                LegacyPojoStackTraceWriter stackTraceWriter =
                    new LegacyPojoStackTraceWriter( "test subystem", "no method", t.getTargetException() );
                StringBuilder stringBuilder = new StringBuilder();
                ForkingRunListener.encode( stringBuilder, stackTraceWriter, false );
                originalOut.println( ( (char) ForkingRunListener.BOOTERCODE_ERROR )
                                     + ",0," + stringBuilder.toString() );
            }
            catch ( Throwable t )
            {
                StackTraceWriter stackTraceWriter = new LegacyPojoStackTraceWriter( "test subystem", "no method", t );
                StringBuilder stringBuilder = new StringBuilder();
                ForkingRunListener.encode( stringBuilder, stackTraceWriter, false );
                originalOut.println( ( (char) ForkingRunListener.BOOTERCODE_ERROR )
                                     + ",0," + stringBuilder.toString() );
            }
            // Say bye.
            originalOut.println( ( (char) ForkingRunListener.BOOTERCODE_BYE ) + ",0,BYE!" );
            originalOut.flush();
            // noinspection CallToSystemExit
            exit( 0 );
        }
        catch ( Throwable t )
        {
            // Just throwing does getMessage() and a local trace - we want to call printStackTrace for a full trace
            // noinspection UseOfSystemOutOrSystemErr
            t.printStackTrace( System.err );
            // noinspection ProhibitedExceptionThrown,CallToSystemExit
            exit( 1 );
        }
    }

    private static final long SYSTEM_EXIT_TIMEOUT = 30 * 1000;

    private static void exit( final int returnCode )
    {
        launchLastDitchDaemonShutdownThread( returnCode );
        System.exit( returnCode );
    }


    private static RunResult runSuitesInProcess( Object testSet, StartupConfiguration startupConfiguration,
                                                 ProviderConfiguration providerConfiguration,
                                                 PrintStream originalSystemOut )
        throws SurefireExecutionException, TestSetFailedException, InvocationTargetException
    {
        final ReporterFactory factory = createForkingReporterFactory( providerConfiguration, originalSystemOut );

        return invokeProviderInSameClassLoader( testSet, factory, providerConfiguration, true, startupConfiguration,
                                                false );
    }

    private static ReporterFactory createForkingReporterFactory( ProviderConfiguration providerConfiguration,
                                                                 PrintStream originalSystemOut )
    {
        final Boolean trimStackTrace = providerConfiguration.getReporterConfiguration().isTrimStackTrace();
        return SurefireReflector.createForkingReporterFactoryInCurrentClassLoader( trimStackTrace, originalSystemOut );
    }

    @SuppressWarnings( "checkstyle:emptyblock" )
    private static void launchLastDitchDaemonShutdownThread( final int returnCode )
    {
        Thread lastExit = new Thread( new Runnable()
        {
            public void run()
            {
                try
                {
                    Thread.sleep( SYSTEM_EXIT_TIMEOUT );
                    Runtime.getRuntime().halt( returnCode );
                }
                catch ( InterruptedException ignore )
                {
                }
            }
        } );
        lastExit.setDaemon( true );
        lastExit.start();
    }

    public static RunResult invokeProviderInSameClassLoader( Object testSet, Object factory,
                                                             ProviderConfiguration providerConfiguration,
                                                             boolean insideFork,
                                                             StartupConfiguration startupConfiguration1,
                                                             boolean restoreStreams )
        throws TestSetFailedException, InvocationTargetException
    {
        final PrintStream orgSystemOut = System.out;
        final PrintStream orgSystemErr = System.err;
        // Note that System.out/System.err are also read in the "ReporterConfiguration" instatiation
        // in createProvider below. These are the same values as here.

        final SurefireProvider provider =
            createProviderInCurrentClassloader( startupConfiguration1, insideFork, providerConfiguration, factory );
        try
        {
            return provider.invoke( testSet );
        }
        finally
        {
            if ( restoreStreams && System.getSecurityManager() == null )
            {
                System.setOut( orgSystemOut );
                System.setErr( orgSystemErr );
            }
        }
    }

    public static SurefireProvider createProviderInCurrentClassloader( StartupConfiguration startupConfiguration1,
                                                                       boolean isInsideFork,
                                                                       ProviderConfiguration providerConfiguration,
                                                                       Object reporterManagerFactory1 )
    {

        BaseProviderFactory bpf = new BaseProviderFactory( (ReporterFactory) reporterManagerFactory1, isInsideFork );
        bpf.setTestRequest( providerConfiguration.getTestSuiteDefinition() );
        bpf.setReporterConfiguration( providerConfiguration.getReporterConfiguration() );
        ClassLoader clasLoader = Thread.currentThread().getContextClassLoader();
        bpf.setClassLoaders( clasLoader );
        bpf.setTestArtifactInfo( providerConfiguration.getTestArtifact() );
        bpf.setProviderProperties( providerConfiguration.getProviderProperties() );
        bpf.setRunOrderParameters( providerConfiguration.getRunOrderParameters() );
        bpf.setDirectoryScannerParameters( providerConfiguration.getDirScannerParams() );
        return (SurefireProvider) ReflectionUtils.instantiateOneArg( clasLoader,
                                                                     startupConfiguration1.getActualClassName(),
                                                                     ProviderParameters.class, bpf );
    }
}
