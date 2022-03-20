package org.apache.maven.surefire.junitplatform;

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

import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.logging.Level.WARNING;
import static java.util.stream.Collectors.toList;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.EXCLUDE_JUNIT5_ENGINES_PROP;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.INCLUDE_JUNIT5_ENGINES_PROP;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.TESTNG_EXCLUDEDGROUPS_PROP;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.TESTNG_GROUPS_PROP;
import static org.apache.maven.surefire.api.report.ConsoleOutputCapture.startCapture;
import static org.apache.maven.surefire.api.report.RunMode.NORMAL_RUN;
import static org.apache.maven.surefire.api.report.RunMode.RERUN_TEST_AFTER_FAILURE;
import static org.apache.maven.surefire.api.testset.TestListResolver.optionallyWildcardFilter;
import static org.apache.maven.surefire.api.util.TestsToRun.fromClass;
import static org.apache.maven.surefire.shared.utils.StringUtils.isBlank;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.maven.surefire.api.provider.AbstractProvider;
import org.apache.maven.surefire.api.provider.ProviderParameters;
import org.apache.maven.surefire.api.report.ReporterException;
import org.apache.maven.surefire.api.report.ReporterFactory;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.apache.maven.surefire.api.util.ScanResult;
import org.apache.maven.surefire.api.util.SurefireReflectionException;
import org.apache.maven.surefire.api.util.TestsToRun;
import org.apache.maven.surefire.shared.utils.StringUtils;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.Filter;
import org.junit.platform.launcher.EngineFilter;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;

/**
 * JUnit 5 Platform Provider.
 *
 * @since 2.22.0
 */
