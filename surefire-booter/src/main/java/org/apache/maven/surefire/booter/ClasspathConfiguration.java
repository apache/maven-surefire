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
 */
public class ClasspathConfiguration
{
    public static final String CHILD_DELEGATION = "childDelegation";

    public static final String ENABLE_ASSERTIONS = "enableAssertions";

    public static final String CLASSPATH = "classPathUrl.";

    public static final String SUREFIRE_CLASSPATH = "surefireClassPathUrl.";

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
        this( Classpath.emptyClasspath(), Classpath.emptyClasspath(), Classpath.emptyClasspath(), enableAssertions, childDelegation );
    }

    ClasspathConfiguration( PropertiesWrapper properties )
    {
        this( properties.getClasspath( CLASSPATH ), properties.getClasspath( SUREFIRE_CLASSPATH ),
              Classpath.emptyClasspath(),
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

    public ClassLoader createMergedClassLoader()
        throws SurefireExecutionException
    {
        return Classpath.join( inprocClasspath, classpathUrls)
            .createClassLoader( null, this.childDelegation, enableAssertions, "test" );
    }

    public Classpath getProviderClasspath()
    {
        return surefireClasspathUrls;
    }


        public Classpath getTestClasspath()
    {
        return classpathUrls;
    }

    public void trickClassPathWhenManifestOnlyClasspath()
        throws SurefireExecutionException
    {
            System.setProperty( "surefire.real.class.path", System.getProperty( "java.class.path" ) );
            getTestClasspath().writeToSystemProperty( "java.class.path" );
    }

    public boolean isEnableAssertions()
    {
        return enableAssertions;
    }

    public boolean isChildDelegation()
    {
        return childDelegation;
    }
}
