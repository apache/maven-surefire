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

import org.apache.maven.plugin.surefire.extensions.SurefireConsoleOutputReporter;
import org.apache.maven.plugin.surefire.extensions.SurefireStatelessReporter;
import org.apache.maven.plugin.surefire.extensions.SurefireStatelessTestsetInfoReporter;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.log.api.ConsoleLoggerDecorator;
import org.apache.maven.plugin.surefire.report.DefaultReporterFactory;
import org.apache.maven.surefire.api.util.SurefireReflectionException;

import javax.annotation.Nonnull;
import java.io.File;
import java.lang.reflect.Constructor;

import static org.apache.maven.surefire.api.util.ReflectionUtils.getConstructor;
import static org.apache.maven.surefire.api.util.ReflectionUtils.instantiateObject;
import static org.apache.maven.surefire.api.util.ReflectionUtils.newInstance;

/**
 * @author Kristian Rosenvold
 */
public class CommonReflector
{
    private final Class<?> startupReportConfiguration;
    private final Class<?> consoleLogger;
    private final Class<?> statelessTestsetReporter;
    private final Class<?> consoleOutputReporter;
    private final Class<?> statelessTestsetInfoReporter;
    private final ClassLoader surefireClassLoader;

    public CommonReflector( @Nonnull ClassLoader surefireClassLoader )
    {
        this.surefireClassLoader = surefireClassLoader;

        try
        {
            startupReportConfiguration = surefireClassLoader.loadClass( StartupReportConfiguration.class.getName() );
            consoleLogger = surefireClassLoader.loadClass( ConsoleLogger.class.getName() );
            statelessTestsetReporter = surefireClassLoader.loadClass( SurefireStatelessReporter.class.getName() );
            consoleOutputReporter = surefireClassLoader.loadClass( SurefireConsoleOutputReporter.class.getName() );
            statelessTestsetInfoReporter =
                    surefireClassLoader.loadClass( SurefireStatelessTestsetInfoReporter.class.getName() );
        }
        catch ( ClassNotFoundException e )
        {
            throw new SurefireReflectionException( e );
        }
    }

    public Object createReportingReporterFactory( @Nonnull StartupReportConfiguration startupReportConfiguration,
                                                  @Nonnull ConsoleLogger consoleLogger )
    {
        Class<?>[] args = { this.startupReportConfiguration, this.consoleLogger };
        Object src = createStartupReportConfiguration( startupReportConfiguration );
        Object logger = createConsoleLogger( consoleLogger, surefireClassLoader );
        Object[] params = { src, logger };
        return instantiateObject( DefaultReporterFactory.class.getName(), args, params, surefireClassLoader );
    }

    private Object createStartupReportConfiguration( @Nonnull StartupReportConfiguration reporterConfiguration )
    {
        Constructor<?> constructor = getConstructor( startupReportConfiguration, boolean.class, boolean.class,
                                                     String.class, boolean.class, File.class,
                                                     boolean.class, String.class, File.class, boolean.class,
                                                     int.class, String.class, String.class, boolean.class,
                                                     statelessTestsetReporter, consoleOutputReporter,
                                                     statelessTestsetInfoReporter );
        Object[] params = { reporterConfiguration.isUseFile(), reporterConfiguration.isPrintSummary(),
            reporterConfiguration.getReportFormat(), reporterConfiguration.isRedirectTestOutputToFile(),
            reporterConfiguration.getReportsDirectory(),
            reporterConfiguration.isTrimStackTrace(), reporterConfiguration.getReportNameSuffix(),
            reporterConfiguration.getStatisticsFile(), reporterConfiguration.isRequiresRunHistory(),
            reporterConfiguration.getRerunFailingTestsCount(), reporterConfiguration.getXsdSchemaLocation(),
            reporterConfiguration.getEncoding().name(), reporterConfiguration.isForkMode(),
            reporterConfiguration.getXmlReporter().clone( surefireClassLoader ),
            reporterConfiguration.getConsoleOutputReporter().clone( surefireClassLoader ),
            reporterConfiguration.getTestsetReporter().clone( surefireClassLoader )
        };
        return newInstance( constructor, params );
    }

    static Object createConsoleLogger( ConsoleLogger consoleLogger, ClassLoader cl )
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
