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

import javax.annotation.Nonnull;

/**
 * Configuration that is used by the SurefireStarter but does not make it into the provider itself.
 *
 * @author Kristian Rosenvold
 */
public class StartupConfiguration
{
    private static final String SUREFIRE_TEST_CLASSPATH = "surefire.test.class.path";

    private final String providerClassName;
    private final AbstractPathConfiguration classpathConfiguration;
    private final ClassLoaderConfiguration classLoaderConfiguration;
    private final boolean isForkRequested;
    private final boolean isInForkedVm;

    public StartupConfiguration( @Nonnull String providerClassName,
                                 @Nonnull AbstractPathConfiguration classpathConfiguration,
                                 @Nonnull ClassLoaderConfiguration classLoaderConfiguration, boolean isForkRequested,
                                 boolean inForkedVm )
    {
        this.classpathConfiguration = classpathConfiguration;
        this.classLoaderConfiguration = classLoaderConfiguration;
        this.isForkRequested = isForkRequested;
        this.providerClassName = providerClassName;
        isInForkedVm = inForkedVm;
    }

    public boolean isProviderMainClass()
    {
        return providerClassName.endsWith( "#main" );
    }

    public static StartupConfiguration inForkedVm( String providerClassName,
                                                   ClasspathConfiguration classpathConfiguration,
                                                   ClassLoaderConfiguration classLoaderConfiguration )
    {
        return new StartupConfiguration( providerClassName, classpathConfiguration, classLoaderConfiguration, true,
                                         true );
    }

    public AbstractPathConfiguration getClasspathConfiguration()
    {
        return classpathConfiguration;
    }

    @Deprecated
    public boolean useSystemClassLoader()
    {
        // todo; I am not totally convinced this logic is as simple as it could be
        return classLoaderConfiguration.isUseSystemClassLoader() && ( isInForkedVm || isForkRequested );
    }

    public boolean isManifestOnlyJarRequestedAndUsable()
    {
        return classLoaderConfiguration.isManifestOnlyJarRequestedAndUsable();
    }

    public String getProviderClassName()
    {
        return providerClassName;
    }

    public String getActualClassName()
    {
        return isProviderMainClass() ? stripEnd( providerClassName, "#main" ) : providerClassName;
    }

    /**
     * <p>Strip any of a supplied String from the end of a String.</p>
     * <br>
     * <p>If the strip String is {@code null}, whitespace is
     * stripped.</p>
     *
     * @param str   the String to remove characters from
     * @param strip the String to remove
     * @return the stripped String
     */
    private static String stripEnd( String str, String strip )
    {
        if ( str == null )
        {
            return null;
        }
        int end = str.length();

        if ( strip == null )
        {
            while ( ( end != 0 ) && Character.isWhitespace( str.charAt( end - 1 ) ) )
            {
                end--;
            }
        }
        else
        {
            while ( end != 0 && strip.indexOf( str.charAt( end - 1 ) ) != -1 )
            {
                end--;
            }
        }
        return str.substring( 0, end );
    }

    public ClassLoaderConfiguration getClassLoaderConfiguration()
    {
        return classLoaderConfiguration;
    }

    public boolean isShadefire()
    {
        return providerClassName.startsWith( "org.apache.maven.shadefire.surefire" );
    }

    public void writeSurefireTestClasspathProperty()
    {
        getClasspathConfiguration().getTestClasspath().writeToSystemProperty( SUREFIRE_TEST_CLASSPATH );
    }
}
