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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * An ordered set of classpath elements
 *
 * @author Kristian Rosenvold
 */
public class Classpath
{
    private final List elements;

    private final Set elementSet;

    public Classpath()
    {
        this( new ArrayList() );
    }

    private Classpath( List elements )
    {
        this.elements = elements;
        this.elementSet = new HashSet( elements );
        if ( elements.size() != elementSet.size() )
        {
            throw new IllegalStateException( "This is not permitted and is a violation of contract" );
        }
    }

    public List getClassPath()
    {
        return elements;
    }

    public Classpath append( Classpath otherClassPathToAppend )
    {
        int additionalLength = otherClassPathToAppend != null ? otherClassPathToAppend.size() : 0;
        List combinedClassPath = new ArrayList( elements.size() + additionalLength );

        combinedClassPath.addAll( elements );

        if ( otherClassPathToAppend != null )
        {
            Iterator iterator = otherClassPathToAppend.getClassPath().iterator();
            while ( iterator.hasNext() )
            {
                String element = (String) iterator.next();
                if ( !elementSet.contains( element ) )
                {
                    combinedClassPath.add( element );
                }
            }
        }
        return new Classpath( combinedClassPath );
    }


    public void addClassPathElementUrl( String path )
    {
        if ( !elementSet.contains( path ) )
        {
            elements.add( path );
            elementSet.add( path );
        }
    }

    public Object get( int index )
    {
        return elements.get( index );
    }

    public int size()
    {
        return elements.size();
    }

    public String getClassPathAsString()
    {
        StringBuffer sb = new StringBuffer();
        for ( int i = 0; i < elements.size(); i++ )
        {
            sb.append( elements.get( i ) ).append( File.pathSeparatorChar );
        }
        return sb.toString();
    }

    public List getAsUrlList()
        throws MalformedURLException
    {
        List urls = new ArrayList();

        for ( Iterator i = elements.iterator(); i.hasNext(); )
        {
            String url = (String) i.next();

            if ( url != null )
            {
                File f = new File( url );
                urls.add( UrlUtils.getURL( f ) );
            }
        }
        return urls;
    }

    public void setForkProperties( Properties properties, String prefix )
    {
        for ( int i = 0; i < elements.size(); i++ )
        {
            String url = (String) elements.get( i );
            properties.setProperty( prefix + i, url );
        }
    }

}
