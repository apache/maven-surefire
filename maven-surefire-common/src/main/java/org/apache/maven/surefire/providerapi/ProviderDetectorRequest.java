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

import java.util.Locale;

/**
 * Request data for provider detector.
 *
 * @author Slawomir Jaranowski
 */
public class ProviderDetectorRequest
{
    private ConfigurableProviderInfo dynamicProvider;
    private ProviderInfo defaultProvider;
    private ProviderInfo[] wellKnownProviders;
    private String multipleFrameworks;

    public ConfigurableProviderInfo getDynamicProvider()
    {
        return dynamicProvider;
    }

    public void setDynamicProvider( ConfigurableProviderInfo dynamicProvider )
    {
        this.dynamicProvider = dynamicProvider;
    }

    public ProviderInfo getDefaultProvider()
    {
        return defaultProvider;
    }

    public void setDefaultProvider( ProviderInfo defaultProvider )
    {
        this.defaultProvider = defaultProvider;
    }

    public ProviderInfo[] getWellKnownProviders()
    {
        return wellKnownProviders;
    }

    public void setWellKnownProviders( ProviderInfo... wellKnownProviders )
    {
        this.wellKnownProviders = wellKnownProviders;
    }

    public boolean isWarnOnMultipleFrameworks()
    {
        return "warn".equals( multipleFrameworks );
    }

    public boolean isFailOnMultipleFrameworks()
    {
        return "fail".equals( multipleFrameworks );
    }

    public void setMultipleFrameworks( String multipleFrameworks )
    {
        if ( multipleFrameworks != null )
        {
            this.multipleFrameworks = multipleFrameworks.trim().toLowerCase( Locale.ROOT );
            if ( !( "warn".equals( this.multipleFrameworks )
                || "fail".equals( this.multipleFrameworks ) ) )
            {
                throw new IllegalArgumentException( "multipleFrameworks value:"
                    + " \"" + multipleFrameworks + "\""
                    + " must be one of \"warn\" or \"fail\"" );
            }
        }
    }
}
