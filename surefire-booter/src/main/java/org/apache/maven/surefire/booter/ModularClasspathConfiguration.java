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
public class ModularClasspathConfiguration extends AbstractPathConfiguration
{
    private final ModularClasspath modularClasspath;
    private final Classpath testClasspathUrls;

    public ModularClasspathConfiguration( @Nonnull ModularClasspath modularClasspath,
                                          @Nonnull Classpath testClasspathUrls,
                                          @Nonnull Classpath surefireClasspathUrls,
                                          boolean enableAssertions,
                                          boolean childDelegation )
    {
        super( surefireClasspathUrls, enableAssertions, childDelegation );
        this.modularClasspath = modularClasspath;
        this.testClasspathUrls = testClasspathUrls;
    }

    @Override
    public Classpath getTestClasspath()
    {
        return testClasspathUrls;
    }

    @Override
    public final boolean isModularPathConfig()
    {
        return true;
    }

    @Override
    public final boolean isClassPathConfig()
    {
        return !isModularPathConfig();
    }

    @Override
    public ClassLoader createMergedClassLoader()
        throws SurefireExecutionException
    {
    return join( join( getProviderClasspath(), getTestClasspath() ),
        new Classpath( getModularClasspath().getModulePath() ) )
        .createClassLoader( isChildDelegation(), isEnableAssertions(), "test" );
    }

    public ModularClasspath getModularClasspath()
    {
        return modularClasspath;
    }
}
