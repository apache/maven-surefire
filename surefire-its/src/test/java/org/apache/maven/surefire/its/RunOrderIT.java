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

import java.util.Arrays;
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
@SuppressWarnings( "checkstyle:magicnumber" )
public class RunOrderIT
    extends SurefireJUnit4IntegrationTestCase
{
    private static final String[] TESTS_IN_ALPHABETICAL_ORDER = { "TA", "TB", "TC" };

    private static final String[] TESTS_IN_REVERSE_ALPHABETICAL_ORDER = { "TC", "TB", "TA" };

    // testing random is left as an exercise to the reader. Patches welcome

    @Test
    public void testAlphabeticalJUnit4()
        throws Exception
    {
        OutputValidator validator = executeWithRunOrder( "alphabetical", "junit4" );
        assertTestnamesAppearInSpecificOrder( validator, TESTS_IN_ALPHABETICAL_ORDER );
    }

    @Test
    public void testAlphabeticalJUnit5()
        throws Exception
    {
        OutputValidator validator = executeWithRunOrder( "alphabetical", "junit5" );
        assertTestnamesAppearInSpecificOrder( validator, TESTS_IN_ALPHABETICAL_ORDER );
    }

    @Test
    public void testRandomJUnit4DifferentSeed()
        throws Exception
    {
        long seed = 0L;
        OutputValidator validator = executeWithRandomOrder( "junit4", seed );
        String[] expected = validator.getStringsOrderInLog( TESTS_IN_ALPHABETICAL_ORDER );
        for ( long i = seed; i < 5 + seed; i++ )
        {
            OutputValidator validator2 = executeWithRandomOrder( "junit4", i );
            String[] observed = validator2.getStringsOrderInLog( TESTS_IN_ALPHABETICAL_ORDER );
            if ( ! Arrays.equals( expected, observed ) )
            {
                return;
            }
        }
        throw new VerificationException( "All random orders with the different seeds produced the same orders" );
    }

    @Test
    public void testRandomJUnit4SameSeed()
        throws Exception
    {
        long seed = 0L;
        OutputValidator validator = executeWithRandomOrder( "junit4", seed );
        String[] expected = validator.getStringsOrderInLog( TESTS_IN_ALPHABETICAL_ORDER );
        for ( long i = 0; i < 5; i++ )
        {
            OutputValidator validator2 = executeWithRandomOrder( "junit4", seed );
            String[] observed = validator2.getStringsOrderInLog( TESTS_IN_ALPHABETICAL_ORDER );
            if ( ! Arrays.equals( expected, observed ) )
            {
                throw new VerificationException( "Random orders with the same seed produced different orders" );
            }
        }
    }
    
    @Test
    public void testReverseAlphabeticalJUnit4()
        throws Exception
    {
        OutputValidator validator = executeWithRunOrder( "reversealphabetical", "junit4" );
        assertTestnamesAppearInSpecificOrder( validator, TESTS_IN_REVERSE_ALPHABETICAL_ORDER );
    }

    @Test
    public void testReverseAlphabeticalJUnit5()
        throws Exception
    {
        OutputValidator validator = executeWithRunOrder( "reversealphabetical", "junit5" );
        assertTestnamesAppearInSpecificOrder( validator, TESTS_IN_REVERSE_ALPHABETICAL_ORDER );
    }

    @Test
    public void testHourlyJUnit4()
        throws Exception
    {
        int startHour = Calendar.getInstance().get( Calendar.HOUR_OF_DAY );
        OutputValidator validator = executeWithRunOrder( "hourly", "junit4" );
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
    public void testHourlyJUnit5()
        throws Exception
    {
        int startHour = Calendar.getInstance().get( Calendar.HOUR_OF_DAY );
        OutputValidator validator = executeWithRunOrder( "hourly", "junit5" );
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
    public void testNonExistingRunOrderJUnit4()
    {
        unpack()
            .activateProfile( "junit4" )
            .forkMode( getForkMode() )
            .runOrder( "nonExistingRunOrder" )
            .maven()
            .withFailure()
            .executeTest()
            .verifyTextInLog( "There's no RunOrder with the name nonExistingRunOrder." );
    }

    @Test
    public void testNonExistingRunOrderJUnit5()
    {
        unpack()
            .activateProfile( "junit5" )
            .forkMode( getForkMode() )
            .runOrder( "nonExistingRunOrder" )
            .maven()
            .withFailure()
            .executeTest()
            .verifyTextInLog( "There's no RunOrder with the name nonExistingRunOrder." );
    }

    private OutputValidator executeWithRunOrder( String runOrder, String profile )
    {
        return unpack()
            .activateProfile( profile )
            .forkMode( getForkMode() )
            .runOrder( runOrder )
            .executeTest()
            .verifyErrorFree( 3 );
    }

    private OutputValidator executeWithRandomOrder( String profile, long seed  )
    {
        return unpack()
            .activateProfile( profile )
            .forkMode( getForkMode() )
            .runOrder( "random" )
            .runOrderRandomSeed( String.valueOf( seed ) )
            .executeTest()
            .verifyErrorFree( 3 );
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
