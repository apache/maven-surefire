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

import org.apache.maven.surefire.util.UrlUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An ordered list of classpath elements with set behaviour
 *
 * @author Kristian Rosenvold
 */
public class Classpath
{
    private static final JdkReflector jdkReflector = new JdkReflector();

    public static Classpath join( Classpath firstClasspath, Classpath secondClasspath )
    {
        Classpath joinedClasspath = new Classpath();
        joinedClasspath.addElementsOfClasspath( firstClasspath );
        joinedClasspath.addElementsOfClasspath( secondClasspath );
        return joinedClasspath;
    }

    private final List<String> elements;

    public Classpath()
    {
        this.elements = new ArrayList<String>();
    }

    public Classpath( Classpath other )
    {
        this.elements = new ArrayList<String>( other.elements );
    }

    public Classpath( List<String> elements )
    {
        this();
        addElements( elements );
    }

    public void addClassPathElementUrl( String path )
    {
        if ( path == null )
        {
            throw new IllegalArgumentException( "Null is not a valid class path element url." );
        }
        else if ( !elements.contains( path ) )
        {
            elements.add( path );
        }
    }

    private void addElements( List<String> additionalElements )
    {
        for ( Object additionalElement : additionalElements )
        {
            String element = (String) additionalElement;
            addClassPathElementUrl( element );
        }
    }

    private void addElementsOfClasspath( Classpath otherClasspath )
    {
        if ( otherClasspath != null )
        {
            addElements( otherClasspath.elements );
        }
    }

    public List getClassPath()
    {
        return Collections.unmodifiableList( elements );
    }

    public List getAsUrlList()
        throws MalformedURLException
    {
        List<URL> urls = new ArrayList<URL>();
        for ( String url : elements )
        {
            File f = new File( url );
            urls.add( UrlUtils.getURL( f ) );
        }
        return urls;
    }

    public void writeToSystemProperty( String propertyName )
    {
        StringBuilder sb = new StringBuilder();
        for ( String element : elements )
        {
            sb.append( element ).append( File.pathSeparatorChar );
        }
        System.setProperty( propertyName, sb.toString() );
    }

    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        Classpath classpath = (Classpath) o;

        return !( elements != null ? !elements.equals( classpath.elements ) : classpath.elements != null );

    }

    public ClassLoader createClassLoader( ClassLoader parent, boolean childDelegation, boolean enableAssertions,
                                          String roleName )
        throws SurefireExecutionException
    {
        try
        {
            List urls = getAsUrlList();
            IsolatedClassLoader classLoader = new IsolatedClassLoader( parent, childDelegation, roleName );
            for ( Object url1 : urls )
            {
                URL url = (URL) url1;
                classLoader.addURL( url );
            }
            if ( parent != null )
            {
                jdkReflector.invokeAssertionStatusMethod( parent, enableAssertions );
            }
            jdkReflector.invokeAssertionStatusMethod( classLoader, enableAssertions );
            return classLoader;
        }
        catch ( MalformedURLException e )
        {
            throw new SurefireExecutionException( "When creating classloader", e );
        }
    }


    public int hashCode()
    {
        return elements != null ? elements.hashCode() : 0;
    }

    public String getLogMessage( String descriptor )
    {
        StringBuilder result = new StringBuilder();
        result.append( descriptor ).append( " classpath:" );
        for ( String element : elements )
        {
            result.append( "  " ).append( element );
        }
        return result.toString();
    }
}
