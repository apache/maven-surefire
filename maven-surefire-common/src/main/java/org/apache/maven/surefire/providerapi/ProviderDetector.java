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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.maven.surefire.api.provider.SurefireProvider;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

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
        Set<String> manuallyConfiguredProviders = getManuallyConfiguredProviders();

        List<ProviderInfo> providersToRun = manuallyConfiguredProviders.stream()
            .map( name ->
                findByName( name, wellKnownProviders )
                    .orElseGet( () -> dynamicProvider.instantiate( name ) ) )
            .collect( toList() );

        providersToRun.forEach( p -> logger.info( "Using configured provider " + p.getProviderName() ) );

        if ( providersToRun.isEmpty() )
        {
            return autoDetectOneWellKnownProvider( wellKnownProviders )
                .map( Collections::singletonList )
                .orElse( emptyList() );
        }
        else
        {
            return Collections.unmodifiableList( providersToRun );
        }
    }

    private Optional<ProviderInfo> autoDetectOneWellKnownProvider( ProviderInfo... wellKnownProviders )
    {
        Optional<ProviderInfo> providerInfo = stream( wellKnownProviders )
            .filter( ProviderInfo::isApplicable )
            .findFirst();

        providerInfo.ifPresent( p -> logger.info( "Using auto detected provider " + p.getProviderName() ) );

        return providerInfo;
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

    @Nonnull
    private Optional<ProviderInfo> findByName( String providerClassName, ProviderInfo... wellKnownProviders )
    {
        return stream( wellKnownProviders )
            .filter( p -> p.getProviderName().equals( providerClassName ) )
            .findFirst();
    }
}