public class JUnitPlatformProvider
    extends AbstractProvider
{
    static final String CONFIGURATION_PARAMETERS = "configurationParameters";

    private final ProviderParameters parameters;

    private final Launcher launcher;

    private final Filter<?>[] filters;

    private final Map<String, String> configurationParameters;

    public JUnitPlatformProvider( ProviderParameters parameters )
    {
        this( parameters, new LazyLauncher() );
    }

    JUnitPlatformProvider( ProviderParameters parameters, Launcher launcher )
    {
        this.parameters = parameters;
        this.launcher = launcher;
        filters = newFilters();
        configurationParameters = newConfigurationParameters();
    }

    @Override
    public Iterable<Class<?>> getSuites()
    {
        try
        { 
            return scanClasspath();
        }
        finally
        {
            closeLauncher();
        }
    }

    @Override
    public RunResult invoke( Object forkTestSet )
                    throws TestSetFailedException, ReporterException
    {
        ReporterFactory reporterFactory = parameters.getReporterFactory();
        final RunResult runResult;
        try
        {
            RunListenerAdapter adapter = new RunListenerAdapter( reporterFactory.createTestReportListener() );
            adapter.setRunMode( NORMAL_RUN );
            startCapture( adapter );
            setupJunitLogger();
            if ( forkTestSet instanceof TestsToRun )
            {
                invokeAllTests( (TestsToRun) forkTestSet, adapter );
            }
            else if ( forkTestSet instanceof Class )
            {
                invokeAllTests( fromClass( ( Class<?> ) forkTestSet ), adapter );
            }
            else if ( forkTestSet == null )
            {
                invokeAllTests( scanClasspath(), adapter );
            }
            else
            {
                throw new IllegalArgumentException(
                        "Unexpected value of forkTestSet: " + forkTestSet );
            }
        }
        finally
        {
            runResult = reporterFactory.close();
        }
        return runResult;
    }

    private static void setupJunitLogger()
    {
        Logger logger = Logger.getLogger( "org.junit" );
        if ( logger.getLevel() == null )
        {
            logger.setLevel( WARNING );
        }
    }

    private TestsToRun scanClasspath()
    {
        TestPlanScannerFilter filter = new TestPlanScannerFilter( launcher, filters );
        ScanResult scanResult = parameters.getScanResult();
        TestsToRun scannedClasses = scanResult.applyFilter( filter, parameters.getTestClassLoader() );
        return parameters.getRunOrderCalculator().orderTestClasses( scannedClasses );
    }

    private void invokeAllTests( TestsToRun testsToRun, RunListenerAdapter adapter )
    {
        try
        {
            execute( testsToRun, adapter );
        }
        finally
        {
            closeLauncher();
        }
        // Rerun failing tests if requested
        int count = parameters.getTestRequest().getRerunFailingTestsCount();
        if ( count > 0 && adapter.hasFailingTests() )
        {
            adapter.setRunMode( RERUN_TEST_AFTER_FAILURE );
            for ( int i = 0; i < count; i++ )
            {
                try
                {
                    // Replace the "discoveryRequest" so that it only specifies the failing tests
                    LauncherDiscoveryRequest discoveryRequest =
                            buildLauncherDiscoveryRequestForRerunFailures( adapter );
                    // Reset adapter's recorded failures and invoke the failed tests again
                    adapter.reset();
                    launcher.execute( discoveryRequest, adapter );
                    // If no tests fail in the rerun, we're done
                    if ( !adapter.hasFailingTests() )
                    {
                        break;
                    }
                }
                finally
                {
                    closeLauncher();
                }
            }
        }
    }

    private void execute( TestsToRun testsToRun, RunListenerAdapter adapter )
    {
        if ( testsToRun.allowEagerReading() )
        {
            List<DiscoverySelector> selectors = new ArrayList<>();
            testsToRun.iterator()
                .forEachRemaining( c -> selectors.add( selectClass( c.getName() )  ) );

            LauncherDiscoveryRequestBuilder builder = request()
                .filters( filters )
                .configurationParameters( configurationParameters )
                .selectors( selectors );

            launcher.execute( builder.build(), adapter );
        }
        else
        {
            testsToRun.iterator()
                .forEachRemaining( c ->
                {
                    LauncherDiscoveryRequestBuilder builder = request()
                        .filters( filters )
                        .configurationParameters( configurationParameters )
                        .selectors( selectClass( c.getName() ) );
                    launcher.execute( builder.build(), adapter );
                } );
        }
    }
    
    private void closeLauncher()
    {
        if ( launcher instanceof AutoCloseable )
        {
            try
            {
                ( (AutoCloseable) launcher ).close();
            }
            catch ( Exception e )
            {
                throw new SurefireReflectionException( e );
            }
        }
    }

    private LauncherDiscoveryRequest buildLauncherDiscoveryRequestForRerunFailures( RunListenerAdapter adapter )
    {
        LauncherDiscoveryRequestBuilder builder = request().filters( filters ).configurationParameters(
                configurationParameters );
        // Iterate over recorded failures
        for ( TestIdentifier identifier : new LinkedHashSet<>( adapter.getFailures().keySet() ) )
        {
            builder.selectors( selectUniqueId( identifier.getUniqueId() ) );
        }
        return builder.build();
    }

    private Filter<?>[] newFilters()
    {
        List<Filter<?>> filters = new ArrayList<>();

        getPropertiesList( TESTNG_GROUPS_PROP )
                .map( TagFilter::includeTags )
                .ifPresent( filters::add );

        getPropertiesList( TESTNG_EXCLUDEDGROUPS_PROP )
                .map( TagFilter::excludeTags )
                .ifPresent( filters::add );

        of( optionallyWildcardFilter( parameters.getTestRequest().getTestListResolver() ) )
            .filter( f -> !f.isEmpty() )
            .filter( f -> !f.isWildcard() )
            .map( TestMethodFilter::new )
            .ifPresent( filters::add );

        getPropertiesList( INCLUDE_JUNIT5_ENGINES_PROP )
            .map( EngineFilter::includeEngines )
            .ifPresent( filters::add );

        getPropertiesList( EXCLUDE_JUNIT5_ENGINES_PROP )
            .map( EngineFilter::excludeEngines )
            .ifPresent( filters::add );

        return filters.toArray( new Filter<?>[ filters.size() ] );
    }

    Filter<?>[] getFilters()
    {
        return filters;
    }

    private Map<String, String> newConfigurationParameters()
    {
        String content = parameters.getProviderProperties().get( CONFIGURATION_PARAMETERS );
        if ( content == null )
        {
            return emptyMap();
        }
        try ( StringReader reader = new StringReader( content ) )
        {
            Map<String, String> result = new HashMap<>();
            Properties props = new Properties();
            props.load( reader );
            props.stringPropertyNames()
                    .forEach( key -> result.put( key, props.getProperty( key ) ) );
            return result;
        }
        catch ( IOException e )
        {
            throw new UncheckedIOException( "Error reading " + CONFIGURATION_PARAMETERS, e );
        }
    }

    Map<String, String> getConfigurationParameters()
    {
        return configurationParameters;
    }

    private Optional<List<String>> getPropertiesList( String key )
    {
        String property = parameters.getProviderProperties().get( key );
        return isBlank( property ) ? empty()
                        : of( stream( property.split( "[,]+" ) )
                                              .filter( StringUtils::isNotBlank )
                                              .map( String::trim )
                                              .collect( toList() ) );
    }
}
