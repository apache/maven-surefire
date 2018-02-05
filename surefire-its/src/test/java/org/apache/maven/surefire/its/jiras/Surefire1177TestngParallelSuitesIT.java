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

import static org.apache.maven.surefire.its.fixture.HelperAssertions.assumeJavaVersion;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

/**
 * IT for https://issues.apache.org/jira/browse/SUREFIRE-1177
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public class Surefire1177TestngParallelSuitesIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void shouldRunTwoSuitesInParallel()
        throws VerificationException
    {
        assumeJavaVersion( 1.7d );

        unpack().executeTest()
            .verifyErrorFree( 2 )
            .assertThatLogLine( containsString( "ShouldNotRunTest#shouldNotRun()" ), is( 0 ) )
            .assertThatLogLine( startsWith( "TestNGSuiteTest#shouldRunAndPrintItself()" ), is( 2 ) )
            .assertThatLogLine( is( "TestNGSuiteTest#shouldRunAndPrintItself() 1." ), is( 1 ) )
            .assertThatLogLine( is( "TestNGSuiteTest#shouldRunAndPrintItself() 2." ), is( 1 ) );
    }

    private SurefireLauncher unpack()
    {
        return unpack( "testng-parallel-suites" );
    }
}
