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

/**
 * Configuration that is used by the SurefireStarter but does not make it into the provider itself.
 *
 * @author Kristian Rosenvold
 */
public class StartupConfiguration
{
    private final String providerClassName;

    private final ClasspathConfiguration classpathConfiguration;

    private final ClassLoaderConfiguration classLoaderConfiguration;

    private final boolean isForkRequested;

    private final boolean isInForkedVm;

    private final boolean redirectTestOutputToFile;


    public StartupConfiguration( String providerClassName, ClasspathConfiguration classpathConfiguration,
                                 ClassLoaderConfiguration classLoaderConfiguration, boolean forkRequested,
                                 boolean inForkedVm, boolean redirectTestOutputToFile )
    {
        this.providerClassName = providerClassName;
        this.classpathConfiguration = classpathConfiguration;
        this.classLoaderConfiguration = classLoaderConfiguration;
        isForkRequested = forkRequested;
        isInForkedVm = inForkedVm;
        this.redirectTestOutputToFile = redirectTestOutputToFile;
    }

    public static StartupConfiguration inForkedVm(String providerClassName, ClasspathConfiguration classpathConfiguration,
                                  ClassLoaderConfiguration classLoaderConfiguration){
        return new StartupConfiguration( providerClassName, classpathConfiguration, classLoaderConfiguration, false, true, false );
    }

    public ClasspathConfiguration getClasspathConfiguration()
    {
        return classpathConfiguration;
    }

    public boolean useSystemClassLoader()
    {
        // todo; I am not totally convinced this logic is as simple as it could be
        return classLoaderConfiguration.isUseSystemClassLoader() && ( isInForkedVm || isForkRequested );
    }

    public boolean isManifestOnlyJarRequestedAndUsable()
    {
        return classLoaderConfiguration.isManifestOnlyJarRequestedAndUsable();
    }

    public boolean isRedirectTestOutputToFile()
    {
        return redirectTestOutputToFile;
    }

    public String getProviderClassName()
    {
        return providerClassName;
    }

}
