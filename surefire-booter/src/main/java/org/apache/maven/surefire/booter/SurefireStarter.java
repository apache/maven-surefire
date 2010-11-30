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

import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;

import java.util.Properties;

/**
 * Invokes surefire with the correct classloader setup.
 * <p/>
 * This part of the booter that is always guaranteed to be in the
 * same vm as the tests will be run in.
 *
 * @author Jason van Zyl
 * @author Emmanuel Venisse
 * @author Kristian Rosenvold
 * @version $Id$
 */
public class SurefireStarter
{
    private static final int NO_TESTS = 254;

    private final BooterConfiguration booterConfiguration;

    public SurefireStarter( BooterConfiguration booterConfiguration )
    {
        this.booterConfiguration = booterConfiguration;
    }

    public int runSuitesInProcess( Object testSet, Properties results )
        throws SurefireExecutionException
    {

        // TODO: replace with plexus
        final ProviderConfiguration starterConfiguration = booterConfiguration.getSurefireStarterConfiguration();
        final ClasspathConfiguration classpathConfiguration = starterConfiguration.getClasspathConfiguration();

        ClassLoader testsClassLoader = classpathConfiguration.createTestClassLoaderConditionallySystem(
            starterConfiguration.useSystemClassLoader() );

        // TODO: assertions = true shouldn't be required for this CL if we had proper separation (see TestNG)
        ClassLoader surefireClassLoader = classpathConfiguration.createSurefireClassLoader( testsClassLoader );

        RunResult runResult = invokeProvider( testSet, testsClassLoader, surefireClassLoader );

        updateResultsProperties( runResult, results );

        return processRunCount( runResult, surefireClassLoader );
    }

    private static final String RESULTS_ERRORS = "errors";

    private static final String RESULTS_COMPLETED_COUNT = "completedCount";

    private static final String RESULTS_FAILURES = "failures";

    private static final String RESULTS_SKIPPED = "skipped";


    public synchronized void updateResultsProperties( RunResult runResult, Properties results )
    {
        results.setProperty( RESULTS_ERRORS, String.valueOf( runResult.getErrors() ) );
        results.setProperty( RESULTS_COMPLETED_COUNT, String.valueOf( runResult.getCompletedCount() ) );
        results.setProperty( RESULTS_FAILURES, String.valueOf( runResult.getFailures() ) );
        results.setProperty( RESULTS_SKIPPED, String.valueOf( runResult.getSkipped() ) );
    }

    private RunResult invokeProvider( Object testSet, ClassLoader testsClassLoader, ClassLoader surefireClassLoader )
    {
        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader( surefireClassLoader );

        ProviderFactory providerFactory = new ProviderFactory( booterConfiguration, surefireClassLoader );
        final SurefireProvider provider = providerFactory.createProvider( testsClassLoader );

        try
        {
            final RunResult invoke = provider.invoke( testSet );
            return invoke;
        }
        catch ( TestSetFailedException e )
        {
            throw new RuntimeException( e );
        }
        catch ( ReporterException e )
        {
            throw new RuntimeException( e );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( oldContextClassLoader );
        }
    }

    public int runSuitesInProcess( Properties p )
        throws SurefireExecutionException
    {
        // TODO: replace with plexus

        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();

        // The test classloader must be constructed first to avoid issues with commons-logging until we properly
        // separate the TestNG classloader
        ClassLoader testsClassLoader;
        final ProviderConfiguration starterConfiguration = booterConfiguration.getSurefireStarterConfiguration();
        final ClasspathConfiguration classpathConfiguration = starterConfiguration.getClasspathConfiguration();
        String testClassPath = classpathConfiguration.getTestClasspath().getClassPathAsString();
        System.setProperty( "surefire.test.class.path", testClassPath );
        if ( starterConfiguration.isManifestOnlyJarRequestedAndUsable() )
        {
            testsClassLoader = getClass().getClassLoader(); // ClassLoader.getSystemClassLoader()
            // SUREFIRE-459, trick the app under test into thinking its classpath was conventional
            // (instead of a single manifest-only jar)
            System.setProperty( "surefire.real.class.path", System.getProperty( "java.class.path" ) );
            System.setProperty( "java.class.path", testClassPath );
        }
        else
        {
            testsClassLoader = classpathConfiguration.createTestClassLoader();
        }

        ClassLoader surefireClassLoader = classpathConfiguration.createSurefireClassLoader( testsClassLoader );

        Thread.currentThread().setContextClassLoader( surefireClassLoader );
        final RunResult runResult = invokeProvider( null, testsClassLoader, surefireClassLoader );
        return processRunCount( runResult, surefireClassLoader );
    }

    /**
     * Returns
     *
     * @param runCount            The run result
     * @param surefireClassLoader The sirefore classloader
     * @return The process result code
     * @throws SurefireExecutionException When an exception is found
     */
    private int processRunCount( RunResult runCount, ClassLoader surefireClassLoader )
        throws SurefireExecutionException
    {
        if ( runCount.getCompletedCount() == 0 )
        {
            // TODO: Bugbug find out WTF this means. This blows up like crap when running here ;)
/*            ReporterManagerFactory reporterManagerFactory =
                new ReporterManagerFactory2( surefireClassLoader, booterConfiguration.getReporterConfiguration() );
            try
            {
                reporterManagerFactory.createReporterManager().writeMessage( "There are no tests to run." );
            }
            catch ( TestSetFailedException e )
            {
                throw new SurefireExecutionException( "While reporting error", e );
            }
            finally
            {
                reporterManagerFactory.close();
            }*/
        }

        if ( runCount.getCompletedCount() == 0 && booterConfiguration.isFailIfNoTests().booleanValue() )
        {
            return NO_TESTS;
        }

        return runCount.getBooterCode();
    }

}
