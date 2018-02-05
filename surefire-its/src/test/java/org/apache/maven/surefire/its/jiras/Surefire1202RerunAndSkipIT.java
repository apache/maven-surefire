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

import org.apache.maven.it.VerificationException;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

/**
 * Allow rerunFailingTestsCount, skipAfterFailureCount together
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @see <a href="https://issues.apache.org/jira/browse/SUREFIRE-1202">SUREFIRE-1202</a>
 * @since 2.19.1
 */
public class Surefire1202RerunAndSkipIT
        extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void junit47()
            throws VerificationException
    {
        unpack().executeTest()
                .assertTestSuiteResults( 5, 0, 0, 3, 4 );
    }

    @Test
    public void junit4()
            throws VerificationException
    {
        unpack().addGoal( "-Pjunit4" )
                .executeTest()
                .assertTestSuiteResults( 5, 0, 0, 3, 4 );
    }

    private SurefireLauncher unpack()
    {
        return unpack( "surefire-1202-rerun-and-failfast" );
    }
}
