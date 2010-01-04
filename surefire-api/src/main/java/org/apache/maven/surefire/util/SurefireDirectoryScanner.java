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

import org.codehaus.plexus.util.StringUtils;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.testset.SurefireTestSet;

import java.util.*;
import java.io.File;
import java.lang.reflect.Modifier;

/**
 * Scans directories looking for tests.
 * @author Karl M. Davis
 * @author Kristian Rosenvold
 */
public class SurefireDirectoryScanner {

    private static final String FS = System.getProperty( "file.separator" );
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private final File basedir;

    private final List includes;

    private final List excludes;

    protected Map testSets;

    private int totalTests;


    public Map getTestSets() {
        return testSets;
    }

    public SurefireDirectoryScanner(File basedir, List includes, List excludes) {
        this.basedir = basedir;
        this.includes = includes;
        this.excludes = excludes;
    }

    public interface TestSetCreator{
        SurefireTestSet createTestSet(Class clazz);
    }

    public Map locateTestSets( ClassLoader classLoader, TestSetCreator testSetCreator )
        throws TestSetFailedException
    {
        if ( testSets != null )
        {
            throw new IllegalStateException( "You can't call locateTestSets twice" );
        }
        testSets = new HashMap();

        Class[] locatedClasses = locateTestClasses( classLoader);

        for ( int i = 0; i < locatedClasses.length; i++ )
        {
            Class testClass = locatedClasses[i];
            SurefireTestSet testSet = testSetCreator.createTestSet( testClass);

                if ( testSet == null )
                {
                    continue;
                }

                if ( testSets.containsKey( testSet.getName() ) )
                {
                    throw new TestSetFailedException( "Duplicate test set '" + testSet.getName() + "'" );
                }
                testSets.put( testSet.getName(), testSet );

                totalTests++;
        }

        return Collections.unmodifiableMap( testSets );
    }

    public Class[] locateTestClasses( ClassLoader classLoader)
        throws TestSetFailedException
    {
        String[] testClassNames =   collectTests( );
        List result = new ArrayList();

        for ( int i = 0; i < testClassNames.length; i++ )
        {
            String className = testClassNames[i];

            Class testClass;
            try
            {
                testClass = classLoader.loadClass( className );
            }
            catch ( ClassNotFoundException e )
            {
                throw new TestSetFailedException( "Unable to create test class '" + className + "'", e );
            }

            if ( !Modifier.isAbstract( testClass.getModifiers() ) )
            {

                result.add( testClass);
            }
        }
        return (Class[]) result.toArray(new Class[result.size()]);
    }


    String[] collectTests( )
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
            incs[i] = StringUtils.replace( (String) list.get( i ), "java", "class" );

        }
        return incs;
    }



}
