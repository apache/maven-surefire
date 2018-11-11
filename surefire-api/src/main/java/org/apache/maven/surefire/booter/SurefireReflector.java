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

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.log.api.ConsoleLoggerDecorator;
import org.apache.maven.surefire.cli.CommandLineOption;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.RunOrderParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestListResolver;
import org.apache.maven.surefire.testset.TestRequest;
import org.apache.maven.surefire.util.RunOrder;
import org.apache.maven.surefire.util.SurefireReflectionException;

import javax.annotation.Nonnull;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Collections.checkedList;
import static org.apache.maven.surefire.util.ReflectionUtils.getConstructor;
import static org.apache.maven.surefire.util.ReflectionUtils.getMethod;
import static org.apache.maven.surefire.util.ReflectionUtils.instantiateOneArg;
import static org.apache.maven.surefire.util.ReflectionUtils.instantiateTwoArgs;
import static org.apache.maven.surefire.util.ReflectionUtils.invokeGetter;
import static org.apache.maven.surefire.util.ReflectionUtils.invokeMethodWithArray;
import static org.apache.maven.surefire.util.ReflectionUtils.invokeSetter;
import static org.apache.maven.surefire.util.ReflectionUtils.newInstance;

/**
 * Does reflection based invocation of the surefire methods.
 * <br>
 * This is to avoid complications with linkage issues
 *
 * @author Kristian Rosenvold
 */
public class SurefireReflector
{
    private final ClassLoader surefireClassLoader;

    private final Class<?> reporterConfiguration;

    private final Class<?> testRequest;

    private final Class<?> testArtifactInfo;

    private final Class<?> testArtifactInfoAware;

    private final Class<?> directoryScannerParameters;

    private final Class<?> runOrderParameters;

    private final Class<?> directoryScannerParametersAware;

    private final Class<?> testSuiteDefinitionAware;

    private final Class<?> testClassLoaderAware;

    private final Class<?> reporterConfigurationAware;

    private final Class<?> providerPropertiesAware;

    private final Class<?> runResult;

    private final Class<?> booterParameters;

    private final Class<?> reporterFactory;

    private final Class<?> testListResolver;

    private final Class<?> mainCliOptions;

    private final Class<Enum> commandLineOptionsClass;

    private final Class<?> shutdownAwareClass;

    private final Class<Enum> shutdownClass;

    @SuppressWarnings( "unchecked" )
    public SurefireReflector( ClassLoader surefireClassLoader )
    {
        this.surefireClassLoader = surefireClassLoader;
        try
        {
            reporterConfiguration = surefireClassLoader.loadClass( ReporterConfiguration.class.getName() );
            testRequest = surefireClassLoader.loadClass( TestRequest.class.getName() );
            testArtifactInfo = surefireClassLoader.loadClass( TestArtifactInfo.class.getName() );
            testArtifactInfoAware = surefireClassLoader.loadClass( TestArtifactInfoAware.class.getName() );
            directoryScannerParameters = surefireClassLoader.loadClass( DirectoryScannerParameters.class.getName() );
            runOrderParameters = surefireClassLoader.loadClass( RunOrderParameters.class.getName() );
            directoryScannerParametersAware =
                surefireClassLoader.loadClass( DirectoryScannerParametersAware.class.getName() );
            testSuiteDefinitionAware = surefireClassLoader.loadClass( TestRequestAware.class.getName() );
            testClassLoaderAware = surefireClassLoader.loadClass( SurefireClassLoadersAware.class.getName() );
            reporterConfigurationAware = surefireClassLoader.loadClass( ReporterConfigurationAware.class.getName() );
            providerPropertiesAware = surefireClassLoader.loadClass( ProviderPropertiesAware.class.getName() );
            reporterFactory = surefireClassLoader.loadClass( ReporterFactory.class.getName() );
            runResult = surefireClassLoader.loadClass( RunResult.class.getName() );
            booterParameters = surefireClassLoader.loadClass( ProviderParameters.class.getName() );
            testListResolver = surefireClassLoader.loadClass( TestListResolver.class.getName() );
            mainCliOptions = surefireClassLoader.loadClass( MainCliOptionsAware.class.getName() );
            commandLineOptionsClass = (Class<Enum>) surefireClassLoader.loadClass( CommandLineOption.class.getName() );
            shutdownAwareClass = surefireClassLoader.loadClass( ShutdownAware.class.getName() );
            shutdownClass = (Class<Enum>) surefireClassLoader.loadClass( Shutdown.class.getName() );
        }
        catch ( ClassNotFoundException e )
        {
            throw new SurefireReflectionException( e );
        }
    }

    public Object convertIfRunResult( Object result )
    {
        if ( result == null || !isRunResult( result ) )
        {
            return result;
        }
        int getCompletedCount1 = (Integer) invokeGetter( result, "getCompletedCount" );
        int getErrors = (Integer) invokeGetter( result, "getErrors" );
        int getSkipped = (Integer) invokeGetter( result, "getSkipped" );
        int getFailures = (Integer) invokeGetter( result, "getFailures" );
        return new RunResult( getCompletedCount1, getErrors, getFailures, getSkipped );

    }

