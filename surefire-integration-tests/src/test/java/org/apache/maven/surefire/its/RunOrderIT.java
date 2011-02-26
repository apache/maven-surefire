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

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.reporting.MavenReportException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

/**
 * Verifies the runOrder setting and its effect
 *
 * @author Kristian Rosenvold
 */
public class RunOrderIT
    extends AbstractSurefireIntegrationTestClass
{
    // testing random is left as an exercise to the reader. Patches welcome

    public void testAlphabetical()
        throws Exception
    {
        checkOrder( "alphabetical", getAlphabetical() );
    }


    public void testReverseAlphabetical()
        throws Exception
    {
        checkOrder( "reversealphabetical", getReverseAlphabetical() );
    }


    public void testHourly()
        throws Exception
    {
        int startHour = Calendar.getInstance().get( Calendar.HOUR_OF_DAY );
        final List<String> actual = executeWithRunOrder( "hourly" );
        int endHour = Calendar.getInstance().get( Calendar.HOUR_OF_DAY );
        if ( startHour != endHour )
        {
            return; // Race condition, cannot test when hour changed mid-run
        }

        List<String> expected = ( ( startHour % 2 ) == 0 ) ? getAlphabetical() : getReverseAlphabetical();
        if ( !contains( actual, expected ) )
        {
            throw new VerificationException( "Response does not contain expected item" );
        }
    }

    private boolean contains( List<String> items, List<String> expected )
    {
        Iterator<String> expectedIterator = expected.iterator();
        String next = (String) expectedIterator.next();
        Iterator<String> content = items.iterator();
        while ( content.hasNext() )
        {
            String line = content.next();
            if ( line.startsWith( next ) )
            {
                if ( !expectedIterator.hasNext() )
                {
                    return true;
                }
                next = expectedIterator.next();
            }
        }
        return content.hasNext();
    }

    private void checkOrder( String alphabetical, List<String> expected )
        throws VerificationException, MavenReportException, IOException
    {
        final List<String> list = executeWithRunOrder( alphabetical );
        if ( !contains( list, expected ) )
        {
            throw new VerificationException( "Response does not contain expected item" );
        }
    }

    private List<String> executeWithRunOrder( String runOrder )
        throws IOException, VerificationException, MavenReportException
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/runOrder" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        List<String> goals = getInitialGoals();
        goals.add( "-DrunOrder=" + runOrder );
        goals.add( "test" );
        this.executeGoals( verifier, goals );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        HelperAssertions.assertTestSuiteResults( 3, 0, 0, 0, testDir );
        return verifier.loadFile( verifier.getBasedir(), verifier.getLogFileName(), false );
    }

    private List<String> getAlphabetical()
    {
        return Arrays.asList( new String[]{ "TA", "TB", "TC" } );
    }

    private List<String> getReverseAlphabetical()
    {
        return Arrays.asList( new String[]{ "TC", "TB", "TA" } );
    }

}
