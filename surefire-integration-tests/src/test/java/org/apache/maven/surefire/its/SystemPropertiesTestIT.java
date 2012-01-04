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

import org.apache.maven.surefire.its.fixture.SurefireVerifierTestClass;

/**
 * Test system properties
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 */
public class SystemPropertiesTestIT
    extends SurefireVerifierTestClass
{

    public SystemPropertiesTestIT()
    {
        super( "/system-properties" );
    }

    public void testSystemProperties()
        throws Exception
    {
        addGoal( "-DsetOnMavenCommandLine=baz" );
        addGoal( "-DsetOnArgLineWorkAround=baz" );
        executeTest();
        verifyErrorFreeLog();
        assertTestSuiteResults( 8, 0, 0, 0 );
    }

    public void testSystemPropertiesNoFork()
        throws Exception
    {
        addGoal( "-DforkMode=never" );
        addGoal( "-DsetOnArgLineWorkAround=baz" );
        addGoal( "-DsetOnMavenCommandLine=baz" );
        // DGF fake the argLine, since we're not forking
        addGoal( "-DsetOnArgLine=bar" );
        executeTest();
        verifyErrorFreeLog();

        assertTestSuiteResults( 8, 0, 0, 0 );
    }
}
