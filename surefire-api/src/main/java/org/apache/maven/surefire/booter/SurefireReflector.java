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

import org.apache.maven.surefire.providerapi.BaseProviderFactory;
import org.apache.maven.surefire.providerapi.BooterParameters;
import org.apache.maven.surefire.providerapi.DirectoryScannerParametersAware;
import org.apache.maven.surefire.providerapi.ProviderPropertiesAware;
import org.apache.maven.surefire.providerapi.ReporterConfigurationAware;
import org.apache.maven.surefire.providerapi.SurefireClassLoadersAware;
import org.apache.maven.surefire.providerapi.TestArtifactInfoAware;
import org.apache.maven.surefire.providerapi.TestRequestAware;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestRequest;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;

/**
 * Does reflection based invocation of the surefire methods.
 * <p/>
 * This is to avoid compilications with linkage issues
 *
 * @author Kristian Rosenvold
 */
public class SurefireReflector
{
    private final ClassLoader classLoader;

    private final Class reporterConfiguration;

    private final Class testSuiteDefinition;

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

    private static final Class[] noargs = new Class[0];

    private static final Object[] noargsValues = new Object[0];

    public SurefireReflector( ClassLoader surefireClassLoader )
    {
        this.classLoader = surefireClassLoader;
        try
        {
            reporterConfiguration = surefireClassLoader.loadClass( ReporterConfiguration.class.getName() );
            testSuiteDefinition = surefireClassLoader.loadClass( TestRequest.class.getName() );
            testArtifactInfo = surefireClassLoader.loadClass( TestArtifactInfo.class.getName() );
            testArtifactInfoAware = surefireClassLoader.loadClass( TestArtifactInfoAware.class.getName() );
            directoryScannerParameters = surefireClassLoader.loadClass( DirectoryScannerParameters.class.getName() );
            directoryScannerParametersAware =
                surefireClassLoader.loadClass( DirectoryScannerParametersAware.class.getName() );
            testSuiteDefinitionAware = surefireClassLoader.loadClass( TestRequestAware.class.getName() );
            testClassLoaderAware = surefireClassLoader.loadClass( SurefireClassLoadersAware.class.getName() );
            reporterConfigurationAware = surefireClassLoader.loadClass( ReporterConfigurationAware.class.getName() );
            providerPropertiesAware = surefireClassLoader.loadClass( ProviderPropertiesAware.class.getName() );
            runResult = surefireClassLoader.loadClass( RunResult.class.getName() );
            booterParameters= surefireClassLoader.loadClass( BooterParameters.class.getName() );
        }
        catch ( ClassNotFoundException e )
        {
            throw new RuntimeException( "When loading class", e );
        }
    }

