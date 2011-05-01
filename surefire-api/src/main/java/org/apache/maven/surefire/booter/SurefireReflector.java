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
import org.apache.maven.plugin.surefire.report.ReporterManagerFactory;
import org.apache.maven.surefire.forking.ForkConfigurationInfo;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestRequest;
import org.apache.maven.surefire.util.ReflectionUtils;
import org.apache.maven.surefire.util.SurefireReflectionException;

/**
 * Does reflection based invocation of the surefire methods.
 * <p/>
 * This is to avoid compilications with linkage issues
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

    private final Class directoryScannerParametersAware;

    private final Class testSuiteDefinitionAware;

    private final Class testClassLoaderAware;

    private final Class reporterConfigurationAware;

    private final Class providerPropertiesAware;

    private final Class runResult;

    private final Class booterParameters;

    private final Class reporterFactory;

    private final Class forkConfigurationInfo;

    private final Class startupReportConfiguration;

    public SurefireReflector( ClassLoader surefireClassLoader )
    {
        this.surefireClassLoader = surefireClassLoader;
        try
        {
            reporterConfiguration = surefireClassLoader.loadClass( ReporterConfiguration.class.getName() );
            startupReportConfiguration = surefireClassLoader.loadClass( StartupReportConfiguration.class.getName() );
            testRequest = surefireClassLoader.loadClass( TestRequest.class.getName() );
            testArtifactInfo = surefireClassLoader.loadClass( TestArtifactInfo.class.getName() );
            testArtifactInfoAware = surefireClassLoader.loadClass( TestArtifactInfoAware.class.getName() );
            directoryScannerParameters = surefireClassLoader.loadClass( DirectoryScannerParameters.class.getName() );
            directoryScannerParametersAware =
                surefireClassLoader.loadClass( DirectoryScannerParametersAware.class.getName() );
            testSuiteDefinitionAware = surefireClassLoader.loadClass( TestRequestAware.class.getName() );
            testClassLoaderAware = surefireClassLoader.loadClass( SurefireClassLoadersAware.class.getName() );
            reporterConfigurationAware = surefireClassLoader.loadClass( ReporterConfigurationAware.class.getName() );
            providerPropertiesAware = surefireClassLoader.loadClass( ProviderPropertiesAware.class.getName() );
            forkConfigurationInfo = surefireClassLoader.loadClass( ForkConfigurationInfo.class.getName() );
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
        return new RunResult( getCompletedCount1.intValue(), getErrors.intValue(), getFailures.intValue(),
                              getSkipped.intValue() );

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
        Class[] arguments = { List.class, File.class, String.class, String.class };
        Constructor constructor = ReflectionUtils.getConstructor( this.testRequest, arguments );
        return ReflectionUtils.newInstance( constructor, new Object[]{ suiteDefinition.getSuiteXmlFiles(),
            suiteDefinition.getTestSourceDirectory(), suiteDefinition.getRequestedTest(),
            suiteDefinition.getRequestedTestMethod() } );
    }


    Object createDirectoryScannerParameters( DirectoryScannerParameters directoryScannerParameters )
    {
        if ( directoryScannerParameters == null )
        {
            return null;
        }
        Class[] arguments = { File.class, List.class, List.class, Boolean.class, String.class };
        Constructor constructor = ReflectionUtils.getConstructor( this.directoryScannerParameters, arguments );
        return ReflectionUtils.newInstance( constructor,
                                            new Object[]{ directoryScannerParameters.getTestClassesDirectory(),
                                                directoryScannerParameters.getIncludes(),
                                                directoryScannerParameters.getExcludes(),
                                                directoryScannerParameters.isFailIfNoTests(),
                                                directoryScannerParameters.getRunOrder() } );
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

    Object createForkConfigurationInfo( ForkConfigurationInfo forkConfigurationInfo )
    {
        if ( forkConfigurationInfo == null )
        {
            return null;
        }

        final Class[] arguments = { String.class, Boolean.class };
        Constructor constructor = ReflectionUtils.getConstructor( this.forkConfigurationInfo, arguments );
        return ReflectionUtils.newInstance( constructor, new Object[]{ forkConfigurationInfo.getForkMode(),
            forkConfigurationInfo.getInFork() } );
    }


    Object createReporterConfiguration( ReporterConfiguration reporterConfiguration )
    {
        Constructor constructor = ReflectionUtils.getConstructor( this.reporterConfiguration,
                                                                  new Class[]{ File.class, Boolean.class } );
        return ReflectionUtils.newInstance( constructor, new Object[]{ reporterConfiguration.getReportsDirectory(),
            reporterConfiguration.isTrimStackTrace()} );
    }

    Object createStartupReportConfiguration( StartupReportConfiguration reporterConfiguration )
    {
        Constructor constructor = ReflectionUtils.getConstructor( this.startupReportConfiguration,
                                                                  new Class[]{ boolean.class, boolean.class,
                                                                      String.class, boolean.class, boolean.class,
                                                                      File.class, boolean.class } );
        //noinspection BooleanConstructorCall
        final Object[] params =
            { new Boolean( reporterConfiguration.isUseFile() ), new Boolean( reporterConfiguration.isPrintSummary() ),
                reporterConfiguration.getReportFormat(),
                new Boolean( reporterConfiguration.isRedirectTestOutputToFile() ),
                new Boolean( reporterConfiguration.isDisableXmlReport() ),
                reporterConfiguration.getReportsDirectory(),
                new Boolean( reporterConfiguration.isTrimStackTrace())};
        return ReflectionUtils.newInstance( constructor, params );
    }

    public Object createForkingReporterFactory( Boolean trimStackTrace, PrintStream originalSystemOut )
    {
        Class[] args = new Class[]{ Boolean.class, PrintStream.class };
        Object[] values = new Object[]{ trimStackTrace, originalSystemOut };
        return ReflectionUtils.instantiateObject( ForkingReporterFactory.class.getName(), args, values,
                                                  surefireClassLoader );
    }

    public Object createReportingReporterFactory( StartupReportConfiguration startupReportConfiguration )
    {
        Class[] args =
            new Class[]{ ClassLoader.class, this.startupReportConfiguration };
        Object src = createStartupReportConfiguration( startupReportConfiguration );
        Object[] params = new Object[]{ this.surefireClassLoader, src };
        return ReflectionUtils.instantiateObject( ReporterManagerFactory.class.getName(), args, params,
                                                  surefireClassLoader );

    }

    public Object createBooterConfiguration( ClassLoader surefireClassLoader, Object factoryInstance )
    {
        return ReflectionUtils.instantiateOneArg( surefireClassLoader, BaseProviderFactory.class.getName(),
                                                  reporterFactory, factoryInstance );
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


    public void setForkConfigurationInfo( Object o, ForkConfigurationInfo forkConfigurationInfo )
    {
        if ( forkConfigurationInfo == null )
        {
            throw new IllegalArgumentException( "ForkConfiguration cannot be null" );
        }
        final Object forkConfig = createForkConfigurationInfo( forkConfigurationInfo );
        ReflectionUtils.invokeSetter( o, "setForkConfigurationInfo", this.forkConfigurationInfo, forkConfig );
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

    public void setTestClassLoaderAware( Object o, ClassLoader surefireClassLoader, ClassLoader testClassLoader )
    {
        if ( testClassLoaderAware.isAssignableFrom( o.getClass() ) )
        {
            setTestClassLoader( o, surefireClassLoader, testClassLoader );
        }
    }

    void setTestClassLoader( Object o, ClassLoader surefireClassLoader, ClassLoader testClassLoader )
    {
        final Method setter =
            ReflectionUtils.getMethod( o, "setClassLoaders", new Class[]{ ClassLoader.class, ClassLoader.class } );
        ReflectionUtils.invokeMethodWithArray( o, setter, new Object[]{ surefireClassLoader, testClassLoader } );
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
