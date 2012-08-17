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
     * The surefire classpath to use when invoking in-process with the plugin
     */
    private final Classpath inprocClasspath;

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

    ClasspathConfiguration( PropertiesWrapper properties )
    {
        this( properties.getClasspath( CLASSPATH ), properties.getClasspath( SUREFIRE_CLASSPATH ), new Classpath(),
              properties.getBooleanProperty( ENABLE_ASSERTIONS ), properties.getBooleanProperty( CHILD_DELEGATION ) );
    }

    public ClasspathConfiguration( Classpath testClasspath, Classpath surefireClassPathUrls, Classpath inprocClasspath,
                                   boolean enableAssertions, boolean childDelegation )
    {
        this.enableAssertions = enableAssertions;
        this.childDelegation = childDelegation;
        this.inprocClasspath = inprocClasspath;
        this.classpathUrls = testClasspath;
        this.surefireClasspathUrls = surefireClassPathUrls;
    }

    public void addForkProperties( PropertiesWrapper properties )
    {
        properties.setClasspath( CLASSPATH, classpathUrls );
        properties.setClasspath( SUREFIRE_CLASSPATH, surefireClasspathUrls );
        properties.setProperty( ENABLE_ASSERTIONS, String.valueOf( enableAssertions ) );
        properties.setProperty( CHILD_DELEGATION, String.valueOf( childDelegation ) );
    }

    public ClassLoader createTestClassLoader( boolean childDelegation )
        throws SurefireExecutionException
    {
        return classpathUrls.createClassLoader( null, childDelegation, enableAssertions, "test" );
    }

    public ClassLoader createTestClassLoader()
        throws SurefireExecutionException
    {
        return classpathUrls.createClassLoader( null, this.childDelegation, enableAssertions, "test" );
    }

    public ClassLoader createSurefireClassLoader( ClassLoader parent )
        throws SurefireExecutionException
    {
        return surefireClasspathUrls.createClassLoader( parent, false, enableAssertions, "provider" );
    }

    public ClassLoader createInprocSurefireClassLoader( ClassLoader parent )
        throws SurefireExecutionException
    {
        return inprocClasspath.createClassLoader( parent, false, enableAssertions, "provider" );
    }

    public Classpath getTestClasspath()
    {
        return classpathUrls;
    }

    public ClassLoader createForkingTestClassLoader( boolean manifestOnlyJarRequestedAndUsable )
        throws SurefireExecutionException
    {
        if ( manifestOnlyJarRequestedAndUsable )
        {
            System.setProperty( "surefire.real.class.path", System.getProperty( "java.class.path" ) );
            getTestClasspath().writeToSystemProperty( "java.class.path" );
            // this.getClass.getClassLoader() is always loaded in system classloader if forking
            // this.getClass().getClassLoader() is plugin classloder if in-process
            // "this" must refer to a class within the booter module
            return this.getClass().getClassLoader();   // SUREFIRE-459, trick the app under test into thinking its classpath was conventional;
        }
        else
        {
            return createTestClassLoader();
        }
    }
}
