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
 *
 * This part of the booter that is always guaranteed to be in the
 * same vm as the tests will be run in.
 *
 * @author Jason van Zyl
 * @author Emmanuel Venisse
 * @version $Id$
 */
public class SurefireStarter
{

    private final BooterConfiguration booterConfiguration;

    public SurefireStarter( BooterConfiguration booterConfiguration )
    {
        this.booterConfiguration = booterConfiguration;
    }

    public int runSuitesInProcess( String testSet, Properties results )
        throws SurefireExecutionException
    {
        if ( booterConfiguration.getTestSuites().size() != 1 )
        {
            throw new IllegalArgumentException( "Cannot only specify testSet for single test suites" );
        }

        // TODO: replace with plexus

        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            ClassLoader testsClassLoader = getClasspathConfiguration().createTestClassLoaderConditionallySystem(
                booterConfiguration.useSystemClassLoader() );

            // TODO: assertions = true shouldn't be required for this CL if we had proper separation (see TestNG)
            ClassLoader surefireClassLoader = getClasspathConfiguration().createSurefireClassLoader( testsClassLoader );

            SurefireReflector reflector = new SurefireReflector( surefireClassLoader );

            Thread.currentThread().setContextClassLoader( testsClassLoader );
            return reflector.run( booterConfiguration.getReports(),
                                  (Object[]) booterConfiguration.getTestSuites().get( 0 ), testSet, surefireClassLoader,
                                  testsClassLoader, results, booterConfiguration.isFailIfNoTests() );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( oldContextClassLoader );
        }
    }

    public int runSuitesInProcess()
        throws SurefireExecutionException
    {
        // TODO: replace with plexus

        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();

        try
        {
            // The test classloader must be constructed first to avoid issues with commons-logging until we properly
            // separate the TestNG classloader
            ClassLoader testsClassLoader;
            String testClassPath = getClasspathConfiguration().getTestClassPathAsString();
            System.setProperty( "surefire.test.class.path", testClassPath );
            if ( booterConfiguration.isManifestOnlyJarRequestedAndUsable() )
            {
                testsClassLoader = getClass().getClassLoader(); // ClassLoader.getSystemClassLoader()
                // SUREFIRE-459, trick the app under test into thinking its classpath was conventional
                // (instead of a single manifest-only jar)
                System.setProperty( "surefire.real.class.path", System.getProperty( "java.class.path" ) );
                System.setProperty( "java.class.path", testClassPath );
            }
            else
            {
                testsClassLoader = getClasspathConfiguration().createTestClassLoader();
            }

            ClassLoader surefireClassLoader = getClasspathConfiguration().createSurefireClassLoader( testsClassLoader );

            SurefireReflector reflector = new SurefireReflector( surefireClassLoader );

            Thread.currentThread().setContextClassLoader( testsClassLoader );

            return reflector.run( booterConfiguration.getReports(), booterConfiguration.getTestSuites(),
                                  surefireClassLoader, testsClassLoader, booterConfiguration.isFailIfNoTests() );

        }
        finally
        {
            Thread.currentThread().setContextClassLoader( oldContextClassLoader );
        }
    }

    private ClasspathConfiguration getClasspathConfiguration()
    {
        return booterConfiguration.getClasspathConfiguration();
    }

}
