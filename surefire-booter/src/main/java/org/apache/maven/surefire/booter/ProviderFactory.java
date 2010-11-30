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

import org.apache.maven.surefire.providerapi.DirectoryScannerParametersAware;

/**
 * @author Kristian Rosenvold
 */
public class ProviderFactory
{
    private final BooterConfiguration booterConfiguration;

    private final ClassLoader surefireClassLoader;

    private final Class directoryScannerParametersAware;

    private final SurefireReflector surefireReflector;


    public ProviderFactory( BooterConfiguration booterConfiguration, ClassLoader surefireClassLoader )
    {
        this.booterConfiguration = booterConfiguration;
        this.surefireClassLoader = surefireClassLoader;
        this.surefireReflector = new SurefireReflector( surefireClassLoader );
        try
        {
            directoryScannerParametersAware =
                surefireClassLoader.loadClass( DirectoryScannerParametersAware.class.getName() );
        }
        catch ( ClassNotFoundException e )
        {
            throw new RuntimeException( "When loading class", e );
        }
    }

    public Object createProvider( ClassLoader testClassLoader )
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

        /*
        if ( o instanceof ReportingAware )
        {
            ReporterManagerFactory reporterManagerFactory = new ReporterManagerFactory2( booterConfiguration.getReports(), surefireClassLoader, booterConfiguration.getReporterConfiguration() );
            ((ReportingAware) o).setReporterManagerFactory( reporterManagerFactory );
        }

*/
        Thread.currentThread().setContextClassLoader( context );

        return o;
    }
}
