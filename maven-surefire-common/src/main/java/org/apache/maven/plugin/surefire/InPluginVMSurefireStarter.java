package org.apache.maven.plugin.surefire;

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

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import org.apache.maven.surefire.booter.ProviderConfiguration;
import org.apache.maven.surefire.booter.ProviderFactory;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.SurefireExecutionException;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.DefaultScanResult;

import javax.annotation.Nonnull;

/**
 * Starts the provider in the same VM as the surefire plugin.
 * <p/>
 * This part of the booter is always guaranteed to be in the
 * same vm as the tests will be run in.
 *
 * @author Jason van Zyl
 * @author Brett Porter
 * @author Emmanuel Venisse
 * @author Dan Fabulich
 * @author Kristian Rosenvold
 */
public class InPluginVMSurefireStarter
{

    private final StartupConfiguration startupConfiguration;

    private final StartupReportConfiguration startupReportConfiguration;

    private final ProviderConfiguration providerConfiguration;

    public InPluginVMSurefireStarter( @Nonnull StartupConfiguration startupConfiguration,
                                      @Nonnull ProviderConfiguration providerConfiguration,
                                      @Nonnull StartupReportConfiguration startupReportConfiguration )
    {
        this.startupConfiguration = startupConfiguration;
        this.startupReportConfiguration = startupReportConfiguration;
        this.providerConfiguration = providerConfiguration;
    }

    public RunResult runSuitesInProcess( @Nonnull DefaultScanResult scanResult )
        throws SurefireExecutionException, TestSetFailedException
    {
        // The test classloader must be constructed first to avoid issues with commons-logging until we properly
        // separate the TestNG classloader

        Properties providerProperties = providerConfiguration.getProviderProperties();
        scanResult.writeTo( providerProperties );

        startupConfiguration.writeSurefireTestClasspathProperty();
        ClassLoader testsClassLoader = startupConfiguration.getClasspathConfiguration().createMergedClassLoader();

        CommonReflector surefireReflector = new CommonReflector( testsClassLoader );

        final Object factory = surefireReflector.createReportingReporterFactory( startupReportConfiguration );

        try
        {
            return ProviderFactory.invokeProvider( null, testsClassLoader, factory,
                                                   providerConfiguration, false, startupConfiguration, true );
        }
        catch ( InvocationTargetException e )
        {
            throw new SurefireExecutionException( "Exception in provider", e.getTargetException() );
        }
    }

}
