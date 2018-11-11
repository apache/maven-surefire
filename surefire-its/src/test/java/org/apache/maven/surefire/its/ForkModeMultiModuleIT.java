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

import org.apache.maven.surefire.its.fixture.*;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test forkMode in a multi module project with parallel maven builds
 * 
 * @author Andreas Gudian
 */
public class ForkModeMultiModuleIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void testForkCountOneNoReuse()
    {
        List<String> pids = doTest( unpack( getProject() ).forkCount( 1 ).reuseForks( false ) );
        assertAllDifferentPids( pids );
        int matchesOne = countSuffixMatches( pids, "_1_1");
        int matchesTwo = countSuffixMatches( pids, "_2_2" );
        assertTrue( "At least one fork had forkNumber 1", matchesOne >= 1 );
        assertTrue( "At least one fork had forkNumber 2", matchesTwo >= 1 );
        assertEquals( "No other forkNumbers than 1 and 2 have been used", 6, matchesOne + matchesTwo);
    }


    @Test
    public void testForkCountOneReuse()
    {
        List<String> pids = doTest( unpack( getProject() ).forkCount( 1 ).reuseForks( true ) );
        assertDifferentPids( pids, 2 );
        assertEndWith( pids, "_1_1", 3 );
        assertEndWith( pids, "_2_2", 3 );
    }

    @Test
    public void testForkCountTwoNoReuse()
    {
        List<String> pids = doTest( unpack( getProject() ).forkCount( 2 ).reuseForks( false ) );
        assertAllDifferentPids( pids );
        int matchesOne = countSuffixMatches( pids, "_1_1");
        int matchesTwo = countSuffixMatches( pids, "_2_2" );
        int matchesThree = countSuffixMatches( pids, "_3_3");
        int matchesFour = countSuffixMatches( pids, "_4_4" );
        assertTrue( "At least one fork had forkNumber 1", matchesOne >= 1 );
        assertTrue( "At least one fork had forkNumber 2", matchesTwo >= 1 );
        assertTrue( "At least one fork had forkNumber 3", matchesThree >= 1 );
        assertTrue( "At least one fork had forkNumber 4", matchesFour >= 1 );
        assertEquals( "No other forkNumbers than 1, 2, 3, or 4 have been used", 6, matchesOne + matchesTwo + matchesThree + matchesFour );
    }

    @Test
    public void testForkCountTwoReuse()
    {
        List<String> pids =
            doTest( unpack( getProject() ).forkCount( 2 ).reuseForks( true ) );
        assertDifferentPids( pids, 4 );
        
        int matchesOne = countSuffixMatches( pids, "_1_1");
        int matchesTwo = countSuffixMatches( pids, "_2_2" );
        int matchesThree = countSuffixMatches( pids, "_3_3");
        int matchesFour = countSuffixMatches( pids, "_4_4" );
        assertTrue( "At least one fork had forkNumber 1", matchesOne >= 1 );
        assertTrue( "At least one fork had forkNumber 2", matchesTwo >= 1 );
        assertTrue( "At least one fork had forkNumber 3", matchesThree >= 1 );
        assertTrue( "At least one fork had forkNumber 4", matchesFour >= 1 );
        assertEquals( "No other forkNumbers than 1, 2, 3, or 4 have been used", 6, matchesOne + matchesTwo + matchesThree + matchesFour );
    }

    private void assertEndWith( List<String> pids, String suffix, int expectedMatches )
    {
        int matches = countSuffixMatches( pids, suffix );

        assertEquals( "suffix " + suffix + " matched the correct number of pids", expectedMatches, matches );
    }

    private int countSuffixMatches( List<String> pids, String suffix )
    {
        int matches = 0;
        for ( String pid : pids )
        {
            if ( pid.endsWith( suffix ) )
            {
                matches++;
            }
        }
        return matches;
    }

    private void assertDifferentPids( List<String> pids, int numOfDifferentPids )
    {
        Set<String> pidSet = new HashSet<>( pids );
        assertEquals( "number of different pids is not as expected", numOfDifferentPids, pidSet.size() );
    }

    private void assertAllDifferentPids( List<String> pids )
    {
        assertDifferentPids( pids, pids.size() );
    }

    private List<String> doTest( SurefireLauncher forkMode )
    {
        forkMode.addGoal( "-T2" );
        forkMode.sysProp( "testProperty", "testValue_${surefire.threadNumber}_${surefire.forkNumber}" );
        final OutputValidator outputValidator = forkMode.setForkJvm().executeTest();
        List<String> pids = new ArrayList<>( 6 );
        pids.addAll( validateModule( outputValidator, "module-a" ) );
        pids.addAll( validateModule( outputValidator, "module-b" ) );

        return pids;
    }

    private List<String> validateModule( OutputValidator outputValidator, String module )
    {
        HelperAssertions.assertTestSuiteResults( 3, 0, 0, 0, new File( outputValidator.getBaseDir(), module ) );

        List<String> pids = new ArrayList<>( 3 );
        for ( int i = 1; i <= 3; i++ )
        {
            final TestFile targetFile = outputValidator.getTargetFile( module, "test" + i + "-pid" );
            String pid = targetFile.slurpFile();
            pids.add( pid );
        }
        
        return pids;
    }

    protected String getProject()
    {
        return "fork-mode-multimodule";
    }


}