    class ClassLoaderProxy
        implements InvocationHandler
    {
        private final Object target;

        /**
         * @param delegate a target
         */
        ClassLoaderProxy( Object delegate )
        {
            this.target = delegate;
        }

        @Override
        public Object invoke( Object proxy, Method method, Object[] args )
            throws Throwable
        {
            Method delegateMethod = target.getClass().getMethod( method.getName(), method.getParameterTypes() );
            return delegateMethod.invoke( target, args );
        }
    }

    Object createTestRequest( TestRequest suiteDefinition )
    {
        if ( suiteDefinition == null )
        {
            return null;
        }
        else
        {
            Object resolver = createTestListResolver( suiteDefinition.getTestListResolver() );
            Class<?>[] arguments = { List.class, File.class, testListResolver, int.class };
            Constructor constructor = getConstructor( testRequest, arguments );
            return newInstance( constructor,
                                suiteDefinition.getSuiteXmlFiles(),
                                suiteDefinition.getTestSourceDirectory(),
                                resolver,
                                suiteDefinition.getRerunFailingTestsCount() );
        }
    }

    Object createTestListResolver( TestListResolver resolver )
    {
        if ( resolver == null )
        {
            return null;
        }
        else
        {
            Constructor constructor = getConstructor( testListResolver, String.class );
            return newInstance( constructor, resolver.getPluginParameterTest() );
        }
    }

    Object createDirectoryScannerParameters( DirectoryScannerParameters directoryScannerParameters )
    {
        if ( directoryScannerParameters == null )
        {
            return null;
        }
        //Can't use the constructor with the RunOrder parameter. Using it causes some integration tests to fail.
        Class<?>[] arguments = { File.class, List.class, List.class, List.class, boolean.class, String.class };
        Constructor constructor = getConstructor( this.directoryScannerParameters, arguments );
        return newInstance( constructor,
                            directoryScannerParameters.getTestClassesDirectory(),
                            directoryScannerParameters.getIncludes(),
                            directoryScannerParameters.getExcludes(),
                            directoryScannerParameters.getSpecificTests(),
                            directoryScannerParameters.isFailIfNoTests(),
                            RunOrder.asString( directoryScannerParameters.getRunOrder() ) );
    }


    Object createRunOrderParameters( RunOrderParameters runOrderParameters )
    {
        if ( runOrderParameters == null )
        {
            return null;
        }
        //Can't use the constructor with the RunOrder parameter. Using it causes some integration tests to fail.
        Class<?>[] arguments = { String.class, File.class };
        Constructor constructor = getConstructor( this.runOrderParameters, arguments );
        File runStatisticsFile = runOrderParameters.getRunStatisticsFile();
        return newInstance( constructor, RunOrder.asString( runOrderParameters.getRunOrder() ), runStatisticsFile );
    }

    Object createTestArtifactInfo( TestArtifactInfo testArtifactInfo )
    {
        if ( testArtifactInfo == null )
        {
            return null;
        }
        Class<?>[] arguments = { String.class, String.class };
        Constructor constructor = getConstructor( this.testArtifactInfo, arguments );
        return newInstance( constructor, testArtifactInfo.getVersion(), testArtifactInfo.getClassifier() );
    }

    Object createReporterConfiguration( ReporterConfiguration reporterConfig )
    {
        Constructor constructor = getConstructor( reporterConfiguration, File.class, boolean.class );
        return newInstance( constructor, reporterConfig.getReportsDirectory(), reporterConfig.isTrimStackTrace() );
    }

    public Object createBooterConfiguration( ClassLoader surefireClassLoader, Object factoryInstance,
                                             boolean insideFork )
    {
        return instantiateTwoArgs( surefireClassLoader, BaseProviderFactory.class.getName(),
                                   reporterFactory, factoryInstance, boolean.class, insideFork );
    }

    public Object instantiateProvider( String providerClassName, Object booterParameters )
    {
        return instantiateOneArg( surefireClassLoader, providerClassName, this.booterParameters, booterParameters );
    }

    public void setIfDirScannerAware( Object o, DirectoryScannerParameters dirScannerParams )
    {
        if ( directoryScannerParametersAware.isAssignableFrom( o.getClass() ) )
        {
            setDirectoryScannerParameters( o, dirScannerParams );
        }
    }

    public void setMainCliOptions( Object o, List<CommandLineOption> options )
    {
        if ( mainCliOptions.isAssignableFrom( o.getClass() ) )
        {
            List<Enum> newOptions = checkedList( new ArrayList<Enum>( options.size() ), commandLineOptionsClass );
            Collection<Integer> ordinals = toOrdinals( options );
            for ( Enum e : commandLineOptionsClass.getEnumConstants() )
            {
                if ( ordinals.contains( e.ordinal() ) )
                {
                    newOptions.add( e );
                }
            }
            invokeSetter( o, "setMainCliOptions", List.class, newOptions );
        }
    }

