package org.apache.maven.plugin.surefire.booterclient;

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
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Stephen Conolly
 * @author Kristian Rosenvold
 * @noinspection UnusedDeclaration
 */
public class ProviderDetector
{

    @Nonnull public static Set<String> getServiceNames( Class<?> clazz, ClassLoader classLoader )
        throws IOException
    {
        final String resourceName = "META-INF/services/" + clazz.getName();

        if ( classLoader == null )
        {
            return Collections.emptySet();
        }
        final Enumeration<URL> urlEnumeration = classLoader.getResources( resourceName );
        return getNames( urlEnumeration );
    }


    /**
     * Method loadServices loads the services of a class that are
     * defined using the SPI mechanism.
     *
     * @param urlEnumeration The urls from the resource
     * @return The set of service provider names
     * @throws IOException When reading the streams fails
     */
    @Nonnull private static Set<String> getNames( final Enumeration<URL> urlEnumeration )
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
                String line;
                while ( ( line = reader.readLine() ) != null )
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

                    if ( ( line.indexOf( ' ' ) >= 0 ) || ( line.indexOf( '\t' ) >= 0 ) )
                    {
                        continue nextUrl; // next url
                    }
                    char cp = line.charAt( 0 ); // should use codePointAt but this is JDK1.3
                    if ( !Character.isJavaIdentifierStart( cp ) )
                    {
                        continue nextUrl; // next url
                    }
                    for ( int i = 1; i < n; i++ )
                    {
                        cp = line.charAt( i );  // should use codePointAt but this is JDK1.3
                        if ( !Character.isJavaIdentifierPart( cp ) && ( cp != '.' ) )
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

    @Nonnull private static BufferedReader getReader( @Nonnull URL url )
        throws IOException
    {
        final InputStream inputStream = url.openStream();
        final InputStreamReader inputStreamReader = new InputStreamReader( inputStream );
        return new BufferedReader( inputStreamReader );
    }
}
