package org.apache.maven.surefire.util;

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

import org.apache.maven.surefire.SpecificTestClassFilter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Scans directories looking for tests.
 *
 * @author Karl M. Davis
 * @author Kristian Rosenvold
 */
@Deprecated
public class DefaultDirectoryScanner
    implements DirectoryScanner
{

    private static final String FS = System.getProperty( "file.separator" );

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final String JAVA_SOURCE_FILE_EXTENSION = ".java";

    private static final String JAVA_CLASS_FILE_EXTENSION = ".class";

    private final File basedir;

    private final List<String> includes;

    private final List<String> excludes;

    private final List<String> specificTests;

    public DefaultDirectoryScanner( File basedir, List<String> includes, List<String> excludes,
                                    List<String> specificTests )
    {
        this.basedir = basedir;
        this.includes = includes;
        this.excludes = excludes;
        this.specificTests = specificTests;
    }

    @Override
    public TestsToRun locateTestClasses( ClassLoader classLoader, ScannerFilter scannerFilter )
    {
        String[] testClassNames = collectTests();
        Set<Class<?>> result = new LinkedHashSet<Class<?>>();

        String[] specific = specificTests == null ? new String[0] : processIncludesExcludes( specificTests );
        SpecificTestClassFilter specificTestFilter = new SpecificTestClassFilter( specific );

        for ( String className : testClassNames )
        {
            Class<?> testClass = loadClass( classLoader, className );

            if ( !specificTestFilter.accept( testClass ) )
            {
                // FIXME: Log this somehow!
                continue;
            }

            if ( scannerFilter == null || scannerFilter.accept( testClass ) )
            {
                result.add( testClass );
            }
        }

        return new TestsToRun( result );
    }

    private static Class<?> loadClass( ClassLoader classLoader, String className )
    {
        try
        {
            return classLoader.loadClass( className );
        }
        catch ( ClassNotFoundException e )
        {
            throw new RuntimeException( "Unable to create test class '" + className + "'", e );
        }
    }

    String[] collectTests()
    {
        String[] tests = EMPTY_STRING_ARRAY;
        if ( basedir.exists() )
        {
            org.apache.maven.shared.utils.io.DirectoryScanner scanner =
                new org.apache.maven.shared.utils.io.DirectoryScanner();

            scanner.setBasedir( basedir );

            if ( includes != null )
            {
                scanner.setIncludes( processIncludesExcludes( includes ) );
            }

            if ( excludes != null )
            {
                scanner.setExcludes( processIncludesExcludes( excludes ) );
            }

            scanner.scan();

            tests = scanner.getIncludedFiles();
            for ( int i = 0; i < tests.length; i++ )
            {
                String test = tests[i];
                test = test.substring( 0, test.indexOf( "." ) );
                tests[i] = test.replace( FS.charAt( 0 ), '.' );
            }
        }
        return tests;
    }

    private static String[] processIncludesExcludes( List<String> list )
    {
        List<String> newList = new ArrayList<>();
        for ( String include : list )
        {
            String[] includes = include.split( "," );
            Collections.addAll( newList, includes );
        }

        String[] incs = new String[newList.size()];

        for ( int i = 0; i < incs.length; i++ )
        {
            String inc = newList.get( i );
            if ( inc.endsWith( JAVA_SOURCE_FILE_EXTENSION ) )
            {
                inc = inc.substring( 0, inc.lastIndexOf( JAVA_SOURCE_FILE_EXTENSION ) ) + JAVA_CLASS_FILE_EXTENSION;
            }
            incs[i] = inc;

        }
        return incs;
    }

}