    public void setSkipAfterFailureCount( Object o, int skipAfterFailureCount )
    {
        invokeSetter( o, "setSkipAfterFailureCount", int.class, skipAfterFailureCount );
    }

    public void setShutdown( Object o, Shutdown shutdown )
    {
        if ( shutdownAwareClass.isAssignableFrom( o.getClass() ) )
        {
            for ( Enum e : shutdownClass.getEnumConstants() )
            {
                if ( shutdown.ordinal() == e.ordinal() )
                {
                    invokeSetter( o, "setShutdown", shutdownClass, e );
                    break;
                }
            }
        }
    }

    public void setSystemExitTimeout( Object o, Integer systemExitTimeout )
    {
        invokeSetter( o, "setSystemExitTimeout", Integer.class, systemExitTimeout );
    }

    public void setDirectoryScannerParameters( Object o, DirectoryScannerParameters dirScannerParams )
    {
        Object param = createDirectoryScannerParameters( dirScannerParams );
        invokeSetter( o, "setDirectoryScannerParameters", directoryScannerParameters, param );
    }

    public void setRunOrderParameters( Object o, RunOrderParameters runOrderParameters )
    {
        Object param = createRunOrderParameters( runOrderParameters );
        invokeSetter( o, "setRunOrderParameters", this.runOrderParameters, param );
    }

    public void setTestSuiteDefinitionAware( Object o, TestRequest testSuiteDefinition2 )
    {
        if ( testSuiteDefinitionAware.isAssignableFrom( o.getClass() ) )
        {
            setTestSuiteDefinition( o, testSuiteDefinition2 );
        }
    }

    void setTestSuiteDefinition( Object o, TestRequest testSuiteDefinition1 )
    {
        Object param = createTestRequest( testSuiteDefinition1 );
        invokeSetter( o, "setTestRequest", testRequest, param );
    }

    public void setProviderPropertiesAware( Object o, Map<String, String> properties )
    {
        if ( providerPropertiesAware.isAssignableFrom( o.getClass() ) )
        {
            setProviderProperties( o, properties );
        }
    }

    void setProviderProperties( Object o, Map<String, String> providerProperties )
    {
        invokeSetter( o, "setProviderProperties", Map.class, providerProperties );
    }

    public void setReporterConfigurationAware( Object o, ReporterConfiguration reporterConfiguration1 )
    {
        if ( reporterConfigurationAware.isAssignableFrom( o.getClass() ) )
        {
            setReporterConfiguration( o, reporterConfiguration1 );
        }
    }


    void setReporterConfiguration( Object o, ReporterConfiguration reporterConfiguration )
    {
        Object param = createReporterConfiguration( reporterConfiguration );
        invokeSetter( o, "setReporterConfiguration", this.reporterConfiguration, param );
    }

    public void setTestClassLoaderAware( Object o, ClassLoader testClassLoader )
    {
        if ( testClassLoaderAware.isAssignableFrom( o.getClass() ) )
        {
            setTestClassLoader( o, testClassLoader );
        }
    }

    void setTestClassLoader( Object o, ClassLoader testClassLoader )
    {
        Method setter = getMethod( o, "setClassLoaders", ClassLoader.class );
        invokeMethodWithArray( o, setter, testClassLoader );
    }

    public void setTestArtifactInfoAware( Object o, TestArtifactInfo testArtifactInfo1 )
    {
        if ( testArtifactInfoAware.isAssignableFrom( o.getClass() ) )
        {
            setTestArtifactInfo( o, testArtifactInfo1 );
        }
    }

    void setTestArtifactInfo( Object o, TestArtifactInfo testArtifactInfo )
    {
        Object param = createTestArtifactInfo( testArtifactInfo );
        invokeSetter( o, "setTestArtifactInfo", this.testArtifactInfo, param );
    }

    private boolean isRunResult( Object o )
    {
        return runResult.isAssignableFrom( o.getClass() );
    }

    public Object createConsoleLogger( @Nonnull ConsoleLogger consoleLogger )
    {
        return createConsoleLogger( consoleLogger, surefireClassLoader );
    }

    private static Collection<Integer> toOrdinals( Collection<? extends Enum> enums )
    {
        Collection<Integer> ordinals = new ArrayList<>( enums.size() );
        for ( Enum e : enums )
        {
            ordinals.add( e.ordinal() );
        }
        return ordinals;
    }

    public static Object createConsoleLogger( ConsoleLogger consoleLogger, ClassLoader cl )
    {
        try
        {
            Class<?> decoratorClass = cl.loadClass( ConsoleLoggerDecorator.class.getName() );
            return getConstructor( decoratorClass, Object.class ).newInstance( consoleLogger );
        }
        catch ( Exception e )
        {
            throw new SurefireReflectionException( e );
        }
    }

}
