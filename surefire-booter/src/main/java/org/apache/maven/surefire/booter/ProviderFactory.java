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

import org.apache.maven.surefire.providerapi.SurefireProvider;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Creates the surefire provider.
 * <p/>
 * Todo: This class does a little bit too little ;)
 *
 * @author Kristian Rosenvold
 */
public class ProviderFactory
{
    private final BooterConfiguration booterConfiguration;

    private final ClassLoader surefireClassLoader;

    private final SurefireReflector surefireReflector;

    public ProviderFactory( BooterConfiguration booterConfiguration, ClassLoader surefireClassLoader )
    {
        this.booterConfiguration = booterConfiguration;
        this.surefireClassLoader = surefireClassLoader;
        this.surefireReflector = new SurefireReflector( surefireClassLoader );
    }

    public SurefireProvider createProvider( ClassLoader testClassLoader )
    {
        ClassLoader context = java.lang.Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader( surefireClassLoader );

        ProviderConfiguration starterConfiguration = booterConfiguration.getSurefireStarterConfiguration();
        final Object o = surefireReflector.newInstance( starterConfiguration.getProviderClassName() );
        surefireReflector.setIfDirScannerAware( o, booterConfiguration.getDirScannerParams() );
        surefireReflector.setTestSuiteDefinitionAware( o, booterConfiguration.getTestSuiteDefinition() );
        surefireReflector.setProviderPropertiesAware( o, booterConfiguration.getProviderProperties() );
        surefireReflector.setReporterConfigurationAware( o, booterConfiguration.getReporterConfiguration() );
        surefireReflector.setTestClassLoaderAware( o, testClassLoader );
        surefireReflector.setTestArtifactInfoAware( o, booterConfiguration.getTestNg() );

        Thread.currentThread().setContextClassLoader( context );

        return createClassLoaderProxy( o );
    }

    private SurefireProvider createClassLoaderProxy( Object target )
    {
        return (SurefireProvider) Proxy.newProxyInstance( this.getClass().getClassLoader(),
                                                          new Class[]{ SurefireProvider.class },
                                                          new ClassLoaderProxy( target ) );
    }

    private class ClassLoaderProxy
        implements InvocationHandler
    {
        private final Object target;

        public ClassLoaderProxy( Object delegate )
        {
            this.target = delegate;
        }

        public Object invoke( Object proxy, Method method, Object[] args )
            throws Throwable
        {
            Method delegateMethod = target.getClass().getMethod( method.getName(), method.getParameterTypes() );
            final Object result = delegateMethod.invoke( target, args );
            return surefireReflector.convertIfRunResult(result);
        }
    }

}
