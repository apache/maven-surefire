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

import org.apache.maven.surefire.util.ReflectionUtils;
import org.apache.maven.surefire.util.internal.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.*;

/**
 * @author Kristian Rosenvold
 */
public class PropertiesWrapper
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
        for ( Iterator i = properties.keySet().iterator(); i.hasNext(); )
        {
            String key = (String) i.next();

            System.setProperty( key, properties.getProperty( key ) );
        }
    }

    public String getProperty( String key )
    {
        return properties.getProperty( key );
    }

    public boolean getBooleanProperty( String propertyName )
    {
        final Boolean aBoolean = Boolean.valueOf( properties.getProperty( propertyName ) );
        return aBoolean.booleanValue();
    }

    public File getFileProperty( String key )
    {
        final String property = getProperty( key );
        if ( property == null )
        {
            return null;
        }
        return (File) getParamValue( property, File.class.getName() );
    }

    public List getListOfTypedObjects( String propertyPrefix )
    {
        String type;
        String value;
        List result = new ArrayList();
        for ( int i = 0; ( type = getProperty( propertyPrefix + i + BooterConstants.TYPES_SUFIX ) ) != null; i++ )
        {
            value = getProperty( propertyPrefix + i + BooterConstants.PARAMS_SUFIX );
            result.add( getParamValue( value, type ) );
        }
        return result;
    }

    public List getStringList( String propertyPrefix )
    {
        String value;
        List result = new ArrayList();
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
    public Object getTypeDecoded( String key )
    {
        String typeEncoded = getProperty( key );
        if ( typeEncoded == null )
        {
            return null;
        }
        int typeSep = typeEncoded.indexOf( "|" );
        String type = typeEncoded.substring( 0, typeSep );
        String value = typeEncoded.substring( typeSep + 1 );
        return getParamValue( value, type );
    }

    private Object getParamValue( String param, String typeName )
    {
        if ( typeName.trim().length() == 0 )
        {
            return null;
        }
        else if ( typeName.equals( String.class.getName() ) )
        {
            return param;
        }
        else if ( typeName.equals( Class.class.getName() ) )
        {
            return ReflectionUtils.loadClass( Thread.currentThread().getContextClassLoader(), param );
        }
        else if ( typeName.equals( File.class.getName() ) )
        {
            return new File( param );
        }
        else if ( typeName.equals( File[].class.getName() ) )
        {
            List stringList = processStringList( param );
            File[] fileList = new File[stringList.size()];
            for ( int j = 0; j < stringList.size(); j++ )
            {
                fileList[j] = new File( (String) stringList.get( j ) );
            }
            return fileList;
        }
        else if ( typeName.equals( ArrayList.class.getName() ) )
        {
            return processStringList( param );
        }
        else if ( typeName.equals( Boolean.class.getName() ) )
        {
            return Boolean.valueOf( param );
        }
        else if ( typeName.equals( Integer.class.getName() ) )
        {
            return Integer.valueOf( param );
        }
        else if ( typeName.equals( Properties.class.getName() ) )
        {
            final Properties result = new Properties();
            try
            {
                ByteArrayInputStream bais = new ByteArrayInputStream( param.getBytes( "8859_1" ) );
                result.load( bais );
            }
            catch ( Exception e )
            {
                throw new RuntimeException( "bug in property conversion", e );
            }
            return result;
        }
        else
        {
            // TODO: could attempt to construct with a String constructor if needed
            throw new IllegalArgumentException( "Unknown parameter type: " + typeName );
        }
    }

    private static List processStringList( String stringList )
    {
        String sl = stringList;

        if ( sl.startsWith( "[" ) && sl.endsWith( "]" ) )
        {
            sl = sl.substring( 1, sl.length() - 1 );
        }

        List list = new ArrayList();

        String[] stringArray = StringUtils.split( sl, "," );

        for ( int i = 0; i < stringArray.length; i++ )
        {
            list.add( stringArray[i].trim() );
        }
        return list;
    }

    public void setProperty( String key, File file )
    {
        if ( file != null )
        {
            setProperty( key, file.toString() );
        }
    }

    public void setProperty( String key, Boolean aBoolean )
    {
        if ( aBoolean != null )
        {
            setProperty( key, aBoolean.toString() );
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
        for (Iterator iterator = items.iterator(); iterator.hasNext();)
        {
            Object item = iterator.next();
            if ( item == null )
            {
                throw new NullPointerException( propertyPrefix + i + " has null value" );
            }

            String[] stringArray = StringUtils.split(item.toString(), ",");

            for ( int j = 0; j < stringArray.length; j++ )
            {
                properties.setProperty( propertyPrefix + i, stringArray[j] );
                i++;
            }

        }
    }

}
