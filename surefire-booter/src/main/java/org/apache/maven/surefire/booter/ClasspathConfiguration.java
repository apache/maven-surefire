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

import org.apache.maven.surefire.util.NestedRuntimeException;
import org.apache.maven.surefire.util.UrlUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;

/**
 * Represents the classpaths for the BooterConfiguration.
 * <p/>
 *
 * @author Jason van Zyl
 * @author Emmanuel Venisse
 * @author Kristian Rosenvold
 * @version $Id$
 */
public class ClasspathConfiguration
{

    private final List classPathUrls = new ArrayList();

    private final List surefireClassPathUrls = new ArrayList();

    private final List surefireBootClassPathUrls = new ArrayList();

    /**
     * Whether to enable assertions or not (can be affected by the fork arguments, and the ability to do so based on the
     * JVM).
     */
    private final boolean enableAssertions;

    // todo: @deprecated because the IsolatedClassLoader is really isolated - no parent.
    private final boolean childDelegation;



    public ClasspathConfiguration( boolean enableAssertions, boolean childDelegation)
    {
        this.enableAssertions = enableAssertions;
        this.childDelegation = childDelegation;
    }

    /*
   * Reads the config from the supplied stream. Closes the stream.
    */
    public ClasspathConfiguration( SortedMap classPathUrls, SortedMap surefireClassPathUrls, Collection booterClassPath,
                                   boolean enableAssertions, boolean childDelegation )
        throws IOException
    {

        this.enableAssertions = enableAssertions;
        this.childDelegation = childDelegation;
        for ( Iterator cpi = classPathUrls.keySet().iterator(); cpi.hasNext(); )
        {
            String url = (String) classPathUrls.get( cpi.next() );
            this.classPathUrls.add( url );
        }

        for ( Iterator scpi = surefireClassPathUrls.keySet().iterator(); scpi.hasNext(); )
        {
            String url = (String) surefireClassPathUrls.get( scpi.next() );
            this.surefireClassPathUrls.add( url );
        }
        for ( Iterator scpi = booterClassPath.iterator(); scpi.hasNext(); )
        {
            String url = (String) surefireClassPathUrls.get( scpi.next() );
            this.surefireBootClassPathUrls.add( url );
        }

    }


    public void setForkProperties( Properties properties )
    {
        for ( int i = 0; i < classPathUrls.size(); i++ )
        {
            String url = (String) classPathUrls.get( i );
            properties.setProperty( "classPathUrl." + i, url );
        }

        for ( int i = 0; i < surefireClassPathUrls.size(); i++ )
        {
            String url = (String) surefireClassPathUrls.get( i );
            properties.setProperty( "surefireClassPathUrl." + i, url );
        }

        properties.setProperty( "enableAssertions", String.valueOf( enableAssertions ) );
        properties.setProperty( "childDelegation", String.valueOf( childDelegation ) );
    }


    private static Method assertionStatusMethod;

    static
    {
        try
        {
            assertionStatusMethod =
                ClassLoader.class.getMethod( "setDefaultAssertionStatus", new Class[]{ boolean.class } );
        }
        catch ( NoSuchMethodException e )
        {
            assertionStatusMethod = null;
        }
    }

    public List getBootClasspath( boolean useSystemClassLoader )
    {
        List bootClasspath = new ArrayList( getSurefireBootClassPathUrls().size() + getClassPathUrls().size() );

        bootClasspath.addAll( getSurefireBootClassPathUrls() );

        if ( useSystemClassLoader )
        {
            bootClasspath.addAll( getClassPathUrls() );
        }
        return bootClasspath;
    }

    public String getTestClassPathAsString()
    {
        StringBuffer sb = new StringBuffer();
        for ( int i = 0; i < getClassPathUrls().size(); i++ )
        {
            sb.append( getClassPathUrls().get( i ) ).append( File.pathSeparatorChar );
        }
        return sb.toString();
    }

    public ClassLoader createTestClassLoaderConditionallySystem( boolean useSystemClassLoader )
        throws SurefireExecutionException
    {
        return useSystemClassLoader ? ClassLoader.getSystemClassLoader() : createTestClassLoader( this.childDelegation );
    }

    public ClassLoader createTestClassLoader( boolean childDelegation )
        throws SurefireExecutionException
    {
        return createClassLoaderSEE( getClassPathUrls(), null, childDelegation );
    }

    public ClassLoader createTestClassLoader( )
        throws SurefireExecutionException
    {
        return createClassLoaderSEE( getClassPathUrls(), null, this.childDelegation );
    }

    public ClassLoader createSurefireClassLoader( ClassLoader parent )
        throws SurefireExecutionException
    {
        return createClassLoaderSEE( getSurefireClassPathUrls(), parent, false );
    }

    private ClassLoader createClassLoaderSEE( List classPathUrls, ClassLoader parent, boolean childDelegation )
        throws SurefireExecutionException
    {
        try
        {
            return createClassLoader( classPathUrls, parent, childDelegation );
        }
        catch ( MalformedURLException e )
        {
            throw new SurefireExecutionException( "When creating classloader", e );
        }

    }

    private ClassLoader createClassLoader( List classPathUrls, ClassLoader parent, boolean childDelegation )
        throws MalformedURLException
    {
        List urls = new ArrayList();

        for ( Iterator i = classPathUrls.iterator(); i.hasNext(); )
        {
            String url = (String) i.next();

            if ( url != null )
            {
                File f = new File( url );
                urls.add( UrlUtils.getURL( f ) );
            }
        }

        IsolatedClassLoader classLoader = new IsolatedClassLoader( parent, childDelegation );
        if ( assertionStatusMethod != null )
        {
            try
            {
                Object[] args = new Object[]{ enableAssertions ? Boolean.TRUE : Boolean.FALSE };
                if ( parent != null )
                {
                    assertionStatusMethod.invoke( parent, args );
                }
                assertionStatusMethod.invoke( classLoader, args );
            }
            catch ( IllegalAccessException e )
            {
                throw new NestedRuntimeException( "Unable to access the assertion enablement method", e );
            }
            catch ( InvocationTargetException e )
            {
                throw new NestedRuntimeException( "Unable to invoke the assertion enablement method", e );
            }
        }
        for ( Iterator iter = urls.iterator(); iter.hasNext(); )
        {
            URL url = (URL) iter.next();
            classLoader.addURL( url );
        }
        return classLoader;
    }

    public List getSurefireClassPathUrls()
    {
        return surefireClassPathUrls;
    }

    public List getSurefireBootClassPathUrls()
    {
        return surefireBootClassPathUrls;
    }

    public void addClassPathUrl( String path )
    {
        if ( !classPathUrls.contains( path ) )
        {
            classPathUrls.add( path );
        }
    }

    public void addSurefireClassPathUrl( String path )
    {
        if ( !surefireClassPathUrls.contains( path ) )
        {
            surefireClassPathUrls.add( path );
        }
    }

    public void addSurefireBootClassPathUrl( String path )
    {
        if ( !surefireBootClassPathUrls.contains( path ) )
        {
            surefireBootClassPathUrls.add( path );
        }
    }

    public List getClassPathUrls()
    {
        return classPathUrls;
    }
}
