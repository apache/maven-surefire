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

import javax.annotation.Nonnull;

import static org.apache.maven.surefire.booter.Classpath.emptyClasspath;

/**
 * Represents the classpaths for the BooterConfiguration.
 * <br>
 *
 * @author Jason van Zyl
 * @author Emmanuel Venisse
 * @author Kristian Rosenvold
 */
public class ClasspathConfiguration extends AbstractPathConfiguration
{
    private final Classpath testClasspathUrls;

    /**
     * The surefire classpath to use when invoking in-process with the plugin
     */
    private final Classpath inprocClasspath;

    public ClasspathConfiguration( boolean enableAssertions, boolean childDelegation )
    {
        this( emptyClasspath(), emptyClasspath(), emptyClasspath(), enableAssertions, childDelegation );
    }

    ClasspathConfiguration( @Nonnull PropertiesWrapper properties )
    {
        this( properties.getClasspath( CLASSPATH ), properties.getClasspath( SUREFIRE_CLASSPATH ), emptyClasspath(),
              properties.getBooleanProperty( ENABLE_ASSERTIONS ), properties.getBooleanProperty( CHILD_DELEGATION ) );
    }

    public ClasspathConfiguration( @Nonnull Classpath testClasspathUrls, @Nonnull Classpath surefireClassPathUrls,
                                   @Nonnull Classpath inprocClasspath, boolean enableAssertions,
                                   boolean childDelegation )
    {
        super( surefireClassPathUrls, enableAssertions, childDelegation );
        this.testClasspathUrls = testClasspathUrls;
        this.inprocClasspath = inprocClasspath;
    }

    @Override
    protected Classpath getInprocClasspath()
    {
        return inprocClasspath;
    }

    public Classpath getTestClasspath()
    {
        return testClasspathUrls;
    }

    @Override
    public final boolean isModularPathConfig()
    {
        return !isClassPathConfig();
    }

    @Override
    public final boolean isClassPathConfig()
    {
        return true;
    }

    public void trickClassPathWhenManifestOnlyClasspath()
    {
        System.setProperty( "surefire.real.class.path", System.getProperty( "java.class.path" ) );
        getTestClasspath().writeToSystemProperty( "java.class.path" );
    }
}
