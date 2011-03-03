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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

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
    private static final String CHILD_DELEGATION = "childDelegation";

    private static final String ENABLE_ASSERTIONS = "enableAssertions";

    private static final String CLASSPATH = "classPathUrl.";

    private static final String SUREFIRE_CLASSPATH = "surefireClassPathUrl.";

    private final Classpath classpathUrls;

    private final Classpath surefireClasspathUrls;

    /**
     * Whether to enable assertions or not (can be affected by the fork arguments, and the ability to do so based on the
     * JVM).
     */
    private final boolean enableAssertions;

    // todo: @deprecated because the IsolatedClassLoader is really isolated - no parent.
    private final boolean childDelegation;

    public ClasspathConfiguration( boolean enableAssertions, boolean childDelegation )
    {
        this( new Classpath(), new Classpath(), enableAssertions, childDelegation );
    }


    ClasspathConfiguration( PropertiesWrapper properties )
    {
        this( properties.getClasspath( CLASSPATH ),
              properties.getClasspath( SUREFIRE_CLASSPATH ),
              properties.getBooleanProperty( ENABLE_ASSERTIONS ), properties.getBooleanProperty( CHILD_DELEGATION ) );
    }

    public ClasspathConfiguration( Classpath testClasspath, Classpath surefireClassPathUrls, boolean enableAssertions,
                                    boolean childDelegation )
    {
        this.enableAssertions = enableAssertions;
        this.childDelegation = childDelegation;
        this.classpathUrls = testClasspath;
        this.surefireClasspathUrls = surefireClassPathUrls;
    }

    public void setForkProperties( PropertiesWrapper properties )
    {
        properties.setClasspath( CLASSPATH, classpathUrls );
        properties.setClasspath( SUREFIRE_CLASSPATH, surefireClasspathUrls );
        properties.setProperty( ENABLE_ASSERTIONS, String.valueOf( enableAssertions ) );
        properties.setProperty( CHILD_DELEGATION, String.valueOf( childDelegation ) );
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

    public ClassLoader createTestClassLoaderConditionallySystem( boolean useSystemClassLoader )
        throws SurefireExecutionException
    {
        return useSystemClassLoader
            ? ClassLoader.getSystemClassLoader()
            : createTestClassLoader( this.childDelegation );
    }

    public ClassLoader createTestClassLoader( boolean childDelegation )
        throws SurefireExecutionException
    {
        return createClassLoaderSEE( classpathUrls, null, childDelegation );
    }

    public ClassLoader createTestClassLoader()
        throws SurefireExecutionException
    {
        return createClassLoaderSEE( classpathUrls, null, this.childDelegation );
    }

    public ClassLoader createSurefireClassLoader( ClassLoader parent )
        throws SurefireExecutionException
    {
        return createClassLoaderSEE( surefireClasspathUrls, parent, false );
    }

    private ClassLoader createClassLoaderSEE( Classpath classPathUrls, ClassLoader parent, boolean childDelegation )
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

    private ClassLoader createClassLoader( Classpath classPathUrls, ClassLoader parent, boolean childDelegation )
        throws MalformedURLException
    {
        List urls = classPathUrls.getAsUrlList();
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

    public Classpath getTestClasspath()
    {
        return classpathUrls;
    }

    public void addClasspathUrl( String path )
    {
        classpathUrls.addClassPathElementUrl( path );
    }

    public void addSurefireClasspathUrl( String path )
    {
        surefireClasspathUrls.addClassPathElementUrl( path );
    }
}
