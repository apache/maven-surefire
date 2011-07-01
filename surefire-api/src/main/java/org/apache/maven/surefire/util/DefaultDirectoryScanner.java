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

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Scans directories looking for tests.
 * 
 * @author Karl M. Davis
 * @author Kristian Rosenvold
 */
public class DefaultDirectoryScanner
    implements DirectoryScanner
{

    private static final String FS = System.getProperty( "file.separator" );

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final String JAVA_SOURCE_FILE_EXTENSION = ".java";

    private static final String JAVA_CLASS_FILE_EXTENSION = ".class";

    private final File basedir;

    private final List includes;

    private final List excludes;

    private final List classesSkippedByValidation = new ArrayList();

    private final Comparator sortOrder;

    private final RunOrder runOrder;

    public DefaultDirectoryScanner( File basedir, List includes, List excludes, RunOrder runOrder )
    {
        this.basedir = basedir;
        this.includes = includes;
        this.excludes = excludes;
        this.runOrder = runOrder;
        this.sortOrder = getSortOrderComparator();
    }

    public TestsToRun locateTestClasses( ClassLoader classLoader, ScannerFilter scannerFilter )
    {
        String[] testClassNames = collectTests();
        List result = new ArrayList();

        for ( int i = 0; i < testClassNames.length; i++ )
        {
            String className = testClassNames[i];

            Class testClass = loadClass( classLoader, className );

            if ( scannerFilter == null || scannerFilter.accept( testClass ) )
            {
                result.add( testClass );
            }
            else
            {
                classesSkippedByValidation.add( testClass );
            }
        }
        orderTestClasses( result );
        return new TestsToRun( result );
    }

    private static Class loadClass( ClassLoader classLoader, String className )
    {
        Class testClass;
        try
        {
            testClass = classLoader.loadClass( className );
        }
        catch ( ClassNotFoundException e )
        {
            throw new NestedRuntimeException( "Unable to create test class '" + className + "'", e );
        }
        return testClass;
    }

    String[] collectTests()
    {
        String[] tests = EMPTY_STRING_ARRAY;
        if ( basedir.exists() )
        {
            org.codehaus.plexus.util.DirectoryScanner scanner = new org.codehaus.plexus.util.DirectoryScanner();

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

    private static String[] processIncludesExcludes( List list )
    {
        String[] incs = new String[list.size()];

        for ( int i = 0; i < incs.length; i++ )
        {
            String inc = (String) list.get( i );
            if ( inc.endsWith( JAVA_SOURCE_FILE_EXTENSION ) )
            {
                inc =
                    new StringBuffer( inc.length() - JAVA_SOURCE_FILE_EXTENSION.length()
                        + JAVA_CLASS_FILE_EXTENSION.length() ).append( inc.substring( 0,
                                                                                      inc.lastIndexOf( JAVA_SOURCE_FILE_EXTENSION ) ) ).append( JAVA_CLASS_FILE_EXTENSION ).toString();
            }
            incs[i] = inc;

        }
        return incs;
    }

    public List getClassesSkippedByValidation()
    {
        return classesSkippedByValidation;
    }

    private void orderTestClasses( List testClasses )
    {
        if ( RunOrder.RANDOM.equals( runOrder ) )
        {
            Collections.shuffle( testClasses );
        }
        else if ( sortOrder != null )
        {
            Collections.sort( testClasses, sortOrder );
        }
    }

    private Comparator getSortOrderComparator()
    {
        if ( RunOrder.ALPHABETICAL.equals( runOrder ) )
        {
            return getAlphabeticalComparator();
        }
        else if ( RunOrder.REVERSE_ALPHABETICAL.equals( runOrder ) )
        {
            return getReverseAlphabeticalComparator();
        }
        else if ( RunOrder.HOURLY.equals( runOrder ) )
        {
            final int hour = Calendar.getInstance().get( Calendar.HOUR_OF_DAY );
            return ( ( hour % 2 ) == 0 ) ? getAlphabeticalComparator() : getReverseAlphabeticalComparator();
        }
        else
        {
            return null;
        }
    }

    private Comparator getReverseAlphabeticalComparator()
    {
        return new Comparator()
        {
            public int compare( Object o1, Object o2 )
            {
                return ( (Class) o2 ).getName().compareTo( ( (Class) o1 ).getName() );
            }
        };
    }

    private Comparator getAlphabeticalComparator()
    {
        return new Comparator()
        {
            public int compare( Object o1, Object o2 )
            {
                return ( (Class) o1 ).getName().compareTo( ( (Class) o2 ).getName() );
            }
        };
    }

}
