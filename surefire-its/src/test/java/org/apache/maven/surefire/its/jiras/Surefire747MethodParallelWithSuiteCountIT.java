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
import org.junit.Before;
import org.junit.Test;

import java.text.Format;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import static java.lang.String.format;
import static java.math.RoundingMode.DOWN;
import static java.util.Locale.ROOT;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Kristian Rosenvold
 */
public class Surefire747MethodParallelWithSuiteCountIT
        extends SurefireJUnit4IntegrationTestCase
{
    // if you want to change his constant, change it in SuiteTest1.java and SuiteTest2.java as well
    private static final int PERFORMANCE_TEST_MULTIPLICATION_FACTOR = 4;

    private Format lowerScaleFormatter, noFractionalDigitsFormatter;

    private static Set<String> printTestLines( OutputValidator validator, String pattern )
            throws VerificationException
    {
        Set<String> log = new TreeSet<>( validator.loadLogLines() );
        for ( Iterator<String> it = log.iterator(); it.hasNext(); )
        {
            String line = it.next();
            if ( !line.contains( pattern ) )
            {
                it.remove();
            }
        }
        return log;
    }

    private static long duration( String logLine )
    {
        return Integer.decode( logLine.split( "=" )[1] );
    }

    @Before
    public void init()
    {
        NumberFormat lowScaleFormatter = NumberFormat.getInstance( ROOT );
        lowScaleFormatter.setRoundingMode( DOWN );
        lowScaleFormatter.setMinimumFractionDigits( 1 );
        lowScaleFormatter.setMaximumFractionDigits( 1 );
        this.lowerScaleFormatter = lowScaleFormatter;

        NumberFormat noFractionalDigitsFormatter = NumberFormat.getInstance( ROOT );
        noFractionalDigitsFormatter.setRoundingMode( DOWN );
        noFractionalDigitsFormatter.setMinimumFractionDigits( 0 );
        noFractionalDigitsFormatter.setMaximumFractionDigits( 0 );
        this.noFractionalDigitsFormatter = noFractionalDigitsFormatter;
    }

    @Test
    public void testMethodsParallelWithSuite()
            throws VerificationException
    {
        OutputValidator validator = unpack().executeTest().verifyErrorFree( 6 );
        Set<String> testLines = printTestLines( validator, "test finished after duration=" );
        assertThat( testLines.size(), is( 2 ) );
        for ( String testLine : testLines )
        {
            long duration = duration( testLine );
            long min = 250 * PERFORMANCE_TEST_MULTIPLICATION_FACTOR;
            long max = 750 * PERFORMANCE_TEST_MULTIPLICATION_FACTOR;
            assertTrue( format( "duration %d should be between %d and %d ms", duration, min, max ),
                              duration > min && duration < max
            );
        }
        Set<String> suiteLines = printTestLines( validator, "suite finished after duration=" );
        assertThat( suiteLines.size(), is( 1 ) );
        long duration = duration( suiteLines.iterator().next() );
        long min = 750 * PERFORMANCE_TEST_MULTIPLICATION_FACTOR;
        long max = 1250 * PERFORMANCE_TEST_MULTIPLICATION_FACTOR;
        assertTrue( format( "duration %d should be between %d and %d ms", duration, min, max ),
                          duration > min && duration < max
        );

        String delayMin = lowerScaleFormatter.format( 0.98 * PERFORMANCE_TEST_MULTIPLICATION_FACTOR * 0.5 );
        String delayMax = noFractionalDigitsFormatter.format( PERFORMANCE_TEST_MULTIPLICATION_FACTOR * 0.5 ) + ".";

        for ( String line : validator.loadLogLines() )
        {
            if ( line.startsWith( "Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed:" ) )
            {
                assertThat( line, anyOf( // 1.9xx to 2.xxx can vary depending on CI jobs
                                               containsString( "Time elapsed: " + delayMin ),
                                               containsString( "Time elapsed: " + delayMax )
                        )
                );
                assertThat( line, anyOf(
                                               endsWith( " s - in surefire747.SuiteTest1" ),
                                               endsWith( " s - in surefire747.SuiteTest2" )
                        )
                );
            }
        }
    }

    @Test
    public void testClassesParallelWithSuite()
            throws VerificationException
    {
        OutputValidator validator = unpack().parallelClasses().executeTest().verifyErrorFree( 6 );
        Set<String> testLines = printTestLines( validator, "test finished after duration=" );
        assertThat( testLines.size(), is( 2 ) );
        for ( String testLine : testLines )
        {
            long duration = duration( testLine );
            long min = 1450 * PERFORMANCE_TEST_MULTIPLICATION_FACTOR;
            long max = 1750 * PERFORMANCE_TEST_MULTIPLICATION_FACTOR;
            assertTrue( format( "duration %d should be between %d and %d ms", duration, min, max ),
                              duration > min && duration < max
            );
        }
        Set<String> suiteLines = printTestLines( validator, "suite finished after duration=" );
        assertThat( suiteLines.size(), is( 1 ) );
        long duration = duration( suiteLines.iterator().next() );
        long min = 1450 * PERFORMANCE_TEST_MULTIPLICATION_FACTOR;
        long max = 1750 * PERFORMANCE_TEST_MULTIPLICATION_FACTOR;
        assertTrue( format( "duration %d should be between %d and %d ms", duration, min, max ),
                          duration > min && duration < max
        );
    }

    public SurefireLauncher unpack()
    {
        return unpack( "junit47-parallel-with-suite" );
    }
}
