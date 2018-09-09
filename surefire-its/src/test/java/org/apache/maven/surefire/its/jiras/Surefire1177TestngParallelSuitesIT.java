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
import org.apache.maven.surefire.its.fixture.OutputValidator;
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
    private static final String EXPECTED_LINE = "TestNGSuiteTest#shouldRunAndPrintItself()";
    private static final String UNEXPECTED_LINE = "ShouldNotRunTest#shouldNotRun()";

    @Test
    public void twoSuitesInParallel()
        throws VerificationException
    {
        assumeJavaVersion( 1.7d );

        OutputValidator validator = unpack()
                .forkMode( "never" )
                .executeTest()
                .verifyErrorFree( 2 );

        validator.assertThatLogLine( startsWith( EXPECTED_LINE ), is( 2 ) );
        validator.assertThatLogLine( is( EXPECTED_LINE + " 1." ), is( 1 ) );
        validator.assertThatLogLine( is( EXPECTED_LINE + " 2." ), is( 1 ) );
        validator.assertThatLogLine( containsString( UNEXPECTED_LINE ), is( 0 ) );
    }

    @Test
    public void twoSuitesInParallelForked()
            throws VerificationException
    {
        assumeJavaVersion( 1.7d );

        OutputValidator validator = unpack()
                .forkMode( "once" )
                .executeTest()
                .verifyErrorFree( 2 );

        validator.assertThatLogLine( startsWith( EXPECTED_LINE ), is( 2 ) );
        validator.assertThatLogLine( is( EXPECTED_LINE + " 1." ), is( 1 ) );
        validator.assertThatLogLine( is( EXPECTED_LINE + " 2." ), is( 1 ) );
        validator.assertThatLogLine( containsString( UNEXPECTED_LINE ), is( 0 ) );
    }

    private SurefireLauncher unpack()
    {
        return unpack( "testng-parallel-suites" );
    }
}
