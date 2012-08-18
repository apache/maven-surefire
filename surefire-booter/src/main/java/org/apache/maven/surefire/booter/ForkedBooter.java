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
import org.apache.maven.surefire.suite.RunResult;

/**
 * The part of the booter that is unique to a forked vm.
 * <p/>
 * Deals with deserialization of the booter wire-level protocol
 * <p/>
 *
 * @author Jason van Zyl
 * @author Emmanuel Venisse
 * @author Kristian Rosenvold
 * @version $Id$
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

            final ClasspathConfiguration classpathConfiguration = startupConfiguration.getClasspathConfiguration();
            final ClassLoader testClassLoader = classpathConfiguration.createForkingTestClassLoader(
                startupConfiguration.isManifestOnlyJarRequestedAndUsable() );

            startupConfiguration.writeSurefireTestClasspathProperty();

            Object testSet = forkedTestSet != null ? forkedTestSet.getDecodedValue( testClassLoader ) : null;
            runSuitesInProcess( testSet, testClassLoader, startupConfiguration, providerConfiguration, originalOut );
            // Say bye.
            originalOut.println( "Z,0,BYE!" );
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

    private final static long SYSTEM_EXIT_TIMEOUT = 30 * 1000;

    private static void exit( final int returnCode )
    {
        launchLastDitchDaemonShutdownThread( returnCode );
        System.exit( returnCode );
    }


    private static RunResult runSuitesInProcess( Object testSet, ClassLoader testsClassLoader,
                                                StartupConfiguration startupConfiguration,
                                                ProviderConfiguration providerConfiguration,
                                                PrintStream originalSystemOut )
        throws SurefireExecutionException
    {
        final ClasspathConfiguration classpathConfiguration = startupConfiguration.getClasspathConfiguration();
        ClassLoader surefireClassLoader = classpathConfiguration.createSurefireClassLoader( testsClassLoader );

        SurefireReflector surefireReflector = new SurefireReflector( surefireClassLoader );

        final Object factory =
            createForkingReporterFactory( surefireReflector, providerConfiguration, originalSystemOut );

        return ProviderFactory.invokeProvider( testSet, testsClassLoader, surefireClassLoader, factory,
                                               providerConfiguration, true, startupConfiguration, false );
    }

    private static Object createForkingReporterFactory( SurefireReflector surefireReflector,
                                                        ProviderConfiguration providerConfiguration,
                                                        PrintStream originalSystemOut )
    {
        final Boolean trimStackTrace = providerConfiguration.getReporterConfiguration().isTrimStackTrace();
        return surefireReflector.createForkingReporterFactory( trimStackTrace, originalSystemOut );
    }

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

}
