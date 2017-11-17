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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Character.isJavaIdentifierStart;
import static java.util.Collections.emptySet;
import static org.apache.maven.surefire.util.ReflectionUtils.getConstructor;

/**
 * SPI loader for Surefire/Failsafe should use {@link Thread#getContextClassLoader() current ClassLoader}.
 * <br>
 * The {@link java.util.ServiceLoader} embedded in JVM uses
 * {@link ClassLoader#getSystemClassLoader() System ClassLoader} and cannot be used in Surefire/Failsafe.
 *
 * @since 2.20
 */
public final class ServiceLoader
{

    @Nonnull
    @SuppressWarnings( "unchecked" )
    public <T> Set<T> load( Class<T> clazz, ClassLoader classLoader )
    {
        try
        {
            Set<T> implementations = new HashSet<T>();
            for ( String fullyQualifiedClassName : lookup( clazz, classLoader ) )
            {
                Class<?> implClass = classLoader.loadClass( fullyQualifiedClassName );
                implementations.add( (T) getConstructor( implClass ).newInstance() );
            }
            return implementations;
        }
        catch ( IOException e )
        {
            throw new IllegalStateException( e.getLocalizedMessage(), e );
        }
        catch ( ClassNotFoundException e )
        {
            throw new IllegalStateException( e.getLocalizedMessage(), e );
        }
        catch ( InvocationTargetException e )
        {
            throw new IllegalStateException( e.getLocalizedMessage(), e );
        }
        catch ( InstantiationException e )
        {
            throw new IllegalStateException( e.getLocalizedMessage(), e );
        }
        catch ( IllegalAccessException e )
        {
            throw new IllegalStateException( e.getLocalizedMessage(), e );
        }
    }

    @Nonnull
    public Set<String> lookup( Class<?> clazz, ClassLoader classLoader )
            throws IOException
    {
        final String resourceName = "META-INF/services/" + clazz.getName();

        if ( classLoader == null )
        {
            return emptySet();
        }
        final Enumeration<URL> urls = classLoader.getResources( resourceName );
        return lookupSpiImplementations( urls );
    }

    /**
     * Method loadServices loads the services of a class that are
     * defined using the SPI mechanism.
     *
     * @param urlEnumeration The urls from the resource
     * @return The set of service provider names
     * @throws IOException When reading the streams fails
     */
    @Nonnull
    @SuppressWarnings( "checkstyle:innerassignment" )
    private static Set<String> lookupSpiImplementations( final Enumeration<URL> urlEnumeration )
            throws IOException
    {
        final Set<String> names = new HashSet<String>();
        nextUrl:
        while ( urlEnumeration.hasMoreElements() )
        {
            final URL url = urlEnumeration.nextElement();
            final BufferedReader reader = getReader( url );
            try
            {
                for ( String line; ( line = reader.readLine() ) != null; )
                {
                    int ci = line.indexOf( '#' );
                    if ( ci >= 0 )
                    {
                        line = line.substring( 0, ci );
                    }
                    line = line.trim();
                    int n = line.length();
                    if ( n == 0 )
                    {
                        continue; // next line
                    }

                    if ( line.indexOf( ' ' ) >= 0 || line.indexOf( '\t' ) >= 0 )
                    {
                        continue nextUrl; // next url
                    }
                    char cp = line.charAt( 0 ); // should use codePointAt but this was JDK1.3
                    if ( !isJavaIdentifierStart( cp ) )
                    {
                        continue nextUrl; // next url
                    }
                    for ( int i = 1; i < n; i++ )
                    {
                        cp = line.charAt( i );  // should use codePointAt but this was JDK1.3
                        if ( !isJavaIdentifierPart( cp ) && cp != '.' )
                        {
                            continue nextUrl; // next url
                        }
                    }
                    if ( !names.contains( line ) )
                    {
                        names.add( line );
                    }
                }
            }
            finally
            {
                reader.close();
            }
        }

        return names;
    }

    @Nonnull
    private static BufferedReader getReader( @Nonnull URL url )
            throws IOException
    {
        final InputStream inputStream = url.openStream();
        final InputStreamReader inputStreamReader = new InputStreamReader( inputStream );
        return new BufferedReader( inputStreamReader );
    }
}
