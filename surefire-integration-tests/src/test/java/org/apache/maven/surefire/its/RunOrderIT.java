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

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.surefire.its.misc.HelperAssertions;

/**
 * Verifies the runOrder setting and its effect
 *
 * @author Kristian Rosenvold
 */
public class RunOrderIT
    extends AbstractSurefireIntegrationTestClass
{
    private static final String[] TESTS_IN_ALPHABETICAL_ORDER = { "TA", "TB", "TC" };

    private static final String[] TESTS_IN_REVERSE_ALPHABETICAL_ORDER = { "TC", "TB", "TA" };

    // testing random is left as an exercise to the reader. Patches welcome

    private File testDir;

    protected Verifier verifier;

    public void setUp()
        throws IOException, VerificationException
    {
        testDir = ResourceExtractor.simpleExtractResources( getClass(), "/runOrder" );
        verifier = new Verifier( testDir.getAbsolutePath() );
    }

    public void tearDown()
        throws Exception
    {
        verifier.resetStreams();
    }

    public void testAlphabetical()
        throws Exception
    {
        executeWithRunOrder( "alphabetical" );
        assertTestnamesAppearInSpecificOrder( TESTS_IN_ALPHABETICAL_ORDER );
    }

    public void testReverseAlphabetical()
        throws Exception
    {
        executeWithRunOrder( "reversealphabetical" );
        assertTestnamesAppearInSpecificOrder( TESTS_IN_REVERSE_ALPHABETICAL_ORDER );
    }

    public void testHourly()
        throws Exception
    {
        int startHour = Calendar.getInstance().get( Calendar.HOUR_OF_DAY );
        executeWithRunOrder( "hourly" );
        int endHour = Calendar.getInstance().get( Calendar.HOUR_OF_DAY );
        if ( startHour != endHour )
        {
            return; // Race condition, cannot test when hour changed mid-run
        }

        String[] testnames =
            ( ( startHour % 2 ) == 0 ) ? TESTS_IN_ALPHABETICAL_ORDER : TESTS_IN_REVERSE_ALPHABETICAL_ORDER;
        assertTestnamesAppearInSpecificOrder( testnames );
    }

    public void testNonExistingRunOrder()
        throws Exception
    {
        try
        {
            executeTestsWithRunOrder( "nonExistingRunOrder" );
        }
        catch ( VerificationException e )
        {
        }
        verifier.verifyTextInLog( "There's no RunOrder with the name nonExistingRunOrder." );
    }

    private void executeWithRunOrder( String runOrder )
        throws IOException, VerificationException
    {
        executeTestsWithRunOrder( runOrder );
        verifier.verifyErrorFreeLog();
        HelperAssertions.assertTestSuiteResults( 3, 0, 0, 0, testDir );
    }


    protected String getForkMode()
    {
        return "once";
    }

    protected void executeTestsWithRunOrder( String runOrder )
        throws VerificationException
    {
        List<String> goals = getInitialGoals();
        goals.add( "-DforkMode=" + getForkMode() );
        goals.add( "-DrunOrder=" + runOrder );
        goals.add( "test" );
        executeGoals( verifier, goals );
    }

    private void assertTestnamesAppearInSpecificOrder( String[] testnames )
        throws VerificationException
    {
        if ( !testnamesAppearInSpecificOrder( testnames ) )
        {
            throw new VerificationException( "Response does not contain expected item" );
        }
    }

    private boolean testnamesAppearInSpecificOrder( String[] testnames )
        throws VerificationException
    {
        int i = 0;
        for ( String line : getLog() )
        {
            if ( line.startsWith( testnames[i] ) )
            {
                if ( i == testnames.length - 1 )
                {
                    return true;
                }
                ++i;
            }
        }
        return false;
    }

    private List<String> getLog()
        throws VerificationException
    {
        return verifier.loadFile( verifier.getBasedir(), verifier.getLogFileName(), false );
    }
}
