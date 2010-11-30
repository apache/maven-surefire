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

import org.apache.maven.surefire.ProviderInvoker;
import org.apache.maven.surefire.providerapi.DirectoryScannerParametersAware;
import org.apache.maven.surefire.providerapi.ProviderPropertiesAware;
import org.apache.maven.surefire.providerapi.ReporterConfigurationAware;
import org.apache.maven.surefire.providerapi.TestArtifactInfoAware;
import org.apache.maven.surefire.providerapi.TestClassLoaderAware;
import org.apache.maven.surefire.providerapi.TestSuiteDefinitionAware;
import org.apache.maven.surefire.report.ReporterConfiguration;
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

    private final Class providerInvoker;

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


    public SurefireReflector( ClassLoader surefireClassLoader )
    {
        this.classLoader = surefireClassLoader;
        try
        {
            providerInvoker = surefireClassLoader.loadClass( ProviderInvoker.class.getName() );
            reporterConfiguration = surefireClassLoader.loadClass( ReporterConfiguration.class.getName() );
            testSuiteDefinition = surefireClassLoader.loadClass( TestRequest.class.getName() );
            testArtifactInfo = surefireClassLoader.loadClass( TestArtifactInfo.class.getName() );
            testArtifactInfoAware = surefireClassLoader.loadClass( TestArtifactInfoAware.class.getName() );
            directoryScannerParameters = surefireClassLoader.loadClass( DirectoryScannerParameters.class.getName() );
            directoryScannerParametersAware =
                surefireClassLoader.loadClass( DirectoryScannerParametersAware.class.getName() );
            testSuiteDefinitionAware = surefireClassLoader.loadClass( TestSuiteDefinitionAware.class.getName() );
            testClassLoaderAware = surefireClassLoader.loadClass( TestClassLoaderAware.class.getName() );
            reporterConfigurationAware = surefireClassLoader.loadClass( ReporterConfigurationAware.class.getName() );
            providerPropertiesAware = surefireClassLoader.loadClass( ProviderPropertiesAware.class.getName() );
        }
        catch ( ClassNotFoundException e )
        {
            throw new RuntimeException( "When loading class", e );
        }
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


    public int runProvider( ReporterConfiguration reporterConfiguration, List reportDefinitions,
                            ClassLoader surefireClassLoader, ClassLoader testsClassLoader, Properties results,
                            Boolean failIfNoTests, TestRequest testSuiteDefinition, TestArtifactInfo testArtifactInfo,
                            String providerClassName, DirectoryScannerParameters dirScannerParams, Object forkTestSet )
    {
        Object surefire = instantiateProviderInvoker();
        Method run = getRunMethod(
            new Class[]{ this.reporterConfiguration, List.class, ClassLoader.class, ClassLoader.class, Properties.class,
                Boolean.class, this.testSuiteDefinition, this.testArtifactInfo, String.class,
                this.directoryScannerParameters, Object.class } );

        Object[] args = { createReporterConfiguration( reporterConfiguration ), reportDefinitions, surefireClassLoader,
            testsClassLoader, results, failIfNoTests, createTestSuiteDefinition( testSuiteDefinition ),
            createTestArtifactInfo( testArtifactInfo ), providerClassName,
            createDirectoryScannerParameters( dirScannerParams ), forkTestSet };
        return invokeRunMethod( surefire, run, args );
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
            getConstructor( this.reporterConfiguration, new Class[]{ File.class, Boolean.class } );
        return newInstance( constructor, new Object[]{ reporterConfiguration.getReportsDirectory(),
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

    public Object newInstance( String providerClassName )
    {
        try
        {

            Class clazz = classLoader.loadClass( providerClassName );
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

    public static Object getSuites( Object surefireProvider )
    {
        final Method getSuites = getMethod( surefireProvider, "getSuites", new Class[]{ } );
        return invokeMethod( surefireProvider, getSuites, new Object[]{ } );
    }


    private int invokeRunMethod( Object surefire, Method method, Object[] args )
        throws RuntimeException

    {
        try
        {
            final Integer invoke = (Integer) method.invoke( surefire, args );
            return invoke.intValue();
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

    public Object instantiateProviderInvoker()
        throws RuntimeException
    {
        try
        {
            return providerInvoker.newInstance();
        }
        catch ( InstantiationException e )
        {
            throw new RuntimeException( "When instanitating surefire", e );
        }
        catch ( IllegalAccessException e )
        {
            throw new RuntimeException( "When instanitating surefire", e );
        }
    }

    private Method getRunMethod( Class[] parameters )
    {
        try
        {
            return providerInvoker.getMethod( "run", parameters );
        }
        catch ( NoSuchMethodException e )
        {
            throw new RuntimeException( "When finding run method", e );
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
        final Method setter = getMethod( o, "setTestSuiteDefinition", new Class[]{ testSuiteDefinition } );
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

    boolean isReporterConfigurationAwareAware( Object o )
    {
        return reporterConfigurationAware.isAssignableFrom( o.getClass() );
    }

    void setReporterConfiguration( Object o, ReporterConfiguration reporterConfiguration )
    {
        final Object param = createReporterConfiguration( reporterConfiguration );
        final Method setter = getMethod( o, "setReporterConfiguration", new Class[]{ this.reporterConfiguration } );
        invokeSetter( o, setter, param );
    }

    public void setTestClassLoaderAware( Object o, ClassLoader testClassLoader )
    {
        if ( isTestClassLoaderAware( o ) )
        {
            setTestClassLoader( o, testClassLoader );
        }
    }

    boolean isTestClassLoaderAware( Object o )
    {
        return testClassLoaderAware.isAssignableFrom( o.getClass() );
    }

    void setTestClassLoader( Object o, ClassLoader aClassLoader )
    {
        final Method setter = getMethod( o, "setTestClassLoader", new Class[]{ ClassLoader.class } );
        invokeSetter( o, setter, aClassLoader );
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

}
