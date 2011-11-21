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
import org.apache.maven.surefire.suite.RunResult;

/**
 * Invokes surefire with the correct classloader setup. This class covers all startups in forked VM's.
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

    private final StarterCommon starterCommon;

    public SurefireStarter( StartupConfiguration startupConfiguration, ProviderConfiguration providerConfiguration )
    {
        this.providerConfiguration = providerConfiguration;
        this.startupConfiguration = startupConfiguration;
        this.starterCommon = new StarterCommon( startupConfiguration, providerConfiguration );
    }

    public RunResult runSuitesInProcessWhenForked( TypeEncodedValue testSet )
        throws SurefireExecutionException
    {
        starterCommon.writeSurefireTestClasspathProperty();
        final ClasspathConfiguration classpathConfiguration = startupConfiguration.getClasspathConfiguration();

        // todo: Find out....
        // Why is the classloader structure created differently when a testSet is specified ?
        // Smells like a legacy bug. Need to check issue tracker.
        ClassLoader testsClassLoader = classpathConfiguration.createTestClassLoaderConditionallySystem(
            startupConfiguration.useSystemClassLoader() );

        ClassLoader surefireClassLoader = classpathConfiguration.createSurefireClassLoader( testsClassLoader );

        SurefireReflector surefireReflector = new SurefireReflector( surefireClassLoader );

        final Object forkingReporterFactory = createForkingReporterFactory( surefireReflector );

        Object test = testSet.getDecodedValue();

        return starterCommon.invokeProvider( test, testsClassLoader, surefireClassLoader, forkingReporterFactory, true );
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
        ClassLoader testsClassLoader = starterCommon.createInProcessTestClassLoader();

        ClassLoader surefireClassLoader = starterCommon.createSurefireClassloader( testsClassLoader );

        SurefireReflector surefireReflector = new SurefireReflector( surefireClassLoader );

        final Object factory = createForkingReporterFactory( surefireReflector );

        return starterCommon.invokeProvider( null, testsClassLoader, surefireClassLoader, factory, true );
    }

}
