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
import org.apache.maven.surefire.its.fixture.TestFile;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import static org.apache.maven.surefire.its.fixture.IsRegex.regex;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @see <a href="https://issues.apache.org/jira/browse/SUREFIRE-1082">SUREFIRE-1082</a>
 * @since 2.18
 */
public class Surefire1082ParallelJUnitParameterizedIT
    extends SurefireJUnit4IntegrationTestCase
{
    private static Set<String> printOnlyTestLinesFromOutFile( OutputValidator validator )
    {
        TestFile report = validator.getSurefireReportsFile( "jiras.surefire1082.Jira1082Test-output.txt" );
        report.assertFileExists();
        return printOnlyTestLines( validator.loadFile( report.getFile(), Charset.forName( "UTF-8" ) ) );
    }

    private static Set<String> printOnlyTestLines( Collection<String> logs )
    {
        Set<String> log = new TreeSet<String>();
        for ( String line : logs )
        {
            if ( line.startsWith( "class jiras.surefire1082." ) )
            {
                log.add( line );
            }
        }
        return log;
    }

    private static void assertParallelRun( Set<String> log )
    {
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

    @Test
    public void checkClassesRunParallel()
        throws VerificationException
    {
        OutputValidator validator = unpack().setTestToRun( "Jira1082Test" )
                                            .parallelClasses()
                                            .useUnlimitedThreads()
                                            .executeTest()
                                            .verifyErrorFree( 4 );

        validator.getSurefireReportsXmlFile( "TEST-jiras.surefire1082.Jira1082Test.xml" )
                .assertFileExists();

        validator.assertThatLogLine( containsString( "Running jiras.surefire1082.Jira1082Test" ), is( 1 ) );

        Set<String> log = new TreeSet<String>( validator.loadLogLines( startsWith( "class jiras.surefire1082." ) ) );
        assertParallelRun( log );
    }

    @Test
    public void checkOutFileClassesRunParallel()
            throws VerificationException
    {
        OutputValidator validator = unpack().redirectToFile( true )
                                            .setTestToRun( "Jira1082Test" )
                                            .parallelClasses()
                                            .useUnlimitedThreads()
                                            .executeTest()
                                            .verifyErrorFree( 4 );

        validator.getSurefireReportsXmlFile( "TEST-jiras.surefire1082.Jira1082Test.xml" )
                .assertFileExists();

        validator.assertThatLogLine( containsString( "Running jiras.surefire1082.Jira1082Test" ), is( 1 ) );

        Set<String> log = printOnlyTestLinesFromOutFile( validator );
        assertParallelRun( log );
    }

    @Test
    public void shouldRunTwo() throws VerificationException
    {
        OutputValidator validator = unpack().redirectToFile( true )
                                            .parallelClasses()
                                            .useUnlimitedThreads()
                                            .executeTest()
                                            .verifyErrorFree( 8 );

        validator.getSurefireReportsXmlFile( "TEST-jiras.surefire1082.Jira1082Test.xml" )
                .assertFileExists();

        validator.getSurefireReportsXmlFile( "TEST-jiras.surefire1082.Jira1264Test.xml" )
                .assertFileExists();

        validator.getSurefireReportsFile( "jiras.surefire1082.Jira1082Test-output.txt" )
                .assertFileExists();

        validator.getSurefireReportsFile( "jiras.surefire1082.Jira1264Test-output.txt" )
                .assertFileExists();

        validator.assertThatLogLine( containsString( "Running jiras.surefire1082.Jira1082Test" ), is( 1 ) );

        validator.assertThatLogLine( containsString( "Running jiras.surefire1082.Jira1264Test" ), is( 1 ) );
    }

    private SurefireLauncher unpack()
    {
        return unpack( "surefire-1082-parallel-junit-parameterized" );
    }
}
