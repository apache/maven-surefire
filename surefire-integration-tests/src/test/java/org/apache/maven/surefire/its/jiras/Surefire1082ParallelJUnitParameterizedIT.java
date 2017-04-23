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
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @see <a href="https://issues.apache.org/jira/browse/SUREFIRE-1082">SUREFIRE-1082</a>
 * @since 2.18
 */
public class Surefire1082ParallelJUnitParameterizedIT
    extends SurefireJUnit4IntegrationTestCase
{

    private static Set<String> printOnlyTestLines( OutputValidator validator )
        throws VerificationException
    {
        Set<String> log = new TreeSet<String>( validator.loadLogLines() );
        for ( Iterator<String> it = log.iterator(); it.hasNext(); )
        {
            String line = it.next();
            if ( !line.startsWith( "class jiras.surefire1082." ) )
            {
                it.remove();
            }
        }
        return log;
    }

    private static Matcher<Set<String>> regex( Set<String> r )
    {
        return new IsRegex( r );
    }

    @Test
    public void test()
        throws VerificationException
    {
        OutputValidator validator = unpack().setTestToRun(
            "Jira1082Test" ).parallelClasses().useUnlimitedThreads().executeTest().verifyErrorFree( 4 );

        Set<String> log = printOnlyTestLines( validator );
        assertThat( log.size(), is( 4 ) );

        Set<String> expectedLogs1 = new TreeSet<String>();
        expectedLogs1.add( "class jiras.surefire1082.Jira1082Test a 0 pool-[\\d]+-thread-1" );
        expectedLogs1.add( "class jiras.surefire1082.Jira1082Test b 0 pool-[\\d]+-thread-1" );
        expectedLogs1.add( "class jiras.surefire1082.Jira1082Test a 1 pool-[\\d]+-thread-2" );
        expectedLogs1.add( "class jiras.surefire1082.Jira1082Test b 1 pool-[\\d]+-thread-2" );

        Set<String> expectedLogs2 = new TreeSet<String>();
        expectedLogs2.add( "class jiras.surefire1082.Jira1082Test a 1 pool-[\\d]+-thread-1" );
        expectedLogs2.add( "class jiras.surefire1082.Jira1082Test b 1 pool-[\\d]+-thread-1" );
        expectedLogs2.add( "class jiras.surefire1082.Jira1082Test a 0 pool-[\\d]+-thread-2" );
        expectedLogs2.add( "class jiras.surefire1082.Jira1082Test b 0 pool-[\\d]+-thread-2" );

        assertThat( log, anyOf( regex( expectedLogs1 ), regex( expectedLogs2 ) ) );
    }

    private SurefireLauncher unpack()
    {
        return unpack( "surefire-1082-parallel-junit-parameterized" );
    }

    private static class IsRegex
        extends BaseMatcher<Set<String>>
    {
        private final Set<String> expectedRegex;

        IsRegex( Set<String> expectedRegex )
        {
            this.expectedRegex = expectedRegex;
        }

        @Override
        public boolean matches( Object o )
        {
            if ( o != null && o instanceof Set )
            {
                Set<String> actual = (Set<String>) o;
                boolean matches = actual.size() == expectedRegex.size();
                Iterator<String> regex = expectedRegex.iterator();
                for ( String s : actual )
                {
                    if ( s == null || !regex.hasNext() || !s.matches( regex.next() ) )
                    {
                        matches = false;
                    }
                }
                return matches;
            }
            else
            {
                return false;
            }
        }

        @Override
        public void describeTo( Description description )
        {
            description.appendValue( expectedRegex );
        }
    }
}
