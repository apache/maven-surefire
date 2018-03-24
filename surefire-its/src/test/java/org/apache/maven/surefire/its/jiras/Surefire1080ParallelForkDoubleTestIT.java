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
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

/**
 * Description of SUREFIRE-1080: <br>
 * <br>
 * There are 9 tests in total in the attached project, and mvn test will show 9 tests run.
 * When I use the command " mvn test -Dparallel=classes -DforkCount=2 -DuseUnlimitedThreads=true", it shows 13 tests
 * run (and sometimes 16), and some tests are run more than once.
 * If I remove forkCount, or parallel, everything will be fine. But it is problematic when combining together.
 * Apache Maven 3.2.2-SNAPSHOT
 * Surefire 2.18-SNAPSHOT
 * JUnit 4.11
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @see <a href="https://issues.apache.org/jira/browse/SUREFIRE-1080">SUREFIRE-1080</a>
 * @since 2.18
 */
public class Surefire1080ParallelForkDoubleTestIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void test()
    {
        unpack().executeTest().assertTestSuiteResults( 9 );
    }

    private SurefireLauncher unpack()
    {
        return unpack( "surefire-1080-parallel-fork-double-test" );
    }
}
