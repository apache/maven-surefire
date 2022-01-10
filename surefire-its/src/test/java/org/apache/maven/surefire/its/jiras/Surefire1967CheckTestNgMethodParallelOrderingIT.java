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

import static org.apache.maven.surefire.its.fixture.HelperAssertions.assumeJavaMaxVersion;

/**
 * Test TestNG setup and teardown ordering with parallelism
 *
 * @author findepi
 */
public class Surefire1967CheckTestNgMethodParallelOrderingIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void testNgParallelOrdering()
    {
        unpack( "surefire-1967-testng-method-parallel-ordering" )
                .sysProp( "testNgVersion", "7.3.0" )
                .executeTest()
                .verifyErrorFree( 12 );
    }

    // Since the test ordering guarantees currently depend on reflection, it's useful to test with
    // some older version too.
    @Test
    public void testNgParallelOrderingWithVersion6()
    {
        unpack( "surefire-1967-testng-method-parallel-ordering" )
                .sysProp( "testNgVersion", "6.10" )
                .executeTest()
                .verifyErrorFree( 12 );
    }

    // TestNG 6.2.1 is the newest version that doesn't have XmlClass.setIndex method yet.
    // Note that the problem of wrong setup methods ordering (SUREFIRE-1967) was not observed on that version.
    // This is likely because SUREFIRE-1967 is related to a change in TestNG 6.3, where preserve-order became true by
    // default (https://github.com/cbeust/testng/commit/8849b3406ef2184ceb6002768a2d087d7a8de8d5).
    @Test
    public void testNgParallelOrderingWithEarlyVersion6()
    {
        unpack( "surefire-1967-testng-method-parallel-ordering" )
                .sysProp( "testNgVersion", "6.2.1" )
                .executeTest()
                .verifyErrorFree( 12 );
    }

    // TestNG 5.13+ already has XmlClass.m_index field, but doesn't have XmlClass.setIndex method.
    // Note that the problem of wrong setup methods ordering (SUREFIRE-1967) was not observed on that version.
    // This is likely because SUREFIRE-1967 is related to a change in TestNG 6.3, where preserve-order became true by
    // default (https://github.com/cbeust/testng/commit/8849b3406ef2184ceb6002768a2d087d7a8de8d5).
    @Test
    public void testNgParallelOrderingWithVersion5()
    {
        // TestNG 5.13 does not work with Java 17
        assumeJavaMaxVersion( 16 );

        unpack( "surefire-1967-testng-method-parallel-ordering" )
                .sysProp( "testNgVersion", "5.13" )
                .executeTest()
                .verifyErrorFree( 12 );
    }
}
