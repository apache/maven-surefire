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
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;

import static org.apache.maven.surefire.util.ReflectionUtils.getMethod;
import static org.apache.maven.surefire.util.ReflectionUtils.invokeGetter;
import static org.apache.maven.surefire.util.ReflectionUtils.invokeMethodWithArray;
import static org.apache.maven.surefire.util.ReflectionUtils.invokeMethodWithArray2;

/**
 * Creates the surefire provider.
 * <br>
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

    private static final Class[] INVOKE_PARAMETERS = { Object.class };

    private static final Class[] INVOKE_EMPTY_PARAMETER_TYPES = { };

    private static final Object[] INVOKE_EMPTY_PARAMETERS = { };

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
                                            StartupConfiguration startupConfig, boolean restoreStreams )
        throws TestSetFailedException, InvocationTargetException
    {
        final PrintStream orgSystemOut = System.out;
        final PrintStream orgSystemErr = System.err;
        // Note that System.out/System.err are also read in the "ReporterConfiguration" instantiation
        // in createProvider below. These are the same values as here.

        try
        {
            return new ProviderFactory( startupConfig, providerConfiguration, testsClassLoader, factory )
                .createProvider( insideFork )
                .invoke( testSet );
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
        surefireReflector.setMainCliOptions( o, providerConfiguration.getMainCliOptions() );
        surefireReflector.setSkipAfterFailureCount( o, providerConfiguration.getSkipAfterFailureCount() );
        surefireReflector.setShutdown( o, providerConfiguration.getShutdown() );
        if ( isInsideFork )
        {
            surefireReflector.setSystemExitTimeout( o, providerConfiguration.getSystemExitTimeout() );
        }

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

        @Override
        @SuppressWarnings( "unchecked" )
        public Iterable<Class<?>> getSuites()
        {
            ClassLoader current = swapClassLoader( testsClassLoader );
            try
            {
                return (Iterable<Class<?>>) invokeGetter( providerInOtherClassLoader, "getSuites" );
            }
            finally
            {
                Thread.currentThread().setContextClassLoader( current );
            }
        }

        @Override
        public RunResult invoke( Object forkTestSet )
            throws ReporterException, InvocationTargetException
        {
            ClassLoader current = swapClassLoader( testsClassLoader );
            try
            {
                Method invoke = getMethod( providerInOtherClassLoader.getClass(), "invoke", INVOKE_PARAMETERS );
                Object result = invokeMethodWithArray2( providerInOtherClassLoader, invoke, forkTestSet );
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

        @Override
        public void cancel()
        {
            Class<?> providerType = providerInOtherClassLoader.getClass();
            Method invoke = getMethod( providerType, "cancel", INVOKE_EMPTY_PARAMETER_TYPES );
            invokeMethodWithArray( providerInOtherClassLoader, invoke, INVOKE_EMPTY_PARAMETERS );
        }
    }
}
