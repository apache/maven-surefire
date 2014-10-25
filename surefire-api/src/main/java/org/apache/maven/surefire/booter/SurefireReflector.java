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
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.RunOrderParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestRequest;
import org.apache.maven.surefire.util.ReflectionUtils;
import org.apache.maven.surefire.util.RunOrder;
import org.apache.maven.surefire.util.SurefireReflectionException;

/**
 * Does reflection based invocation of the surefire methods.
 * <p/>
 * This is to avoid complications with linkage issues
 *
 * @author Kristian Rosenvold
 */
public class SurefireReflector
{
    private final ClassLoader surefireClassLoader;

    private final Class reporterConfiguration;

    private final Class testRequest;

    private final Class testArtifactInfo;

    private final Class testArtifactInfoAware;

    private final Class directoryScannerParameters;

    private final Class runOrderParameters;

    private final Class directoryScannerParametersAware;

    private final Class testSuiteDefinitionAware;

    private final Class testClassLoaderAware;

    private final Class reporterConfigurationAware;

    private final Class providerPropertiesAware;

    private final Class runResult;

    private final Class booterParameters;

    private final Class reporterFactory;


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
        final Integer getCompletedCount1 = (Integer) ReflectionUtils.invokeGetter( result, "getCompletedCount" );
        final Integer getErrors = (Integer) ReflectionUtils.invokeGetter( result, "getErrors" );
        final Integer getSkipped = (Integer) ReflectionUtils.invokeGetter( result, "getSkipped" );
        final Integer getFailures = (Integer) ReflectionUtils.invokeGetter( result, "getFailures" );
        return new RunResult( getCompletedCount1, getErrors, getFailures, getSkipped );

    }


    /**
     * @noinspection UnusedDeclaration
     */
    class ClassLoaderProxy
        implements InvocationHandler
    {
        private final Object target;

        /**
         * @param delegate a target
         * @noinspection UnusedDeclaration
         */
        public ClassLoaderProxy( Object delegate )
        {
            this.target = delegate;
        }

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
        Class[] arguments = { List.class, File.class, String.class, String.class, int.class };
        Constructor constructor = ReflectionUtils.getConstructor( this.testRequest, arguments );
        return ReflectionUtils.newInstance( constructor, new Object[]{ suiteDefinition.getSuiteXmlFiles(),
            suiteDefinition.getTestSourceDirectory(), suiteDefinition.getRequestedTest(),
            suiteDefinition.getRequestedTestMethod(), suiteDefinition.getRerunFailingTestsCount() } );
    }


    Object createDirectoryScannerParameters( DirectoryScannerParameters directoryScannerParameters )
    {
        if ( directoryScannerParameters == null )
        {
            return null;
        }
        //Can't use the constructor with the RunOrder parameter. Using it causes some integration tests to fail.
        Class[] arguments = { File.class, List.class, List.class, List.class, Boolean.class, String.class };
        Constructor constructor = ReflectionUtils.getConstructor( this.directoryScannerParameters, arguments );
        return ReflectionUtils.newInstance( constructor,
                                            new Object[]{ directoryScannerParameters.getTestClassesDirectory(),
                                                directoryScannerParameters.getIncludes(),
                                                directoryScannerParameters.getExcludes(),
                                                directoryScannerParameters.getSpecificTests(),
                                                directoryScannerParameters.isFailIfNoTests(),
                                                RunOrder.asString( directoryScannerParameters.getRunOrder() ) } );
    }


    Object createRunOrderParameters( RunOrderParameters runOrderParameters )
    {
        if ( runOrderParameters == null )
        {
            return null;
        }
        //Can't use the constructor with the RunOrder parameter. Using it causes some integration tests to fail.
        Class[] arguments = { String.class, String.class };
        Constructor constructor = ReflectionUtils.getConstructor( this.runOrderParameters, arguments );
        final File runStatisticsFile = runOrderParameters.getRunStatisticsFile();
        return ReflectionUtils.newInstance( constructor,
                                            new Object[]{ RunOrder.asString( runOrderParameters.getRunOrder() ),
                                                runStatisticsFile != null
                                                    ? runStatisticsFile.getAbsolutePath()
                                                    : null } );
    }

    Object createTestArtifactInfo( TestArtifactInfo testArtifactInfo )
    {
        if ( testArtifactInfo == null )
        {
            return null;
        }
        Class[] arguments = { String.class, String.class };
        Constructor constructor = ReflectionUtils.getConstructor( this.testArtifactInfo, arguments );
        return ReflectionUtils.newInstance( constructor, new Object[]{ testArtifactInfo.getVersion(),
            testArtifactInfo.getClassifier() } );
    }


    Object createReporterConfiguration( ReporterConfiguration reporterConfiguration )
    {
        Constructor constructor =
            ReflectionUtils.getConstructor( this.reporterConfiguration, new Class[]{ File.class, Boolean.class } );
        return ReflectionUtils.newInstance( constructor, new Object[]{ reporterConfiguration.getReportsDirectory(),
            reporterConfiguration.isTrimStackTrace() } );
    }

    public static ReporterFactory createForkingReporterFactoryInCurrentClassLoader( Boolean trimStackTrace,
                                                                                    PrintStream originalSystemOut )
    {
        return new ForkingReporterFactory( trimStackTrace, originalSystemOut );
    }

    public Object createBooterConfiguration( ClassLoader surefireClassLoader, Object factoryInstance,
                                             boolean insideFork )
    {
        return ReflectionUtils.instantiateTwoArgs( surefireClassLoader, BaseProviderFactory.class.getName(),
                                                   reporterFactory, factoryInstance, Boolean.class,
                                                   insideFork ? Boolean.TRUE : Boolean.FALSE );
    }

    public Object instantiateProvider( String providerClassName, Object booterParameters )
    {
        return ReflectionUtils.instantiateOneArg( surefireClassLoader, providerClassName, this.booterParameters,
                                                  booterParameters );
    }

    public void setIfDirScannerAware( Object o, DirectoryScannerParameters dirScannerParams )
    {
        if ( directoryScannerParametersAware.isAssignableFrom( o.getClass() ) )
        {
            setDirectoryScannerParameters( o, dirScannerParams );
        }
    }

    public void setDirectoryScannerParameters( Object o, DirectoryScannerParameters dirScannerParams )
    {
        final Object param = createDirectoryScannerParameters( dirScannerParams );
        ReflectionUtils.invokeSetter( o, "setDirectoryScannerParameters", this.directoryScannerParameters, param );
    }

    public void setRunOrderParameters( Object o, RunOrderParameters runOrderParameters )
    {
        final Object param = createRunOrderParameters( runOrderParameters );
        ReflectionUtils.invokeSetter( o, "setRunOrderParameters", this.runOrderParameters, param );
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
        final Object param = createTestRequest( testSuiteDefinition1 );
        ReflectionUtils.invokeSetter( o, "setTestRequest", this.testRequest, param );
    }

    public void setProviderPropertiesAware( Object o, Properties properties )
    {
        if ( providerPropertiesAware.isAssignableFrom( o.getClass() ) )
        {
            setProviderProperties( o, properties );
        }
    }

    void setProviderProperties( Object o, Properties providerProperties )
    {
        ReflectionUtils.invokeSetter( o, "setProviderProperties", Properties.class, providerProperties );
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
        final Object param = createReporterConfiguration( reporterConfiguration );
        ReflectionUtils.invokeSetter( o, "setReporterConfiguration", this.reporterConfiguration, param );
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
        final Method setter =
            ReflectionUtils.getMethod( o, "setClassLoaders", new Class[]{ ClassLoader.class } );
        ReflectionUtils.invokeMethodWithArray( o, setter, new Object[]{ testClassLoader } );
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
        final Object param = createTestArtifactInfo( testArtifactInfo );
        ReflectionUtils.invokeSetter( o, "setTestArtifactInfo", this.testArtifactInfo, param );
    }

    private boolean isRunResult( Object o )
    {
        return runResult.isAssignableFrom( o.getClass() );
    }

}
