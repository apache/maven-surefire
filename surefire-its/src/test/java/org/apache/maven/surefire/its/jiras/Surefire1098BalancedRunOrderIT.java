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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.AnyOf.anyOf;

/**
 * The purpose of this IT is to assert that the run order of test classes is according to the settings:<br>
 *
 * runOrder=balanced<br>
 * parallel=classes<br>
 * threadCount=2<br>
 * perCoreThreadCount=false<br>
 * <br>
 * The list of tests should be reordered to (DTest, CTest, BTest, ATest) in the second run.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @see <a href="https://issues.apache.org/jira/browse/SUREFIRE-1098">SUREFIRE-1098</a>
 * @since 2.18
 */
public class Surefire1098BalancedRunOrderIT
    extends SurefireJUnit4IntegrationTestCase
{

    @Test
    public void reorderedParallelClasses()
        throws VerificationException
    {
        SurefireLauncher launcher = unpack();

        launcher
            // .runOrder( "balanced" ) call it in 3.x and remove it in surefire-1098-balanced-runorder/pom.xml
            // as soon as there is prefix available "failsafe" and "surefire" in system property for this parameter.
            .parallelClasses().threadCount( 2 ).disablePerCoreThreadCount()
            .executeTest().verifyErrorFree( 4 );

        OutputValidator validator =
            launcher
                // .runOrder( "balanced" ) call it in 3.x and remove it in surefire-1098-balanced-runorder/pom.xml
                // as soon as there is prefix available "failsafe" and "surefire" in system property for this parameter.
                .parallelClasses().threadCount( 2 ).disablePerCoreThreadCount()
                .executeTest().verifyErrorFree( 4 );

        List<String> log = printOnlyTestLines( validator );
        assertThat( log.size(), is( 4 ) );
        Collections.sort( log );
        final int[] threadPoolIdsOfLongestTest = extractThreadPoolIds( log.get( 3 ) );
        final int pool = threadPoolIdsOfLongestTest[0];
        int thread = threadPoolIdsOfLongestTest[1];
        assertThat( thread, anyOf( is( 1 ), is( 2 ) ) );
        thread = thread == 1 ? 2 : 1;
        // If the longest test class DTest is running in pool-2-thread-1, the others should run in pool-2-thread-2
        // and vice versa.
        assertThat( log.get( 0 ), is( testLine( "A", pool, thread ) ) );
        assertThat( log.get( 1 ), is( testLine( "B", pool, thread ) ) );
        assertThat( log.get( 2 ), is( testLine( "C", pool, thread ) ) );
    }

    private SurefireLauncher unpack()
    {
        return unpack( "surefire-1098-balanced-runorder" );
    }

    private static List<String> printOnlyTestLines( OutputValidator validator )
        throws VerificationException
    {
        List<String> log = new ArrayList<>( validator.loadLogLines() );
        for ( Iterator<String> it = log.iterator(); it.hasNext(); ) {
            String line = it.next();
            if ( !line.startsWith( "class jiras.surefire1098." ) ) {
                it.remove();
            }
        }
        return log;
    }

    private static int[] extractThreadPoolIds(String logLine)
    {
        //Example to parse "class jiras.surefire1098.DTest pool-2-thread-1" into {2, 1}.
        String t = logLine.split( " " )[2];
        String[] ids = t.split( "-" );
        return new int[]{ Integer.parseInt( ids[1] ), Integer.parseInt( ids[3] )};
    }

    private String testLine(String test, int pool, int thread)
    {
        return String.format( "class jiras.surefire1098.%sTest pool-%d-thread-%d", test, pool, thread );
    }
}
