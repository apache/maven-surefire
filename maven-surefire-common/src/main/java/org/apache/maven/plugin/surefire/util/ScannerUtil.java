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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

final class ScannerUtil {

	private ScannerUtil() {}

    private static final String FS = System.getProperty( "file.separator" );

    private static final String JAVA_SOURCE_FILE_EXTENSION = ".java";

    private static final String JAVA_CLASS_FILE_EXTENSION = ".class";
    
    private static final boolean IS_NON_UNIX_FS = (!FS.equals( "/" ));

    public static @Nonnull String convertToJavaClassName( @Nonnull String test )
    {
        return StringUtils.removeEnd( test, ".class" ).replace( FS, "." );
    }

    public static @Nonnull String convertJarFileResourceToJavaClassName( @Nonnull String test )
    {
        return StringUtils.removeEnd( test, ".class" ).replace( "/", "." );
    }

    public static @Nonnull String convertSlashToSystemFileSeparator( @Nonnull String path )
    {
        return ( IS_NON_UNIX_FS ? path.replace( "/", FS ) : path );
    }

    public static @Nonnull String stripBaseDir( String basedir, String test )
    {
        return StringUtils.removeStart( test, basedir );
    }

    public static @Nonnull String[] processIncludesExcludes( @Nonnull List<String> list )
    {
        List<String> newList = new ArrayList<String>();
        for ( Object aList : list )
        {
            String include = (String) aList;
            String[] includes = include.split( "," );
            Collections.addAll( newList, includes );
        }

        String[] incs = new String[newList.size()];

        for ( int i = 0; i < incs.length; i++ )
        {
            String inc = newList.get( i );
            if ( inc.endsWith( JAVA_SOURCE_FILE_EXTENSION ) )
            {
                inc = StringUtils.removeEnd( inc, JAVA_SOURCE_FILE_EXTENSION ) + JAVA_CLASS_FILE_EXTENSION;
            }
            incs[i] = convertSlashToSystemFileSeparator( inc );

        }
        return incs;
    }
}
