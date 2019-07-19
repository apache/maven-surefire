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
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

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

    private static final String[] TESTS_IN_RANDOM_ORDER_WITH_SEED_123456 = { "TC", "TB", "TA" };

    private static final String[] TESTS_IN_RANDOM_ORDER_WITH_SEED_654321 = { "TA", "TB", "TC" };

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
    public void testRandomWithSeed123456() throws VerificationException
    {
        OutputValidator validator = executeWithRunOrder( "random:123456" );
        assertTestnamesAppearInSpecificOrder( validator, TESTS_IN_RANDOM_ORDER_WITH_SEED_123456 );
        validator.assertThatLogLine(
                containsString( "Tests are randomly ordered. Re-run the same "
                        + "execution order with -Dsurefire.runOrder=random:123456" ),
                equalTo( 2 )
        );
    }

    @Test
    public void testRandomWithSeed654321() throws VerificationException
    {
        OutputValidator validator = executeWithRunOrder( "random:654321" );
        assertTestnamesAppearInSpecificOrder( validator, TESTS_IN_RANDOM_ORDER_WITH_SEED_654321 );
        validator.assertThatLogLine(
                containsString( "Tests are randomly ordered. Re-run the same "
                        + "execution order with -Dsurefire.runOrder=random:654321" ),
                equalTo( 2 )
        );
    }

    @Test
    public void testRandomWithSetInPomAndSeed123456() throws VerificationException
    {
        OutputValidator validator = forkingMode( unpack( "runOrder-random" ) )
                .executeTest()
                .verifyErrorFree( 3 );

        validator.assertThatLogLine(
                containsString( "Tests are randomly ordered. Re-run the same "
                        + "execution order with -Dsurefire.runOrder=random:" ),
                equalTo( 2 )
        );

        validator = forkingMode( unpack( "runOrder-random" ) )
                .runOrder( "random:123456" )
                .executeTest()
                .verifyErrorFree( 3 );
        assertTestnamesAppearInSpecificOrder( validator, TESTS_IN_RANDOM_ORDER_WITH_SEED_123456 );
        validator.assertThatLogLine(
                containsString( "Tests are randomly ordered. Re-run the same "
                        + "execution order with -Dsurefire.runOrder=random:123456" ),
                equalTo( 2 )
        );
    }

    @Test
    public void testNonExistingRunOrder()
    {
        forkingMode( unpack() )
                .runOrder( "nonExistingRunOrder" )
                .maven()
                .withFailure()
                .executeTest()
                .verifyTextInLog(
                    "There's no RunOrder with the name nonExistingRunOrder."
                );
    }

    private OutputValidator executeWithRunOrder( String runOrder )
    {
        return forkingMode( unpack() )
                .runOrder( runOrder )
                .executeTest()
                .verifyErrorFree( 3 );
    }

    protected SurefireLauncher forkingMode(SurefireLauncher launcher )
    {
        return launcher.forkMode( "once" );
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
            throw new VerificationException(
                    "Does not contain tests in sequence: "
                            + Arrays.toString( testnames )
            );
        }
    }
}
