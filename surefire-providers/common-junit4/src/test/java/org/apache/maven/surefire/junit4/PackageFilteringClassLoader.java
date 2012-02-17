package org.apache.maven.surefire.junit4;

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

import java.util.Set;

/**
 * Emulate an OSGi classloader which only loads packages that have been imported via Import-Package MANIFEST header.
 */
public class PackageFilteringClassLoader
    extends ClassLoader
{

    private ClassLoader wrapped;

    private Set<String> visiblePackages;

    public PackageFilteringClassLoader( ClassLoader wrapped, Set<String> visiblePackages )
    {
        this.wrapped = wrapped;
        this.visiblePackages = visiblePackages;
    }

    @Override
    public Class<?> loadClass( String className )
        throws ClassNotFoundException
    {
        String packageName = "";
        int lastDot = className.lastIndexOf( '.' );
        if ( lastDot != -1 )
        {
            packageName = className.substring( 0, lastDot );
        }
        if ( visiblePackages.contains( packageName ) )
        {
            return wrapped.loadClass( className );
        }
        else
        {
            throw new ClassNotFoundException( className );
        }
    }

}
