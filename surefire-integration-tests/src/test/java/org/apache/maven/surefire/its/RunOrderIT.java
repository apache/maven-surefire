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

import java.io.IOException;
import java.util.Calendar;
import org.apache.maven.it.VerificationException;
import org.apache.maven.surefire.its.fixture.SurefireVerifierTestClass;

/**
 * Verifies the runOrder setting and its effect
 *
 * @author Kristian Rosenvold
 */
public class RunOrderIT
    extends SurefireVerifierTestClass
{
    private static final String[] TESTS_IN_ALPHABETICAL_ORDER = { "TA", "TB", "TC" };

    private static final String[] TESTS_IN_REVERSE_ALPHABETICAL_ORDER = { "TC", "TB", "TA" };

    // testing random is left as an exercise to the reader. Patches welcome

    public RunOrderIT()
    {
        super( "/runOrder" );
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
        verifyTextInLog( "There's no RunOrder with the name nonExistingRunOrder." );
    }

    private void executeWithRunOrder( String runOrder )
        throws IOException, VerificationException
    {
        executeTestsWithRunOrder( runOrder );
        verifyErrorFreeLog();
        assertTestSuiteResults( 3, 0, 0, 0);
    }


    protected String getForkMode()
    {
        return "once";
    }

    protected void executeTestsWithRunOrder( String runOrder )
        throws VerificationException
    {
        forkMode(  getForkMode() );
        runOrder(  runOrder );
        executeTest();
    }

    private void assertTestnamesAppearInSpecificOrder( String[] testnames )
        throws VerificationException
    {
        if ( !stringsAppearInSpecificOrderInLog( testnames ) )
        {
            throw new VerificationException( "Response does not contain expected item" );
        }
    }
}
