package org.apache.maven.plugin.surefire.util;

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

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.maven.shared.utils.io.SelectorUtils;

import static org.apache.maven.plugin.surefire.util.ScannerUtil.convertSlashToSystemFileSeparator;

public class SpecificFileFilter
{

    private Set<String> names;

    public SpecificFileFilter( @Nullable String[] classNames )
    {
        if ( classNames != null && classNames.length > 0 )
        {
            this.names = new HashSet<String>();
            for ( String name : classNames )
            {
                names.add( convertSlashToSystemFileSeparator( name ) );
            }
        }
    }

    public boolean accept( @Nullable String resourceName )
    {
        // If the tests enumeration is empty, allow anything.
        if ( names != null && !names.isEmpty() )
        {
            for ( String pattern : names )
            {
                // This is the same utility used under the covers in the plexus DirectoryScanner, and
                // therefore in the surefire DefaultDirectoryScanner implementation.
                if ( SelectorUtils.matchPath( pattern, resourceName, true ) )
                {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

}
