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
 *
 */
@SuppressWarnings( "checkstyle:magicnumber" )
public class Surefire1881IT extends SurefireJUnit4IntegrationTestCase
{
    @Test( timeout = 30_000L )
    public void testJVMLogging() throws Exception
    {
        // Before SUREFIRE-1881, use of SurefireForkNodeFactory would make the test hang
        // if the JVM would log something to stdOut or stdErr before Surefire was fully initialised
        unpack( "/surefire-1881-jvm-logging" )
            .executeTest()
            .assertTestSuiteResults( 1, 0, 0, 0 );
    }

    @Test( timeout = 30_000L )
    public void testJavaAgent() throws Exception
    {
        // Before SUREFIRE-1881, use of SurefireForkNodeFactory would make the test hang
        // if a Java agent would log something to stdOut or stdErr before Failsafe was fully initialised
        unpack( "/surefire-1881-java-agent" )
            .executeVerify()
            .assertIntegrationTestSuiteResults( 1, 0, 0, 0 );
    }
}
