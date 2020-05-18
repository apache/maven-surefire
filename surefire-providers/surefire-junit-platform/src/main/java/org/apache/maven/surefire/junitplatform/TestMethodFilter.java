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
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.PostDiscoveryFilter;

/**
 * @since 2.22.0
 */
class TestMethodFilter
    implements PostDiscoveryFilter
{

    private final TestListResolver testListResolver;

    TestMethodFilter( TestListResolver testListResolver )
    {
        this.testListResolver = testListResolver;
    }

    @Override
    public FilterResult apply( TestDescriptor descriptor )
    {
        boolean shouldRun = descriptor.getSource()
                                      .filter( MethodSource.class::isInstance )
                                      .map( MethodSource.class::cast )
                                      .map( this::shouldRun )
                                      .orElse( true );

        return FilterResult.includedIf( shouldRun );
    }

    private boolean shouldRun( MethodSource source )
    {
        String testClass = TestListResolver.toClassFileName( source.getClassName() );
        String testMethod = source.getMethodName();
        return this.testListResolver.shouldRun( testClass, testMethod );
    }
}
