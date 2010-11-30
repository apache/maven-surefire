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

    private final BooterConfiguration booterConfiguration;

    public SurefireStarter( BooterConfiguration booterConfiguration )
    {
        this.booterConfiguration = booterConfiguration;
    }

    public int runSuitesInProcess( Object testSet, Properties results )
        throws SurefireExecutionException
    {
        // TODO: replace with plexus

        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            final ProviderConfiguration starterConfiguration = booterConfiguration.getSurefireStarterConfiguration();
            final ClasspathConfiguration classpathConfiguration = starterConfiguration.getClasspathConfiguration();

            ClassLoader testsClassLoader = classpathConfiguration.createTestClassLoaderConditionallySystem(
                starterConfiguration.useSystemClassLoader() );

            // TODO: assertions = true shouldn't be required for this CL if we had proper separation (see TestNG)
            ClassLoader surefireClassLoader = classpathConfiguration.createSurefireClassLoader( testsClassLoader );

            SurefireReflector reflector = new SurefireReflector( surefireClassLoader );

            Thread.currentThread().setContextClassLoader( testsClassLoader );
            return reflector.runProvider( booterConfiguration.getReporterConfiguration(),
                                          booterConfiguration.getReports(), surefireClassLoader, testsClassLoader,
                                          results, booterConfiguration.isFailIfNoTests(),
                                          booterConfiguration.getTestSuiteDefinition(),
                                          booterConfiguration.getTestNg(), starterConfiguration.getProviderClassName(),
                                          booterConfiguration.getDirScannerParams(), testSet );
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

        try
        {
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

            SurefireReflector reflector = new SurefireReflector( surefireClassLoader );

            Thread.currentThread().setContextClassLoader( testsClassLoader );

            return reflector.runProvider( booterConfiguration.getReporterConfiguration(),
                                          booterConfiguration.getReports(), surefireClassLoader, testsClassLoader, p,
                                          booterConfiguration.isFailIfNoTests(),
                                          booterConfiguration.getTestSuiteDefinition(), booterConfiguration.getTestNg(),
                                          starterConfiguration.getProviderClassName(),
                                          booterConfiguration.getDirScannerParams(), null );

        }
        finally
        {
            Thread.currentThread().setContextClassLoader( oldContextClassLoader );
        }
    }

}
