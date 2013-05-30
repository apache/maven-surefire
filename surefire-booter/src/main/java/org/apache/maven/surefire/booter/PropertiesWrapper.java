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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.maven.surefire.util.internal.StringUtils;

/**
 * Makes java.util.Properties behave like it's 2013
 *
 * @author Kristian Rosenvold
 */
public class PropertiesWrapper
    implements KeyValueSource
{
    private final Properties properties;

    public PropertiesWrapper( Properties properties )
    {
        if ( properties == null )
        {
            throw new IllegalStateException( "Properties cannot be null" );
        }
        this.properties = properties;
    }

    public Properties getProperties()
    {
        return properties;
    }

    public void setAsSystemProperties()
    {
        for ( Object o : properties.keySet() )
        {
            String key = (String) o;

            System.setProperty( key, properties.getProperty( key ) );
        }
    }

    public String getProperty( String key )
    {
        return properties.getProperty( key );
    }

    public boolean getBooleanProperty( String propertyName )
    {
        return Boolean.valueOf( properties.getProperty( propertyName ) );
    }

    public Boolean getBooleanObjectProperty( String propertyName )
    {
        return Boolean.valueOf( properties.getProperty( propertyName ) );
    }

    public File getFileProperty( String key )
    {
        final String property = getProperty( key );
        if ( property == null )
        {
            return null;
        }
        TypeEncodedValue typeEncodedValue = new TypeEncodedValue( File.class.getName(), property );
        return (File) typeEncodedValue.getDecodedValue();
    }

    public List<String> getStringList( String propertyPrefix )
    {
        String value;
        List<String> result = new ArrayList<String>();
        // Whoa, C !!
        for ( int i = 0; ( value = getProperty( propertyPrefix + i ) ) != null; i++ )
        {
            result.add( value );
        }
        return result;
    }

    /**
     * Retrieves as single object that is persisted with type encoding
     *
     * @param key The key for the propery
     * @return The object, of a supported type
     */
    public TypeEncodedValue getTypeEncodedValue( String key )
    {
        String typeEncoded = getProperty( key );
        if ( typeEncoded == null )
        {
            return null;
        }
        int typeSep = typeEncoded.indexOf( "|" );
        String type = typeEncoded.substring( 0, typeSep );
        String value = typeEncoded.substring( typeSep + 1 );
        return new TypeEncodedValue( type, value );
    }


    Classpath getClasspath( String prefix )
    {
        List<String> elements = getStringList( prefix );
        return new Classpath( elements );
    }

    public void setClasspath( String prefix, Classpath classpath )
    {
        List classpathElements = classpath.getClassPath();
        for ( int i = 0; i < classpathElements.size(); ++i )
        {
            String element = (String) classpathElements.get( i );
            setProperty( prefix + i, element );
        }
    }


    public void setProperty( String key, String value )
    {
        if ( value != null )
        {
            properties.setProperty( key, value );
        }
    }

    public void addList( List items, String propertyPrefix )
    {
        if ( items == null || items.size() == 0 )
        {
            return;
        }
        int i = 0;
        for ( Object item : items )
        {
            if ( item == null )
            {
                throw new NullPointerException( propertyPrefix + i + " has null value" );
            }

            String[] stringArray = StringUtils.split( item.toString(), "," );

            for ( String aStringArray : stringArray )
            {
                properties.setProperty( propertyPrefix + i, aStringArray );
                i++;
            }

        }
    }

    public void copyTo( Map target )
    {
        Iterator iter = properties.keySet().iterator();
        Object key;
        while ( iter.hasNext() )
        {
            key = iter.next();
            //noinspection unchecked
            target.put( key, properties.get( key ) );
        }
    }
}
