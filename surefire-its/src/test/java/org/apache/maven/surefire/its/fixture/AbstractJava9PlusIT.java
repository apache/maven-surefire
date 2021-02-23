package org.apache.maven.surefire.its.fixture;

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

import java.io.IOException;

import static org.junit.Assume.assumeTrue;

/**
 * Abstract test class for Jigsaw tests.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
 */
public abstract class AbstractJava9PlusIT
        extends SurefireJUnit4IntegrationTestCase
{
    protected abstract String getProjectDirectoryName();

    protected SurefireLauncher assumeJava9() throws IOException
    {
        assumeTrue( "There's no JDK 9 provided.", IS_JAVA9_PLUS );
        return unpack();
    }

    protected String getSuffix()
    {
        return null;
    }

    @Override
    public final SurefireLauncher unpack( String sourceName )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public final SurefireLauncher unpack( String sourceName, String suffix )
    {
        throw new UnsupportedOperationException();
    }

    private SurefireLauncher unpack()
    {
        return unpack( getClass(), getProjectDirectoryName(), getSuffix() );
    }
}
