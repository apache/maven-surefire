package org.apache.maven.surefire.providerapi;

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

import javax.annotation.Nonnull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.surefire.api.provider.SurefireProvider;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import static java.lang.Thread.currentThread;

/**
 * @author Kristian Rosenvold
 */
@Component( role = ProviderDetector.class )
public final class ProviderDetector
{
    @Requirement
    private Logger logger;

    @Requirement
    private ServiceLoader serviceLoader;

    @Nonnull
    public List<ProviderInfo> resolve( ProviderDetectorRequest request )
    {
        List<ProviderInfo> providersToRun = new ArrayList<>();
        Set<String> manuallyConfiguredProviders = getManuallyConfiguredProviders();
        for ( String name : manuallyConfiguredProviders )
        {
            ProviderInfo wellKnown = findByName( name, request.getWellKnownProviders() );
            ProviderInfo providerToAdd =
                wellKnown != null ? wellKnown : request.getDynamicProvider().instantiate( name );
            logger.info( "Using configured provider " + providerToAdd.getProviderName() );
            providersToRun.add( providerToAdd );
        }

        return providersToRun.isEmpty() ? autoDetectOneWellKnownProvider( request ) : providersToRun;
    }

    @Nonnull
    private List<ProviderInfo> autoDetectOneWellKnownProvider( ProviderDetectorRequest request )
    {
        List<ProviderInfo> applicableProviders = new ArrayList<>();
        for ( ProviderInfo wellKnownProvider : request.getWellKnownProviders() )
        {
            if ( wellKnownProvider.isApplicable() )
            {
                applicableProviders.add( wellKnownProvider );
            }
        }

        if ( applicableProviders.isEmpty() )
        {
            return Collections.singletonList(  request.getDefaultProvider() );
        }

        ProviderInfo providerInfoToReturn = applicableProviders.get( 0 );
        if ( applicableProviders.size() > 1 && !isJunit4SpecialCase( applicableProviders ) )
        {
            if ( request.isFailOnMultipleFrameworks() )
            {
                logger.error( "There are many providers automatically detected:" );
                for ( ProviderInfo providerInfo : applicableProviders )
                {
                    logger.error( "   " + providerInfo.getProviderName() );
                }
                logger.error( "" );
                logger.error( "Please select providers manually or check project dependency tree." );
                logger.error( "" );
                providerInfoToReturn = null;
            }
            else if ( request.isWarnOnMultipleFrameworks() )
            {
                logger.warn( "There are many providers automatically detected:" );
                for ( ProviderInfo providerInfo : applicableProviders )
                {
                    logger.warn( "   " + providerInfo.getProviderName() );
                }
                logger.warn( "" );
                logger.warn( "Only first will be used." );
                logger.warn( "" );
            }
        }

        if ( providerInfoToReturn != null )
        {
            logger.info( "Using auto detected provider " + providerInfoToReturn.getProviderName() );
            return Collections.singletonList( providerInfoToReturn );
        }

        return Collections.<ProviderInfo>emptyList();
    }

    /**
     * For JUnit, we have two providers and both can be applicable for junit &gt;= 4.7.
     * In this special case we don't generate warning or break executions.
     * <p>
     * Should be removed after SUREFIRE-1494.
     */
    private boolean isJunit4SpecialCase( List<ProviderInfo> applicableProviders )
    {

        if ( applicableProviders.size() == 2 )
        {
            String providerName1 = applicableProviders.get( 0 ).getProviderName();
            String providerName2 = applicableProviders.get( 1 ).getProviderName();

            boolean p1 = providerName1.endsWith( "JUnitCoreProvider" ) || providerName1.endsWith( "JUnit4Provider" );
            boolean p2 = providerName2.endsWith( "JUnitCoreProvider" ) || providerName2.endsWith( "JUnit4Provider" );
            return p1 && p2;
        }

        return false;
    }

    private Set<String> getManuallyConfiguredProviders()
    {
        try
        {
            ClassLoader cl = currentThread().getContextClassLoader();
            return serviceLoader.lookup( SurefireProvider.class, cl );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    private ProviderInfo findByName( String providerClassName, ProviderInfo... wellKnownProviders )
    {
        for ( ProviderInfo wellKnownProvider : wellKnownProviders )
        {
            if ( wellKnownProvider.getProviderName().equals( providerClassName ) )
            {
                return wellKnownProvider;
            }
        }
        return null;
    }
}
