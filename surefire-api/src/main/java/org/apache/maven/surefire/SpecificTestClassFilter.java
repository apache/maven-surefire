package org.apache.maven.surefire;

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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.maven.shared.utils.io.SelectorUtils;
import org.apache.maven.surefire.util.ScannerFilter;

/**
 * Filter for test class files
 *
 */
public class SpecificTestClassFilter
    implements ScannerFilter
{

    private static final char FS = System.getProperty( "file.separator" ).charAt( 0 );

    private static final String JAVA_CLASS_FILE_EXTENSION = ".class";

    private Set<String> names;

    public SpecificTestClassFilter( String[] classNames )
    {
        if ( classNames != null && classNames.length > 0 )
        {
            names = new HashSet<>();
            Collections.addAll( names, classNames );
        }
    }

    @Override
    public boolean accept( Class testClass )
    {
        // If the tests enumeration is empty, allow anything.
        boolean result = true;

        if ( names != null && !names.isEmpty() )
        {
            String className = testClass.getName().replace( '.', FS ) + JAVA_CLASS_FILE_EXTENSION;

            boolean found = false;
            for ( String pattern : names )
            {
                if ( '\\' == FS )
                {
                    pattern = pattern.replace( '/', FS );
                }

                // This is the same utility used under the covers in the plexus DirectoryScanner, and
                // therefore in the surefire DefaultDirectoryScanner implementation.
                if ( SelectorUtils.matchPath( pattern, className, true ) )
                {
                    found = true;
                    break;
                }
            }

            if ( !found )
            {
                result = false;
            }
        }

        return result;
    }

}
