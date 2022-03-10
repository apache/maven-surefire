package org.apache.maven.surefire.junitplatform;

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

import org.apache.maven.surefire.api.testset.TestListResolver;
import org.junit.platform.engine.TestSource;

import java.util.Objects;

/**
 * A test selector factory used in combination with a {@link org.apache.maven.surefire.api.testset.TestListResolver}
 * to determine whether a given {@link org.junit.platform.engine.TestSource} should be considered for running.
 *
 * <br /><br /><p>This is a service provider interface; clients may provide their own implementations
 * that will be applied along the default {@link MethodSelectorFactory}</p>
 */
public interface TestSelectorFactory
{

    boolean supports( TestSource source );

    String getContainerName( TestSource source );

    String getSelectorName( TestSource source );

    default boolean isClassContainer()
    {
        return true;
    }

    default TestSelector createSelector( TestSource source )
    {
        String containerName = getContainerName( source );
        return new TestSelector(
            isClassContainer() ? TestListResolver.toClassFileName( containerName ) : containerName,
            getSelectorName( source ) );
    }

    /**
     * Represents a single test case selector
     * (e.g. a fully-qualified class name + test-annotated method name)
     */
    class TestSelector
    {
        private final String containerName;
        private final String selectorName;

        public TestSelector( String containerName, String selectorName )
        {
            this.containerName = containerName;
            this.selectorName = selectorName;
        }

        public String getContainerName()
        {
            return containerName;
        }

        public String getSelectorName()
        {
            return selectorName;
        }

        @Override
        public boolean equals( Object o )
        {
            if ( this == o )
            {
                return true;
            }
            if ( o == null || getClass() != o.getClass() )
            {
                return false;
            }
            TestSelector that = ( TestSelector ) o;
            return containerName.equals( that.containerName ) && Objects.equals( selectorName, that.selectorName );
        }

        @Override
        public int hashCode()
        {
            return Objects.hash( containerName, selectorName );
        }
    }
}
