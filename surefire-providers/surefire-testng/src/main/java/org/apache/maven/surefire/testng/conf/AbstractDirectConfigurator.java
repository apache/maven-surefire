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
import java.util.List;
import java.util.Map;

import org.apache.maven.surefire.booter.ProviderParameterNames;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.testng.TestNG;
import org.testng.xml.XmlSuite;

/**
 * Configurator that relies on reflection to set parameters in TestNG
 *
 */
public abstract class AbstractDirectConfigurator
    implements Configurator
{
    final Map<String, Setter> setters;

    AbstractDirectConfigurator()
    {
        Map<String, Setter> options = new HashMap<>();
        // options.put( ProviderParameterNames.TESTNG_GROUPS_PROP, new Setter( "setGroups", String.class ) );
        // options.put( ProviderParameterNames.TESTNG_EXCLUDEDGROUPS_PROP, new Setter( "setExcludedGroups", String.class
        // ) );
        options.put( "junit", new Setter( "setJUnit", Boolean.class ) );
        options.put( ProviderParameterNames.THREADCOUNT_PROP, new Setter( "setThreadCount", int.class ) );
        options.put( "usedefaultlisteners", new Setter( "setUseDefaultListeners", boolean.class ) );
        this.setters = options;
    }

    @Override
    public void configure( TestNG testng, Map<String, String> options )
        throws TestSetFailedException
    {
        System.out.println( "\n\n\n\nCONFIGURING TESTNG\n\n\n\n" );
        // kind of ugly, but listeners are configured differently
        final String listeners = options.remove( "listener" );
        // DGF In 4.7, default listeners dump XML files in the surefire-reports directory,
        // confusing the report plugin.  This was fixed in later versions.
        testng.setUseDefaultListeners( false );
        configureInstance( testng, options );
        // TODO: we should have the Profile so that we can decide if this is needed or not
        testng.setListenerClasses( loadListenerClasses( listeners ) );
    }

    @Override
    public void configure( XmlSuite suite, Map<String, String> options )
        throws TestSetFailedException
    {
        Map<String, String> filtered = filterForSuite( options );
        configureInstance( suite, filtered );
    }

    protected Map<String, String> filterForSuite( Map<String, String> options )
    {
        Map<String, String> result = new HashMap<>();
        addPropIfNotNull( options, result, ProviderParameterNames.PARALLEL_PROP );
        addPropIfNotNull( options, result, ProviderParameterNames.THREADCOUNT_PROP );
        return result;
    }

    private void addPropIfNotNull( Map<String, String> options, Map<String, String> result, String prop )
    {
        if ( options.containsKey( prop ) )
        {
            result.put( prop, options.get( prop ) );
        }
    }

    private void configureInstance( Object testngInstance, Map<String, String> options )
    {
        for ( Map.Entry<String, String> entry : options.entrySet() )
        {
            String key = entry.getKey();
            String val = entry.getValue();
            Setter setter = setters.get( key );
            if ( setter != null )
            {
                try
                {
                    setter.invoke( testngInstance, val );
                }
                catch ( Exception e )
                {
                    throw new RuntimeException( "Cannot set option " + key + " with value " + val, e );
                }
            }
        }
    }

    static List<Class> loadListenerClasses( String listenerClasses )
        throws TestSetFailedException
    {
        if ( listenerClasses == null || listenerClasses.trim().isEmpty() )
        {
            return new ArrayList<>();
        }

        List<Class> classes = new ArrayList<>();
        String[] classNames = listenerClasses.split( "\\s*,\\s*(\\r?\\n)?\\s*" );
        for ( String className : classNames )
        {
            Class<?> clazz = loadClass( className );
            classes.add( clazz );
        }

        return classes;
    }

    static Class<?> loadClass( String className )
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

    /**
     * Describes a property setter by method name and parameter class
     *
     */
    public static final class Setter
    {
        private final String setterName;

        private final Class<?> paramClass;

        public Setter( String name, Class<?> clazz )
        {
            setterName = name;
            paramClass = clazz;
        }

        public void invoke( Object target, String value )
            throws Exception
        {
            Method setter = target.getClass().getMethod( setterName, paramClass );
            if ( setter != null )
            {
                setter.invoke( target, convertValue( value ) );
            }
        }

        private Object convertValue( String value )
        {
            if ( value == null )
            {
                return null;
            }
            if ( paramClass.isAssignableFrom( value.getClass() ) )
            {
                return value;
            }

            if ( Boolean.class.equals( paramClass ) || boolean.class.equals( paramClass ) )
            {
                return Boolean.valueOf( value );
            }
            if ( Integer.class.equals( paramClass ) || int.class.equals( paramClass ) )
            {
                return Integer.valueOf( value );
            }

            return value;
        }
    }
}
