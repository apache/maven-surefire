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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Kristian Rosenvold
 */
public class DefaultScanResult
    implements ScanResult
{
    private final List<String> files;

    private static final String SCAN_RESULT_NUMBER = "tc.";

    public DefaultScanResult( List<String> files )
    {
        this.files = Collections.unmodifiableList( files );
    }

    @Override
    public int size()
    {
        return files.size();
    }

    @Override
    public String getClassName( int index )
    {
        return files.get( index );
    }

    @Override
    public void writeTo( Map<String, String> properties )
    {
        for ( int i = 0, size = files.size(); i < size; i++ )
        {
            properties.put( SCAN_RESULT_NUMBER + i, files.get( i ) );
        }
    }

    public static DefaultScanResult from( Map<String, String> properties )
    {
        List<String> result = new ArrayList<String>();
        int i = 0;
        while ( true )
        {
            String item = properties.get( SCAN_RESULT_NUMBER + ( i++ ) );
            if ( item == null )
            {
                return new DefaultScanResult( result );
            }
            result.add( item );
        }
    }

    public boolean isEmpty()
    {
        return files.isEmpty();
    }

    public List getFiles()
    {
        return files;
    }

    @Override
    public TestsToRun applyFilter( ScannerFilter scannerFilter, ClassLoader testClassLoader )
    {
        Set<Class<?>> result = new LinkedHashSet<Class<?>>();

        int size = size();
        for ( int i = 0; i < size; i++ )
        {
            String className = getClassName( i );

            Class<?> testClass = loadClass( testClassLoader, className );

            if ( scannerFilter == null || scannerFilter.accept( testClass ) )
            {
                result.add( testClass );
            }
        }

        return new TestsToRun( result );
    }

    @Override
    public List<Class<?>> getClassesSkippedByValidation( ScannerFilter scannerFilter, ClassLoader testClassLoader )
    {
        List<Class<?>> result = new ArrayList<Class<?>>();

        int size = size();
        for ( int i = 0; i < size; i++ )
        {
            String className = getClassName( i );

            Class<?> testClass = loadClass( testClassLoader, className );

            if ( scannerFilter != null && !scannerFilter.accept( testClass ) )
            {
                result.add( testClass );
            }
        }

        return result;
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

    public DefaultScanResult append( DefaultScanResult other )
    {
        if ( other != null )
        {
            List<String> src = new ArrayList<String>( files );
            src.addAll( other.files );
            return new DefaultScanResult( src );
        }
        else
        {
            return this;
        }
    }

}
