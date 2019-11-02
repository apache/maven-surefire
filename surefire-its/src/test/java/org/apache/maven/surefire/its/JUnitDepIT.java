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
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

/**
 * Test project using JUnit4.4 -dep.  junit-dep includes only junit.* classes, and depends explicitly on hamcrest-core
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 */
public class JUnitDepIT extends SurefireJUnit4IntegrationTestCase
{
    public SurefireLauncher unpack()
    {
        return unpack( "/junit44-dep" );
    }

    @Test
    public void testJUnit44Dep() throws Exception
    {
        unpack().debugLogging().sysProp( "junit-dep.version", "4.4" ).executeTest().verifyErrorFree(
                1 ).verifyTextInLog( "surefire-junit4" ); // Ahem. Will match on the 4.7 provider too
    }

    @Test
    public void testJUnit44DepWithSneaky381() throws Exception
    {
        unpack().debugLogging().sysProp( "junit-dep.version", "4.4" ).activateProfile(
                "provided381" ).executeTest().verifyErrorFree( 1 );
    }

    @Test
    public void testJUnit47Dep() throws Exception
    {
        unpack().debugLogging().sysProp( "junit-dep.version", "4.7" ).executeTest().verifyErrorFree(
                1 ).verifyTextInLog( "surefire-junit47" );
    }

    @Test
    public void testJUnit48Dep() throws Exception
    {
        unpack().debugLogging().sysProp( "junit-dep.version", "4.8" ).executeTest().verifyErrorFree(
                1 ).verifyTextInLog( "surefire-junit47" );
    }
}
