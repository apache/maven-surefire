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

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.surefire.booterclient.ProviderDetector;
import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.apache.maven.surefire.util.NestedRuntimeException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Kristian Rosenvold
 */
public class ProviderList
{
    private final ProviderInfo[] wellKnownProviders;

    private final ConfigurableProviderInfo dynamicProvider;

    public ProviderList( ProviderInfo[] wellKnownProviders, ConfigurableProviderInfo dynamicProviderInfo )
    {
        this.wellKnownProviders = wellKnownProviders;
        this.dynamicProvider = dynamicProviderInfo;
    }


    public List resolve( Log log )
    {
        List providersToRun = new ArrayList();

        Set manuallyConfiguredProviders = getManuallyConfiguredProviders();
        if ( manuallyConfiguredProviders.size() > 0 )
        {
            Iterator iter = manuallyConfiguredProviders.iterator();
            String name;
            while ( iter.hasNext() )
            {
                name = (String) iter.next();
                ProviderInfo wellKnown = findByName( name );
                ProviderInfo providerToAdd = wellKnown != null ? wellKnown : dynamicProvider.instantiate( name );
                log.info( "Using configured provider " + providerToAdd.getProviderName() );
                providersToRun.add( providerToAdd );
            }
            return providersToRun;
        }

        return autoDetectOneProvider();
    }

    private List autoDetectOneProvider()
    {
        List providersToRun = new ArrayList();
        for ( int i = 0; i < wellKnownProviders.length; i++ )
        {
            if ( wellKnownProviders[i].isApplicable() )
            {
                providersToRun.add( wellKnownProviders[i] );
                return providersToRun;
            }
        }
        return providersToRun;
    }

    private Set getManuallyConfiguredProviders()
    {
        try
        {
            return ProviderDetector.getServiceNames( SurefireProvider.class,
                                                     Thread.currentThread().getContextClassLoader() );
        }

        catch ( IOException e )
        {
            throw new NestedRuntimeException( e );
        }


    }

    private ProviderInfo findByName( String providerClassName )
    {
        for ( int i = 0; i < wellKnownProviders.length; i++ )
        {
            ProviderInfo wellKnownProvider = wellKnownProviders[i];
            if ( wellKnownProvider.getProviderName().equals( providerClassName ) )
            {
                return wellKnownProvider;
            }
        }
        return null;
    }
}
