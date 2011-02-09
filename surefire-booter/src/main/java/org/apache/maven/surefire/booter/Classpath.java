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

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.maven.surefire.util.UrlUtils;

/**
 * An ordered set of classpath elements
 *
 * @author Kristian Rosenvold
 */
public class Classpath
{
    static Classpath readFromForkProperties( PropertiesWrapper properties, String prefix )
    {
        List elements = properties.getStringList( prefix );
        return new Classpath( elements );
    }

    public static Classpath join( Classpath firstClasspath, Classpath secondClasspath )
    {
        Classpath joinedClasspath = new Classpath();
        joinedClasspath.addElementsOfClasspath( firstClasspath );
        joinedClasspath.addElementsOfClasspath( secondClasspath );
        return joinedClasspath;
    }

    private final List elements = new ArrayList();

    public Classpath()
    {
    }

    private Classpath( Collection elements )
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

    private void addElements( Collection additionalElements )
    {
        for ( Iterator it = additionalElements.iterator(); it.hasNext(); )
        {
            String element = (String) it.next();
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
        return new ArrayList( elements );
    }

    public List getAsUrlList()
        throws MalformedURLException
    {
        List urls = new ArrayList();
        for ( Iterator i = elements.iterator(); i.hasNext(); )
        {
            String url = (String) i.next();
            File f = new File( url );
            urls.add( UrlUtils.getURL( f ) );
        }
        return urls;
    }

    void writeToForkProperties( Properties properties, String prefix )
    {
        for ( int i = 0; i < elements.size(); ++i )
        {
            String element = (String) elements.get( i );
            properties.setProperty( prefix + i, element );
        }
    }

    public void writeToSystemProperty( String propertyName )
    {
        StringBuffer sb = new StringBuffer();
        for ( Iterator i = elements.iterator(); i.hasNext(); )
        {
            sb.append( (String) i.next() ).append( File.pathSeparatorChar );
        }
        System.setProperty( propertyName, sb.toString() );
    }
}
