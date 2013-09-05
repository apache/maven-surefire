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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.surefire.booter.ProviderParameterNames;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.testng.TestNG;
import org.testng.xml.XmlSuite;

public abstract class AbstractDirectConfigurator
    implements Configurator
{
    final Map setters;

    AbstractDirectConfigurator()
    {
        Map options = new HashMap();
        // options.put( ProviderParameterNames.TESTNG_GROUPS_PROP, new Setter( "setGroups", String.class ) );
        // options.put( ProviderParameterNames.TESTNG_EXCLUDEDGROUPS_PROP, new Setter( "setExcludedGroups", String.class
        // ) );
        options.put( "junit", new Setter( "setJUnit", Boolean.class ) );
        options.put( ProviderParameterNames.THREADCOUNT_PROP, new Setter( "setThreadCount", int.class ) );
        options.put( "usedefaultlisteners", new Setter( "setUseDefaultListeners", boolean.class ) );
        this.setters = options;
    }

    public void configure( TestNG testng, Map options )
        throws TestSetFailedException
    {
        System.out.println( "\n\n\n\nCONFIGURING TESTNG\n\n\n\n" );
        // kind of ugly, but listeners are configured differently
        final String listeners = (String) options.remove( "listener" );
        // DGF In 4.7, default listeners dump XML files in the surefire-reports directory,
        // confusing the report plugin.  This was fixed in later versions.
        testng.setUseDefaultListeners( false );
        configureInstance( testng, options );
        // TODO: we should have the Profile so that we can decide if this is needed or not
        testng.setListenerClasses( loadListenerClasses( listeners ) );
    }

    public void configure( XmlSuite suite, Map options )
        throws TestSetFailedException
    {
        Map filtered = filterForSuite( options );
        configureInstance( suite, filtered );
    }


    protected Map filterForSuite( Map options )
    {
        Map result = new HashMap();
        addPropIfNotNull( options, result, ProviderParameterNames.PARALLEL_PROP );
        addPropIfNotNull( options, result, ProviderParameterNames.THREADCOUNT_PROP );
        return result;
    }

    private void addPropIfNotNull( Map options, Map result, String prop )
    {
        if ( options.containsKey( prop ) )
        {
            result.put( prop, options.get( prop ) );
        }
    }

    private void configureInstance( Object testngInstance, Map options )
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
                    setter.invoke( testngInstance, val );
                }
                catch ( Exception ex )
                {
                    throw new RuntimeException( "Cannot set option " + key + " with value " + val, ex );
                }

            }
        }
    }

    public static List loadListenerClasses( String listenerClasses )
        throws TestSetFailedException
    {
        if ( listenerClasses == null || "".equals( listenerClasses.trim() ) )
        {
            return new ArrayList();
        }

        List classes = new ArrayList();
        String[] classNames = listenerClasses.split( " *, *" );
        for ( int i = 0; i < classNames.length; i++ )
        {
            String className = classNames[i];
            Class clazz = loadClass( className );
            classes.add( clazz );
        }

        return classes;
    }

    public static Class loadClass( String className )
        throws TestSetFailedException
    {
        try
        {
            return Class.forName( className );
        }
        catch ( Exception ex )
        {
            throw new TestSetFailedException( "Cannot find listener class " + className, ex );
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
            Method setter = target.getClass().getMethod( this.setterName, new Class[]{ this.paramClass } );
            if ( setter != null )
            {
                setter.invoke( target, new Object[]{ convertValue( value ) } );
            }
        }

        Object convertValue( Object value )
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
