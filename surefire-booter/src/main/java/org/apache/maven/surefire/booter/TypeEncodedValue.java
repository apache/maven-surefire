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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Properties;
import org.apache.maven.surefire.util.NestedRuntimeException;
import org.apache.maven.surefire.util.ReflectionUtils;

/**
 * @author Kristian Rosenvold
 */
public class TypeEncodedValue
{
    String type;

    String value;

    public TypeEncodedValue( String type, String value )
    {
        this.type = type;
        this.value = value;
    }

    public boolean isTypeClass()
    {
        return Class.class.getName().equals( type );
    }

    public Object getDecodedValue()
    {
        return getDecodedValue( Thread.currentThread().getContextClassLoader() );
    }

    public Object getDecodedValue( ClassLoader classLoader )
    {
        if ( type.trim().length() == 0 )
        {
            return null;
        }
        else if ( type.equals( String.class.getName() ) )
        {
            return value;
        }
        else if ( isTypeClass() )
        {
            return ReflectionUtils.loadClass( classLoader, value );
        }
        else if ( type.equals( File.class.getName() ) )
        {
            return new File( value );
        }
        else if ( type.equals( Boolean.class.getName() ) )
        {
            return Boolean.valueOf( value );
        }
        else if ( type.equals( Integer.class.getName() ) )
        {
            return Integer.valueOf( value );
        }
        else if ( type.equals( Properties.class.getName() ) )
        {
            final Properties result = new Properties();
            try
            {
                ByteArrayInputStream bais = new ByteArrayInputStream( value.getBytes( "8859_1" ) );
                result.load( bais );
            }
            catch ( Exception e )
            {
                throw new NestedRuntimeException( "bug in property conversion", e );
            }
            return result;
        }
        else
        {
            throw new IllegalArgumentException( "Unknown parameter type: " + type );
        }
    }

    @SuppressWarnings( "SimplifiableIfStatement" )
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

        TypeEncodedValue that = (TypeEncodedValue) o;

        if ( type != null ? !type.equals( that.type ) : that.type != null )
        {
            return false;
        }
        return !( value != null ? !value.equals( that.value ) : that.value != null );

    }

    public int hashCode()
    {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + ( value != null ? value.hashCode() : 0 );
        return result;
    }
}
