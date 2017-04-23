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
 * Plugin Configuration: parallel=classes
 * <br>
 * With Surefire 2.15
 * {@code $ mvn test -Dtest=MyTest#testFoo}
 * Results:
 * Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
 * <br>
 * With Surefire 2.16
 * {@code $ mvn test -Dtest=MyTest#testFoo}
 * <br>
 * Results:
 * Tests run: 0, Failures: 0, Errors: 0, Skipped: 0
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @see <a href="https://issues.apache.org/jira/browse/SUREFIRE-1028">SUREFIRE-1028</a>
 * @since 2.18
 */
public class Surefire1028UnableToRunSingleIT
    extends SurefireJUnit4IntegrationTestCase
{

    @Test
    public void methodFilteringParallelExecution()
    {
        unpack().setTestToRun( "SomeTest#test" ).parallelClasses().useUnlimitedThreads()
                .executeTest().verifyErrorFree( 1 ).verifyTextInLog( "OK!" );
    }

    private SurefireLauncher unpack()
    {
        return unpack( "surefire-1028-unable-to-run-single-test" );
    }
}