    public Object convertIfRunResult( Object result )
    {
        if ( result == null || !isRunResult( result ) )
        {
            return result;
        }
        final Integer getCompletedCount1 = (Integer) invokeGetter( result, "getCompletedCount" );
        final Integer getErrors = (Integer) invokeGetter( result, "getErrors" );
        final Integer getSkipped = (Integer) invokeGetter( result, "getSkipped" );
        final Integer getFailures = (Integer) invokeGetter( result, "getFailures" );
        return new RunResult( getCompletedCount1.intValue(), getErrors.intValue(),
                              getFailures.intValue(), getSkipped.intValue() );

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


    Object createTestSuiteDefinition( TestRequest suiteDefinition )
    {
        if ( suiteDefinition == null )
        {
            return null;
        }
        Class[] arguments = { Object[].class, File.class, String.class };
        Constructor constructor = getConstructor( this.testSuiteDefinition, arguments );
        return newInstance( constructor,
                            new Object[]{ suiteDefinition.getSuiteXmlFiles(), suiteDefinition.getTestSourceDirectory(),
                                suiteDefinition.getRequestedTest() } );
    }


    Object createDirectoryScannerParameters( DirectoryScannerParameters directoryScannerParameters )
    {
        if ( directoryScannerParameters == null )
        {
            return null;
        }
        Class[] arguments = { File.class, List.class, List.class, Boolean.class };
        Constructor constructor = getConstructor( this.directoryScannerParameters, arguments );
        return newInstance( constructor, new Object[]{ directoryScannerParameters.getTestClassesDirectory(),
            directoryScannerParameters.getIncludes(), directoryScannerParameters.getExcludes(),
            directoryScannerParameters.isFailIfNoTests() } );
    }

    Object createTestArtifactInfo( TestArtifactInfo testArtifactInfo )
    {
        if ( testArtifactInfo == null )
        {
            return null;
        }
        Class[] arguments = { String.class, String.class };
        Constructor constructor = getConstructor( this.testArtifactInfo, arguments );
        return newInstance( constructor,
                            new Object[]{ testArtifactInfo.getVersion(), testArtifactInfo.getClassifier() } );
    }

    Object createReporterConfiguration( ReporterConfiguration reporterConfiguration )
    {
        Constructor constructor =
            getConstructor( this.reporterConfiguration, new Class[]{ List.class, File.class, Boolean.class } );
        return newInstance( constructor, new Object[]{ reporterConfiguration.getReports(), reporterConfiguration.getReportsDirectory(),
            reporterConfiguration.isTrimStackTrace() } );
    }

    private Constructor getConstructor( Class clazz, Class[] arguments )
    {
        try
        {
            return clazz.getConstructor( arguments );
        }
        catch ( NoSuchMethodException e )
        {
            throw new RuntimeException( e );
        }
    }

    private Object newInstance( Constructor constructor, Object[] params )
    {
        try
        {
            return constructor.newInstance( params );
        }
        catch ( InvocationTargetException e )
        {
            throw new RuntimeException( e );
        }
        catch ( InstantiationException e )
        {
            throw new RuntimeException( e );
        }
        catch ( IllegalAccessException e )
        {
            throw new RuntimeException( e );
        }
    }

    public Object createBooterConfiguration()
    {
        try
        {

            Class clazz = classLoader.loadClass( BaseProviderFactory.class.getName());
            return clazz.newInstance();
        }
        catch ( InstantiationException e )
        {
            throw new RuntimeException( e );
        }
        catch ( IllegalAccessException e )
        {
            throw new RuntimeException( e );
        }
        catch ( ClassNotFoundException e )
        {
            throw new RuntimeException( e );
        }
    }
    public Object instantiateProvider( String providerClassName, Object booterParameters )
    {

        try
        {
            Class aClass = classLoader.loadClass( providerClassName );
            Constructor constructor = getConstructor(  aClass,  new Class[]{ this.booterParameters } );
            return constructor.newInstance( new Object []{booterParameters} );
        }
        catch ( ClassNotFoundException e )
        {
            throw new RuntimeException( e );
        }
        catch ( InvocationTargetException e )
        {
            throw new RuntimeException( e );
        }
        catch ( InstantiationException e )
        {
            throw new RuntimeException( e );
        }
        catch ( IllegalAccessException e )
        {
            throw new RuntimeException( e );
        }
    }


    private static Object invokeSetter( Object surefire, Method method, Object value )

    {
        try
        {
            return method.invoke( surefire, new Object[]{ value } );
        }
        catch ( IllegalAccessException e )
        {
            throw new RuntimeException( "When instantiating surefire", e );
        }
        catch ( InvocationTargetException e )
        {
            throw new RuntimeException( e.getTargetException().getMessage(), e.getTargetException() );
        }

    }

    private static Object invokeMethod( Object surefire, Method method, Object[] args )

    {
        try
        {
            return method.invoke( surefire, args );
        }
        catch ( IllegalAccessException e )
        {
            throw new RuntimeException( "When instantiating surefire", e );
        }
        catch ( InvocationTargetException e )
        {
            throw new RuntimeException( e.getTargetException().getMessage(), e.getTargetException() );
        }

    }

    private static Object invokeGetter( Object instance, String methodName )
    {
        try
        {
            final Method method = instance.getClass().getMethod( methodName, noargs );
            return method.invoke( instance, noargsValues );
        }
        catch ( NoSuchMethodException e )
        {
            throw new RuntimeException( "When finding method " + methodName, e );
        }
        catch ( InvocationTargetException e )
        {
            throw new RuntimeException( "When running method " + methodName, e );
        }
        catch ( IllegalAccessException e )
        {
            throw new RuntimeException( "When accessing method " + methodName, e );
        }
    }

    private static Method getMethod( Object instance, String methodName, Class[] parameters )
    {
        try
        {
            return instance.getClass().getMethod( methodName, parameters );
        }
        catch ( NoSuchMethodException e )
        {
            throw new RuntimeException( "When finding method " + methodName, e );
        }
    }

    public void setIfDirScannerAware( Object o, DirectoryScannerParameters dirScannerParams )
    {
        if ( isDirectoryScannerParameterAware( o ) )
        {
            setDirectoryScannerParameters( o, dirScannerParams );
        }
    }

    boolean isDirectoryScannerParameterAware( Object o )
    {
        return directoryScannerParametersAware.isAssignableFrom( o.getClass() );
    }

    public void setDirectoryScannerParameters( Object o, DirectoryScannerParameters dirScannerParams )
    {
        final Object param = createDirectoryScannerParameters( dirScannerParams );
        final Method setter =
            getMethod( o, "setDirectoryScannerParameters", new Class[]{ directoryScannerParameters } );
        invokeSetter( o, setter, param );
    }

    public void setTestSuiteDefinitionAware( Object o, TestRequest testSuiteDefinition2 )
    {
        if ( isTestSuiteDefinitionAware( o ) )
        {
            setTestSuiteDefinition( o, testSuiteDefinition2 );
        }
    }


    boolean isTestSuiteDefinitionAware( Object o )
    {
        return testSuiteDefinitionAware.isAssignableFrom( o.getClass() );
    }

    void setTestSuiteDefinition( Object o, TestRequest testSuiteDefinition1 )
    {
        final Object param = createTestSuiteDefinition( testSuiteDefinition1 );
        final Method setter = getMethod( o, "setTestRequest", new Class[]{ testSuiteDefinition } );
        invokeSetter( o, setter, param );
    }

    public void setProviderPropertiesAware( Object o, Properties properties )
    {
        if ( isProviderPropertiesAware( o ) )
        {
            setProviderProperties( o, properties );
        }
    }


    boolean isProviderPropertiesAware( Object o )
    {
        return providerPropertiesAware.isAssignableFrom( o.getClass() );
    }

    void setProviderProperties( Object o, Properties providerProperties )
    {
        final Method setter = getMethod( o, "setProviderProperties", new Class[]{ Properties.class } );
        invokeSetter( o, setter, providerProperties );
    }

    public void setReporterConfigurationAware( Object o, ReporterConfiguration reporterConfiguration1 )
    {
        if ( isReporterConfigurationAwareAware( o ) )
        {
            setReporterConfiguration( o, reporterConfiguration1 );
        }
    }


    void setReporterConfiguration( Object o, ReporterConfiguration reporterConfiguration )
    {
        final Object param = createReporterConfiguration( reporterConfiguration );
        final Method setter = getMethod( o, "setReporterConfiguration", new Class[]{ this.reporterConfiguration } );
        invokeSetter( o, setter, param );
    }

    boolean isReporterConfigurationAwareAware( Object o )
    {
        return reporterConfigurationAware.isAssignableFrom( o.getClass() );
    }

    public void setTestClassLoaderAware( Object o, ClassLoader surefireClassLoader, ClassLoader testClassLoader)
    {
        if ( isTestClassLoaderAware( o ) )
        {
            setTestClassLoader( o, surefireClassLoader, testClassLoader );
        }
    }

    boolean isTestClassLoaderAware( Object o )
    {
        return testClassLoaderAware.isAssignableFrom( o.getClass() );
    }

    void setTestClassLoader( Object o, ClassLoader surefireClassLoader, ClassLoader testClassLoader )
    {
        final Method setter = getMethod( o, "setClassLoaders", new Class[]{ ClassLoader.class, ClassLoader.class  } );
        invokeMethod( o, setter, new Object[]{ surefireClassLoader, testClassLoader } );
    }

    public void setTestArtifactInfoAware( Object o, TestArtifactInfo testArtifactInfo1 )
    {
        if ( isTestArtifactInfoAware( o ) )
        {
            setTestArtifactInfo( o, testArtifactInfo1 );
        }
    }

    public boolean isTestArtifactInfoAware( Object o )
    {
        return testArtifactInfoAware.isAssignableFrom( o.getClass() );
    }

    void setTestArtifactInfo( Object o, TestArtifactInfo testArtifactInfo )
    {
        final Object param = createTestArtifactInfo( testArtifactInfo );
        final Method setter = getMethod( o, "setTestArtifactInfo", new Class[]{ this.testArtifactInfo } );
        invokeSetter( o, setter, param );
    }

    public boolean isRunResult( Object o )
    {
        return runResult.isAssignableFrom( o.getClass() );
    }
}
