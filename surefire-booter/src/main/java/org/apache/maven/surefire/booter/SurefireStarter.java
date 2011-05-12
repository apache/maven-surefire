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

import java.io.PrintStream;
import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.NestedRuntimeException;

/**
 * Invokes surefire with the correct classloader setup.
 * <p/>
 * This part of the booter is always guaranteed to be in the
 * same vm as the tests will be run in.
 *
 * @author Jason van Zyl
 * @author Brett Porter
 * @author Emmanuel Venisse
 * @author Dan Fabulich
 * @author Kristian Rosenvold
 * @version $Id$
 */
public class SurefireStarter
{
    private final ProviderConfiguration providerConfiguration;

    private final StartupConfiguration startupConfiguration;

    private final static String SUREFIRE_TEST_CLASSPATH = "surefire.test.class.path";

    private final StartupReportConfiguration startupReportConfiguration;

    public SurefireStarter( StartupConfiguration startupConfiguration, ProviderConfiguration providerConfiguration,
                            StartupReportConfiguration startupReportConfiguration )
    {
        this.providerConfiguration = providerConfiguration;
        this.startupConfiguration = startupConfiguration;
        this.startupReportConfiguration = startupReportConfiguration;
    }

    public RunResult runSuitesInProcessWhenForked( Object testSet )
        throws SurefireExecutionException
    {
        writeSurefireTestClasspathProperty();
        final ClasspathConfiguration classpathConfiguration = startupConfiguration.getClasspathConfiguration();

        // todo: Find out....
        // Why is the classloader structure created differently when a testSet is specified ?
        // Smells like a legacy bug. Need to check issue tracker.
        ClassLoader testsClassLoader = classpathConfiguration.createTestClassLoaderConditionallySystem(
            startupConfiguration.useSystemClassLoader() );

        ClassLoader surefireClassLoader = classpathConfiguration.createSurefireClassLoader( testsClassLoader );

        SurefireReflector surefireReflector = new SurefireReflector( surefireClassLoader );

        final Object forkingReporterFactory = createForkingReporterFactory( surefireReflector );

        return invokeProvider( testSet, testsClassLoader, surefireClassLoader, forkingReporterFactory );
    }

    private Object createForkingReporterFactory( SurefireReflector surefireReflector )
    {
        final Boolean trimStackTrace = this.providerConfiguration.getReporterConfiguration().isTrimStackTrace();
        final PrintStream originalSystemOut =
            this.providerConfiguration.getReporterConfiguration().getOriginalSystemOut();
        return surefireReflector.createForkingReporterFactory( trimStackTrace, originalSystemOut );
    }

    // todo: Fix duplication in this method and runSuitesInProcess
    // This should be fixed "at a higher level", because this whole way
    // of organizing the code stinks.

    public RunResult runSuitesInProcessWhenForked()
        throws SurefireExecutionException
    {
        ClassLoader testsClassLoader = createInProcessTestClassLoader();

        ClassLoader surefireClassLoader = createSurefireClassloader( testsClassLoader );

        SurefireReflector surefireReflector = new SurefireReflector( surefireClassLoader );

        final Object factory = createForkingReporterFactory( surefireReflector );

        return invokeProvider( null, testsClassLoader, surefireClassLoader, factory );
    }

    public RunResult runSuitesInProcess()
        throws SurefireExecutionException
    {
        // The test classloader must be constructed first to avoid issues with commons-logging until we properly
        // separate the TestNG classloader
        ClassLoader testsClassLoader = createInProcessTestClassLoader();

        ClassLoader surefireClassLoader = createSurefireClassloader( testsClassLoader );

        SurefireReflector surefireReflector = new SurefireReflector( surefireClassLoader );

        final Object factory = surefireReflector.createReportingReporterFactory( startupReportConfiguration );

        return invokeProvider( null, testsClassLoader, surefireClassLoader, factory );
    }

    private ClassLoader createSurefireClassloader( ClassLoader testsClassLoader )
        throws SurefireExecutionException
    {
        final ClasspathConfiguration classpathConfiguration = startupConfiguration.getClasspathConfiguration();

        return classpathConfiguration.createSurefireClassLoader( testsClassLoader );
    }

    private ClassLoader createInProcessTestClassLoader()
        throws SurefireExecutionException
    {
        writeSurefireTestClasspathProperty();
        ClasspathConfiguration classpathConfiguration = startupConfiguration.getClasspathConfiguration();
        if ( startupConfiguration.isManifestOnlyJarRequestedAndUsable() )
        {
            ClassLoader testsClassLoader = getClass().getClassLoader(); // ClassLoader.getSystemClassLoader()
            // SUREFIRE-459, trick the app under test into thinking its classpath was conventional
            // (instead of a single manifest-only jar)
            System.setProperty( "surefire.real.class.path", System.getProperty( "java.class.path" ) );
            classpathConfiguration.getTestClasspath().writeToSystemProperty( "java.class.path" );
            return testsClassLoader;
        }
        else
        {
            return classpathConfiguration.createTestClassLoader();
        }
    }

    private void writeSurefireTestClasspathProperty()
    {
        ClasspathConfiguration classpathConfiguration = startupConfiguration.getClasspathConfiguration();
        classpathConfiguration.getTestClasspath().writeToSystemProperty( SUREFIRE_TEST_CLASSPATH );
    }

    private RunResult invokeProvider( Object testSet, ClassLoader testsClassLoader, ClassLoader surefireClassLoader,
                                      Object factory )
    {
        final PrintStream orgSystemOut = System.out;
        final PrintStream orgSystemErr = System.err;
        // Note that System.out/System.err are also read in the "ReporterConfiguration" instatiation
        // in createProvider below. These are the same values as here.

        ProviderFactory providerFactory =
            new ProviderFactory( startupConfiguration, providerConfiguration, surefireClassLoader, testsClassLoader,
                                 factory );
        final SurefireProvider provider = providerFactory.createProvider();

        try
        {
            return provider.invoke( testSet );
        }
        catch ( TestSetFailedException e )
        {
            throw new NestedRuntimeException( e );
        }
        catch ( ReporterException e )
        {
            throw new NestedRuntimeException( e );
        }
        finally
        {
            if ( System.getSecurityManager() == null )
            {
                System.setOut( orgSystemOut );
                System.setErr( orgSystemErr );
            }
        }
    }
}
