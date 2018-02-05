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
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

/**
 * Verifies the runOrder setting and its effect
 *
 * @author Kristian Rosenvold
 */
public class RunOrderIT
    extends SurefireJUnit4IntegrationTestCase
{
    private static final String[] TESTS_IN_ALPHABETICAL_ORDER = { "TA", "TB", "TC" };

    private static final String[] TESTS_IN_REVERSE_ALPHABETICAL_ORDER = { "TC", "TB", "TA" };

    // testing random is left as an exercise to the reader. Patches welcome

    @Test
    public void testAlphabetical()
        throws Exception
    {
        OutputValidator validator = executeWithRunOrder( "alphabetical" );
        assertTestnamesAppearInSpecificOrder( validator, TESTS_IN_ALPHABETICAL_ORDER );
    }

    @Test
    public void testReverseAlphabetical()
        throws Exception
    {
        OutputValidator validator = executeWithRunOrder( "reversealphabetical" );
        assertTestnamesAppearInSpecificOrder( validator, TESTS_IN_REVERSE_ALPHABETICAL_ORDER );
    }

    @Test
    public void testHourly()
        throws Exception
    {
        int startHour = Calendar.getInstance().get( Calendar.HOUR_OF_DAY );
        OutputValidator validator = executeWithRunOrder( "hourly" );
        int endHour = Calendar.getInstance().get( Calendar.HOUR_OF_DAY );
        if ( startHour != endHour )
        {
            return; // Race condition, cannot test when hour changed mid-run
        }

        String[] testnames =
            ( ( startHour % 2 ) == 0 ) ? TESTS_IN_ALPHABETICAL_ORDER : TESTS_IN_REVERSE_ALPHABETICAL_ORDER;
        assertTestnamesAppearInSpecificOrder( validator, testnames );
    }

    @Test
    public void testNonExistingRunOrder()
        throws Exception
    {
        unpack().forkMode( getForkMode() ).runOrder( "nonExistingRunOrder" ).maven().withFailure().executeTest().verifyTextInLog(
            "There's no RunOrder with the name nonExistingRunOrder." );
    }

    private OutputValidator executeWithRunOrder( String runOrder )
    {
        return unpack().forkMode( getForkMode() ).runOrder( runOrder ).executeTest().verifyErrorFree( 3 );
    }

    protected String getForkMode()
    {
        return "once";
    }

    private SurefireLauncher unpack()
    {
        return unpack( "runOrder" );
    }

    private void assertTestnamesAppearInSpecificOrder( OutputValidator validator, String[] testnames )
        throws VerificationException
    {
        if ( !validator.stringsAppearInSpecificOrderInLog( testnames ) )
        {
            throw new VerificationException( "Response does not contain expected item" );
        }
    }
}
