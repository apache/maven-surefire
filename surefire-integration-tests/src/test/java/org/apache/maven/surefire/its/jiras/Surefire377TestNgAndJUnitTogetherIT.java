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
 * SUREFIRE-377 (When JUnit and TestNG tests are in same project, only one set gets run).
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 */
public class Surefire377TestNgAndJUnitTogetherIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void testTestNgAndJUnitTogether()
    {
        unpack( "/testng-junit-together" )
                .sysProp( "testNgVersion", "5.7" )
                .sysProp( "testNgClassifier", "jdk15" )
                .executeTest()
                .verifyErrorFree( 2 );
    }

    @Test
    public void testTestNgAndJUnit4Together()
    {
        unpack( "/testng-junit4-together" )
                .sysProp( "testNgVersion", "5.7" )
                .sysProp( "testNgClassifier", "jdk15" )
                .executeTest()
                .verifyErrorFree( 3 );
    }
}
