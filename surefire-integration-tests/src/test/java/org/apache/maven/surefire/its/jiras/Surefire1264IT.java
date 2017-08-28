package org.apache.maven.surefire.its.jiras;

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

import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.Test;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @see <a href="https://issues.apache.org/jira/browse/SUREFIRE-1264">SUREFIRE-1264</a>
 * @since 2.20.1
 */
public class Surefire1264IT
        extends SurefireJUnit4IntegrationTestCase
{

    @Test
    public void positiveTests()
    {
        unpack( "surefire-1264" )
                .setForkJvm()
                .parallelAll()
                .useUnlimitedThreads()
                .sysProp( "canFail", "false" )
                .executeTest()
                .assertTestSuiteResults( 16, 0, 0, 0 );
    }

    @Test
    public void negativeTests()
    {
        unpack( "surefire-1264" )
                .setForkJvm()
                .parallelAll()
                .useUnlimitedThreads()
                .sysProp( "canFail", "true" )
                .mavenTestFailureIgnore( true )
                .executeTest()
                .assertTestSuiteResults( 16, 0, 16, 0 );
    }
}
