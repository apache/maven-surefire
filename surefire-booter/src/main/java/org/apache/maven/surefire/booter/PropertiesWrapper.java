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
import java.util.List;
import java.util.Map;
import org.apache.maven.surefire.util.internal.StringUtils;

/**
 * @author Kristian Rosenvold
 */
public class PropertiesWrapper
    implements KeyValueSource
{
    private final Map<String, String> properties;

    public PropertiesWrapper( Map<String, String> properties )
    {
        if ( properties == null )
        {
            throw new IllegalStateException( "Properties cannot be null" );
        }
        this.properties = properties;
    }

    public Map<String, String> getProperties()
    {
        return properties;
    }

    public void setAsSystemProperties()
    {
        for ( Map.Entry<String, String> entry : properties.entrySet() )
        {
            System.setProperty( entry.getKey(), entry.getValue() );
        }
    }

    public String getProperty( String key )
    {
        return properties.get( key );
    }

    public boolean getBooleanProperty( String propertyName )
    {
        return Boolean.valueOf( properties.get( propertyName ) );
    }

    public int getIntProperty( String propertyName )
    {
        return Integer.parseInt( properties.get( propertyName ) );
    }

    public Long getLongProperty( String propertyName )
    {
        String number = getProperty( propertyName );
        return number == null ? null : Long.parseLong( number );
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
        List<String> result = new ArrayList<String>();
        for ( int i = 0; ; i++ )
        {
            String value = getProperty( propertyPrefix + i );

            if ( value == null )
            {
                return result;
            }

            result.add( value );
        }
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
        if ( typeEncoded != null )
        {
            int typeSep = typeEncoded.indexOf( "|" );
            String type = typeEncoded.substring( 0, typeSep );
            String value = typeEncoded.substring( typeSep + 1 );
            return new TypeEncodedValue( type, value );
        }
        else
        {
            return null;
        }
    }

    Classpath getClasspath( String prefix )
    {
        List<String> elements = getStringList( prefix );
        return new Classpath( elements );
    }

    public void setClasspath( String prefix, Classpath classpath )
    {
        List classpathElements = classpath.getClassPath();
        for ( int i = 0, size = classpathElements.size(); i < size; ++i )
        {
            String element = (String) classpathElements.get( i );
            setProperty( prefix + i, element );
        }
    }


    public void setProperty( String key, String value )
    {
        if ( value != null )
        {
            properties.put( key, value );
        }
    }

    public void addList( List items, String propertyPrefix )
    {
        if ( items != null && !items.isEmpty() )
        {
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
                    properties.put( propertyPrefix + i, aStringArray );
                    i++;
                }
            }
        }
    }

    @Override
    public void copyTo( Map<Object, Object> target )
    {
        target.putAll( properties );
    }
}
