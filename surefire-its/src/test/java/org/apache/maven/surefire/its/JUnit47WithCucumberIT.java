package org.apache.maven.surefire.its;

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
 * Tests the JUnit 47 provider with the cucumber runner. At the moment, they don't play along that perfectly (minor
 * glitches in the reports with parallel=classes), but at least all tests are executed, the execution times are counted
 * correctly and failing tests are reported. The main problem that the junit47 provider has with the cucumber runner is
 * that the junit Description instance created by the runner has a null test class attribute.
 * 
 * @author agudian
 */
@SuppressWarnings( "checkstyle:magicnumber" )
public class JUnit47WithCucumberIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void testWithoutParallel()
    {
        doTest( "none" );
    }

    @Test
    public void testWithParallelClasses()
    {
        doTest( "classes" );
    }

    private void doTest( String parallel )
    {
        unpack( "junit47-cucumber" )
                .parallel( parallel )
                .threadCount( 2 )
                .executeTest()
                .assertTestSuiteResults( 2, 0, 1, 0 );
    }
}
