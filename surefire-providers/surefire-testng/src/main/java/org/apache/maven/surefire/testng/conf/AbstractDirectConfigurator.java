package org.apache.maven.surefire.testng.conf;

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

import org.apache.maven.surefire.util.NestedRuntimeException;
import org.testng.TestNG;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class AbstractDirectConfigurator
    implements Configurator
{
    protected final Map setters;

    protected AbstractDirectConfigurator()
    {
        Map options = new HashMap();
        options.put( "groups", new Setter( "setGroups", String.class ) );
        options.put( "excludedgroups", new Setter( "setExcludedGroups", String.class ) );
        options.put( "junit", new Setter( "setJUnit", Boolean.class ) );
        options.put( "threadcount", new Setter( "setThreadCount", int.class ) );
        this.setters = options;
    }

    public void configure( TestNG testng, Map options )
    {
        for ( Iterator it = options.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();
            String key = (String) entry.getKey();
            Object val = entry.getValue();

            Setter setter = (Setter) setters.get( key );
            if ( setter != null )
            {
                try
                {
                    setter.invoke( testng, val );
                }
                catch ( Exception ex )
                {
                    throw new NestedRuntimeException( "Cannot set option " + key + " with value " + val, ex );
                }

            }
        }
    }

    public static final class Setter
    {
        private final String setterName;

        private final Class paramClass;

        public Setter( String name, Class clazz )
        {
            this.setterName = name;
            this.paramClass = clazz;
        }

        public void invoke( Object target, Object value )
            throws Exception
        {
            Method setter = target.getClass().getMethod( this.setterName, new Class[]{this.paramClass} );
            if ( setter != null )
            {
                setter.invoke( target, new Object[]{convertValue( value )} );
            }
        }

        protected Object convertValue( Object value )
        {
            if ( value == null )
            {
                return value;
            }
            if ( this.paramClass.isAssignableFrom( value.getClass() ) )
            {
                return value;
            }

            if ( Boolean.class.equals( this.paramClass ) || boolean.class.equals( this.paramClass ) )
            {
                return Boolean.valueOf( value.toString() );
            }
            if ( Integer.class.equals( this.paramClass ) || int.class.equals( this.paramClass ) )
            {
                return new Integer( value.toString() );
            }

            return value;
        }
    }
}
