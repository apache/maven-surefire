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
import java.util.Properties;

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

    private final Classpath testFrameworkClasspath;

    private final Classpath testClasspath;

    private final Classpath providerClasspath;

    /**
     * Whether to enable assertions or not (can be affected by the fork arguments, and the ability to do so based on the
     * JVM).
     */
    private final boolean enableAssertions;

    // todo: @deprecated because the IsolatedClassLoader is really isolated - no parent.
    private final boolean childDelegation;

    public ClasspathConfiguration( boolean enableAssertions, boolean childDelegation )
    {
        this( new Classpath(), new Classpath(), new Classpath(), enableAssertions, childDelegation );
    }

    public ClasspathConfiguration( Classpath testClasspath, Classpath providerClasspath,
                                   Classpath testFrameworkClasspath, boolean enableAssertions, boolean childDelegation )
    {
        this.testFrameworkClasspath = testFrameworkClasspath;
        this.testClasspath = testClasspath;
        this.providerClasspath = providerClasspath;
        this.enableAssertions = enableAssertions;
        this.childDelegation = childDelegation;
    }

    public void setForkProperties( Properties properties )
    {
        testClasspath.writeToForkProperties( properties, BooterConstants.CLASSPATH_URL );
        providerClasspath.writeToForkProperties( properties, BooterConstants.SUREFIRE_CLASSPATHURL );
        testFrameworkClasspath.writeToForkProperties( properties, BooterConstants.TEST_FRAMEWORK_CLASSPATHURL );
        properties.setProperty( BooterConstants.ENABLE_ASSERTIONS, String.valueOf( enableAssertions ) );
        properties.setProperty( BooterConstants.CHILD_DELEGATION, String.valueOf( childDelegation ) );
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
        ClassLoader testFrameWorkClassLoader = createTestFrameworkClassLoader( childDelegation );
        return createClassLoader( testClasspath, testFrameWorkClassLoader, childDelegation, "Test" );
    }

    private ClassLoader createTestFrameworkClassLoader( boolean childDelegation )
        throws SurefireExecutionException
    {
        return hasTestFrameworkClasspath() ? createClassLoader( testFrameworkClasspath, null, childDelegation,
                                                                "TestFramework" ) : null;
    }

    public ClassLoader createTestClassLoader()
        throws SurefireExecutionException
    {
        final ClassLoader testFrameworkClassLoader = createTestFrameworkClassLoader( this.childDelegation );
        return createClassLoader( testClasspath, testFrameworkClassLoader, this.childDelegation, "Test" );
    }

    public ClassLoader createTestframeworkClassLoader()
        throws SurefireExecutionException
    {
        return createClassLoader( testFrameworkClasspath, null, this.childDelegation, "Test" );
    }

    public ClassLoader createSurefireClassLoader( ClassLoader parent )
        throws SurefireExecutionException
    {
        boolean useTestClassLoaderAsParent =
            parent.equals( ClassLoader.getSystemClassLoader() ) || !hasTestFrameworkClasspath();

        ClassLoader parentToUse = useTestClassLoaderAsParent ? parent : parent.getParent();

        return createClassLoader( providerClasspath, parentToUse, false, "Provider" );
    }

    private ClassLoader createClassLoader( Classpath classPathUrls, ClassLoader parent, boolean childDelegation,
                                           String description )
        throws SurefireExecutionException
    {
        try
        {
            List urls = classPathUrls.getAsUrlList();
            IsolatedClassLoader classLoader = new IsolatedClassLoader( parent, childDelegation, description );
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
        catch ( MalformedURLException e )
        {
            throw new SurefireExecutionException( "When creating classloader", e );
        }
    }

    public Classpath getTestClasspath()
    {
        return testClasspath;
    }

    public void addClasspathUrl( String path )
    {
        testClasspath.addClassPathElementUrl( path );
    }

    public void addSurefireClasspathUrl( String path )
    {
        providerClasspath.addClassPathElementUrl( path );
    }

    private Classpath getTestFrameworkClasspath()
    {
        return testFrameworkClasspath;
    }

    /**
     * Indicates if there is a test framework classpath present, which triggers
     * the pitchfork classloader configuration.
     *
     * @return True if there is a test framework classpath available
     */
    private boolean hasTestFrameworkClasspath()
    {
        return testFrameworkClasspath.size() > 0;
    }


}
