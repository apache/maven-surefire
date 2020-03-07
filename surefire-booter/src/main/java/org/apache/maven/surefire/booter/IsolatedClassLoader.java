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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

/**
 * Loads classes from jar files added via {@link #addURL(URL)}.
 */
public class IsolatedClassLoader
    extends URLClassLoader
{
    private final ClassLoader parent = ClassLoader.getSystemClassLoader();

    private final Set<URL> urls = new HashSet<>();

    private final String roleName;

    private boolean childDelegation = true;

    private static final URL[] EMPTY_URL_ARRAY = new URL[0];

    public IsolatedClassLoader( ClassLoader parent, boolean childDelegation, String roleName )
    {
        super( EMPTY_URL_ARRAY, parent );

        this.childDelegation = childDelegation;

        this.roleName = roleName;
    }

    /**
     * @deprecated this method will use {@link java.io.File} instead of {@link URL} in the next
     * major version.
     */
    @Override
    @Deprecated
    public void addURL( URL url )
    {
        // avoid duplicates
        // todo avoid URL due to calling equals method may cause some overhead due to resolving host or file.
        if ( !urls.contains( url ) )
        {
            super.addURL( url );
            urls.add( url );
        }
    }

    @Override
    public synchronized Class loadClass( String name )
        throws ClassNotFoundException
    {
        if ( childDelegation )
        {
            Class<?> c = findLoadedClass( name );

            if ( c == null )
            {
                try
                {
                    c = findClass( name );
                }
                catch ( ClassNotFoundException e )
                {
                    if ( parent == null )
                    {
                        throw e;
                    }
                    else
                    {
                        c = parent.loadClass( name );
                    }
                }
            }

            return c;
        }
        else
        {
            return super.loadClass( name );
        }
    }

    @Override
    public String toString()
    {
        return "IsolatedClassLoader{roleName='" + roleName + "'}";
    }
}
