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
import org.apache.maven.surefire.group.match.AndGroupMatcher;
import org.apache.maven.surefire.group.match.GroupMatcher;
import org.apache.maven.surefire.group.parse.GroupMatcherParser;
import org.apache.maven.surefire.group.parse.ParseException;
import org.apache.maven.surefire.testng.group.GroupMatcherMethodSelector;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.NestedRuntimeException;
import org.testng.TestNG;

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
        // kind of ugly, but listeners are configured differently
        final String listeners = (String) options.remove( "listener" );
        // DGF In 4.7, default listeners dump XML files in the surefire-reports directory,
        // confusing the report plugin.  This was fixed in later versions.
        testng.setUseDefaultListeners( false );
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
        // TODO: we should have the Profile so that we can decide if this is needed or not
        testng.setListenerClasses( loadListenerClasses( listeners ) );

        loadGroupMatcher( testng, options );
    }

    private void loadGroupMatcher( TestNG testng, Map options )
        throws TestSetFailedException
    {
        String includes = (String) options.get( ProviderParameterNames.TESTNG_GROUPS_PROP );
        String excludes = (String) options.get( ProviderParameterNames.TESTNG_EXCLUDEDGROUPS_PROP );

        try
        {
            GroupMatcher in = includes == null ? null : new GroupMatcherParser( includes ).parse();
            GroupMatcher ex = excludes == null ? null : new GroupMatcherParser( excludes ).parse();

            GroupMatcher matcher = null;
            if ( in != null )
            {
                if ( ex != null )
                {
                    matcher = new AndGroupMatcher( new GroupMatcher[] { in, ex } );
                }
                else
                {
                    matcher = in;
                }
            }
            else if ( ex != null )
            {
                matcher = ex;
            }

            if ( matcher != null )
            {
                // HORRIBLE hack, but TNG doesn't allow us to setup a method selector instance directly.
                // Need some good way of setting the group-matching object / expression, and the test execution
                // should always be in-process from this point on...
                GroupMatcherMethodSelector.setGroupMatcher( matcher );
                testng.addMethodSelector( GroupMatcherMethodSelector.class.getName(), 0 );
            }
        }
        catch ( ParseException e )
        {
            throw new TestSetFailedException( "Cannot parse group includes/excludes expression(s):\nIncludes: "
                + includes + "\nExcludes: " + excludes, e );
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
            Method setter = target.getClass().getMethod( this.setterName, new Class[] { this.paramClass } );
            if ( setter != null )
            {
                setter.invoke( target, new Object[] { convertValue( value ) } );
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
