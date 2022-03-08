package org.apache.maven.plugin.surefire.booterclient.lazytestprovider;

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

import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.maven.surefire.shared.utils.cli.CommandLineUtils;

import static java.util.Collections.addAll;

/**
 * A {@link org.apache.maven.surefire.shared.utils.cli.Commandline} implementation.
 *
 * @author Andreas Gudian
 */
public class Commandline
    extends org.apache.maven.surefire.shared.utils.cli.Commandline
{
    private final Collection<String> excludedEnvironmentVariables;
    private final Set<String> addedEnvironmentVariables;

    /**
     * for testing purposes only
     */
    public Commandline()
    {
        this( new String[0] );
    }

    public Commandline( String[] excludedEnvironmentVariables )
    {
        this.excludedEnvironmentVariables = new ConcurrentLinkedDeque<>();
        addedEnvironmentVariables = new HashSet<>();
        addAll( this.excludedEnvironmentVariables, excludedEnvironmentVariables );
    }

    @Override
    public void addEnvironment( String name, String value )
    {
        super.addEnvironment( name, value );
        addedEnvironmentVariables.add( name );
    }

    @Override
    public final void addSystemEnvironment()
    {
        Properties systemEnvVars = CommandLineUtils.getSystemEnvVars();

        for ( String key : systemEnvVars.stringPropertyNames() )
        {
            if ( !addedEnvironmentVariables.contains( key ) && !excludedEnvironmentVariables.contains( key ) )
            {
                addEnvironment( key, systemEnvVars.getProperty( key ) );
            }
        }
    }
}
