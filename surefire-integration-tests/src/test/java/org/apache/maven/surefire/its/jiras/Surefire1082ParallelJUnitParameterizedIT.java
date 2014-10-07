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

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="mailto:tibor.digana@gmail.com">Tibor Digana (tibor17)</a>
 * @see {@linkplain https://jira.codehaus.org/browse/SUREFIRE-1082}
 * @since 2.18
 */
public class Surefire1082ParallelJUnitParameterizedIT
    extends SurefireJUnit4IntegrationTestCase
{

    @Test
    public void test()
        throws VerificationException
    {
        OutputValidator validator = unpack().setTestToRun(
            "Jira1082Test" ).parallelClasses().useUnlimitedThreads().executeTest().verifyErrorFree( 4 );

        Set<String> log = printOnlyTestLines( validator );
        assertThat( log.size(), is( 4 ) );

        Set<String> expectedLogs1 = new TreeSet<String>();
        expectedLogs1.add( "class jiras.surefire1082.Jira1082Test a 0 pool-1-thread-1" );
        expectedLogs1.add( "class jiras.surefire1082.Jira1082Test b 0 pool-1-thread-1" );
        expectedLogs1.add( "class jiras.surefire1082.Jira1082Test a 1 pool-1-thread-2" );
        expectedLogs1.add( "class jiras.surefire1082.Jira1082Test b 1 pool-1-thread-2" );

        Set<String> expectedLogs2 = new TreeSet<String>();
        expectedLogs2.add( "class jiras.surefire1082.Jira1082Test a 1 pool-1-thread-1" );
        expectedLogs2.add( "class jiras.surefire1082.Jira1082Test b 1 pool-1-thread-1" );
        expectedLogs2.add( "class jiras.surefire1082.Jira1082Test a 0 pool-1-thread-2" );
        expectedLogs2.add( "class jiras.surefire1082.Jira1082Test b 0 pool-1-thread-2" );

        assertThat( log, anyOf( is( expectedLogs1 ), is( expectedLogs2 ) ) );
    }

    private SurefireLauncher unpack()
    {
        return unpack( "surefire-1082-parallel-junit-parameterized" );
    }

    private static Set<String> printOnlyTestLines( OutputValidator validator )
        throws VerificationException
    {
        Set<String> log = new TreeSet<String>( validator.loadLogLines() );
        for ( Iterator<String> it = log.iterator(); it.hasNext(); ) {
            String line = it.next();
            if ( !line.startsWith( "class jiras.surefire1082." ) ) {
                it.remove();
            }
        }
        return log;
    }
}