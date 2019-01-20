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

import static org.apache.maven.surefire.booter.Classpath.join;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.21.0.Jigsaw
 */
public abstract class AbstractPathConfiguration
{
    public static final String CHILD_DELEGATION = "childDelegation";

    public static final String ENABLE_ASSERTIONS = "enableAssertions";

    public static final String CLASSPATH = "classPathUrl.";

    public static final String SUREFIRE_CLASSPATH = "surefireClassPathUrl.";

    private final Classpath surefireClasspathUrls;

    /**
     * Whether to enable assertions or not
     * (can be affected by the fork arguments, and the ability to do so based on the JVM).
     */
    private final boolean enableAssertions;

    // todo: @deprecated because the IsolatedClassLoader is really isolated - no parent.
    private final boolean childDelegation;

    protected AbstractPathConfiguration( @Nonnull Classpath surefireClasspathUrls,
                                         boolean enableAssertions, boolean childDelegation )
    {
        if ( isClassPathConfig() == isModularPathConfig() )
        {
            throw new IllegalStateException( "modular path and class path should be exclusive" );
        }
        this.surefireClasspathUrls = surefireClasspathUrls;
        this.enableAssertions = enableAssertions;
        this.childDelegation = childDelegation;
    }

    public abstract Classpath getTestClasspath();

    /**
     * Must be exclusive with {@link #isClassPathConfig()}.
     *
     * @return {@code true} if <tt>this</tt> is {@link ModularClasspathConfiguration}.
     */
    public abstract boolean isModularPathConfig();

    /**
     * Must be exclusive with {@link #isModularPathConfig()}.
     *
     * @return {@code true} if <tt>this</tt> is {@link ClasspathConfiguration}.
     */
    public abstract boolean isClassPathConfig();

    protected abstract Classpath getInprocClasspath();

    public <T extends AbstractPathConfiguration> T toRealPath( Class<T> type )
    {
        if ( isClassPathConfig() && type == ClasspathConfiguration.class
                || isModularPathConfig() && type == ModularClasspathConfiguration.class )
        {
            return type.cast( this );
        }
        throw new IllegalStateException( "no target matched " + type );
    }

    public ClassLoader createMergedClassLoader()
            throws SurefireExecutionException
    {
        return createMergedClassLoader( getInprocTestClasspath() );
    }

    public Classpath getProviderClasspath()
    {
        return surefireClasspathUrls;
    }

    public boolean isEnableAssertions()
    {
        return enableAssertions;
    }

    @Deprecated
    public boolean isChildDelegation()
    {
        return childDelegation;
    }

    final Classpath getInprocTestClasspath()
    {
        return join( getInprocClasspath(), getTestClasspath() );
    }

    final ClassLoader createMergedClassLoader( Classpath cp )
            throws SurefireExecutionException
    {
        return cp.createClassLoader( isChildDelegation(), isEnableAssertions(), "test" );
    }
}
