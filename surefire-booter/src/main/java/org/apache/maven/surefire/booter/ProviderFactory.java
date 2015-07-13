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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.ReflectionUtils;

/**
 * Creates the surefire provider.
 * <p/>
 *
 * @author Kristian Rosenvold
 */
public class ProviderFactory
{
    private final StartupConfiguration startupConfiguration;

    private final ProviderConfiguration providerConfiguration;

    private final ClassLoader classLoader;

    private final SurefireReflector surefireReflector;

    private final Object reporterManagerFactory;

    private static final Class[] INVOKE_PARAMETERS = new Class[]{ Object.class };


    public ProviderFactory( StartupConfiguration startupConfiguration, ProviderConfiguration providerConfiguration,
                            ClassLoader testsClassLoader, Object reporterManagerFactory )
    {
        this.providerConfiguration = providerConfiguration;
        this.startupConfiguration = startupConfiguration;
        this.surefireReflector = new SurefireReflector( testsClassLoader );
        this.classLoader = testsClassLoader;
        this.reporterManagerFactory = reporterManagerFactory;
    }

    public static RunResult invokeProvider( Object testSet, ClassLoader testsClassLoader, Object factory,
                                            ProviderConfiguration providerConfiguration, boolean insideFork,
                                            StartupConfiguration startupConfiguration1, boolean restoreStreams )
        throws TestSetFailedException, InvocationTargetException
    {
        final PrintStream orgSystemOut = System.out;
        final PrintStream orgSystemErr = System.err;
        // Note that System.out/System.err are also read in the "ReporterConfiguration" instantiation
        // in createProvider below. These are the same values as here.

        ProviderFactory providerFactory =
            new ProviderFactory( startupConfiguration1, providerConfiguration, testsClassLoader, factory );
        final SurefireProvider provider = providerFactory.createProvider( insideFork );
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

    public SurefireProvider createProvider( boolean isInsideFork )
    {
        final Thread currentThread = Thread.currentThread();
        final ClassLoader systemClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader( classLoader );
        // Note: Duplicated in ForkedBooter#createProviderInCurrentClassloader
        Object o = surefireReflector.createBooterConfiguration( classLoader, reporterManagerFactory, isInsideFork );
        surefireReflector.setTestSuiteDefinitionAware( o, providerConfiguration.getTestSuiteDefinition() );
        surefireReflector.setProviderPropertiesAware( o, providerConfiguration.getProviderProperties() );
        surefireReflector.setReporterConfigurationAware( o, providerConfiguration.getReporterConfiguration() );
        surefireReflector.setTestClassLoaderAware( o, classLoader );
        surefireReflector.setTestArtifactInfoAware( o, providerConfiguration.getTestArtifact() );
        surefireReflector.setRunOrderParameters( o, providerConfiguration.getRunOrderParameters() );
        surefireReflector.setIfDirScannerAware( o, providerConfiguration.getDirScannerParams() );

        Object provider = surefireReflector.instantiateProvider( startupConfiguration.getActualClassName(), o );
        currentThread.setContextClassLoader( systemClassLoader );

        return new ProviderProxy( provider, classLoader );
    }


    private final class ProviderProxy
        implements SurefireProvider
    {
        private final Object providerInOtherClassLoader;

        private final ClassLoader testsClassLoader;


        private ProviderProxy( Object providerInOtherClassLoader, ClassLoader testsClassLoader )
        {
            this.providerInOtherClassLoader = providerInOtherClassLoader;
            this.testsClassLoader = testsClassLoader;
        }

        @SuppressWarnings( "unchecked" )
        public Iterable<Class<?>> getSuites()
        {
            ClassLoader current = swapClassLoader( testsClassLoader );
            try
            {
                return (Iterable<Class<?>>) ReflectionUtils.invokeGetter( providerInOtherClassLoader, "getSuites" );
            }
            finally
            {
                Thread.currentThread().setContextClassLoader( current );
            }
        }

        public RunResult invoke( Object forkTestSet )
            throws TestSetFailedException, InvocationTargetException
        {
            ClassLoader current = swapClassLoader( testsClassLoader );
            try
            {
                final Method invoke =
                    ReflectionUtils.getMethod( providerInOtherClassLoader.getClass(), "invoke", INVOKE_PARAMETERS );

                final Object result = ReflectionUtils.invokeMethodWithArray2( providerInOtherClassLoader, invoke,
                                                                              new Object[]{ forkTestSet } );
                return (RunResult) surefireReflector.convertIfRunResult( result );
            }
            finally
            {
                if ( System.getSecurityManager() == null )
                {
                    Thread.currentThread().setContextClassLoader( current );
                }
            }

        }

        private ClassLoader swapClassLoader( ClassLoader newClassLoader )
        {
            ClassLoader current = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader( newClassLoader );
            return current;
        }

        public void cancel()
        {
            final Method invoke =
                ReflectionUtils.getMethod( providerInOtherClassLoader.getClass(), "cancel", new Class[]{ } );
            ReflectionUtils.invokeMethodWithArray( providerInOtherClassLoader, invoke, null );
        }
    }
}
