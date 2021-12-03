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
    public List<ProviderInfo> resolve( ConfigurableProviderInfo dynamicProvider, ProviderInfo... wellKnownProviders )
    {
        List<ProviderInfo> providersToRun = new ArrayList<>();
        Set<String> manuallyConfiguredProviders = getManuallyConfiguredProviders();
        for ( String name : manuallyConfiguredProviders )
        {
            ProviderInfo wellKnown = findByName( name, wellKnownProviders );
            ProviderInfo providerToAdd = wellKnown != null ? wellKnown : dynamicProvider.instantiate( name );
            logger.info( "Using configured provider " + providerToAdd.getProviderName() );
            providersToRun.add( providerToAdd );
        }
        return manuallyConfiguredProviders.isEmpty() ? autoDetectOneWellKnownProvider( wellKnownProviders )
            : providersToRun;
    }

    @Nonnull
    private List<ProviderInfo> autoDetectOneWellKnownProvider( ProviderInfo... wellKnownProviders )
    {
        List<ProviderInfo> providersToRun = new ArrayList<>();
        for ( ProviderInfo wellKnownProvider : wellKnownProviders )
        {
            if ( wellKnownProvider.isApplicable() )
            {
                logger.info( "Using auto detected provider " + wellKnownProvider.getProviderName() );
                providersToRun.add( wellKnownProvider );
                return providersToRun;
            }
        }
        return providersToRun;
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
