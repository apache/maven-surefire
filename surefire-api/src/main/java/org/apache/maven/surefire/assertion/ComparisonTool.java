package org.apache.maven.surefire.assertion;

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

/**
 * Utilities for displaying comparison failures.
 */
public class ComparisonTool
{
    private ComparisonTool()
    {
    }

    /**
     * Returns "..." in place of common prefix and "..." in
     * place of common suffix between expected and actual.
     * <p/>
     * Inspired by a patch from Alex Chaffee mailto:alex@purpletech.com
     *
     * @param message  the message to go along with the comparison
     * @param expected the expected value
     * @param actual   the actual value
     * @return the reduced comparison
     */
    static String trimComparison( String message, String expected, String actual )
    {
        String actualValue;
        String expectedValue;

        if ( expected == null && actual == null )
        {
            throw new IllegalArgumentException( "Cannot pass both expected and actual as null" );
        }
        else if ( expected == null || actual == null )
        {
            actualValue = actual;
            expectedValue = expected;
        }
        else
        {
            int end = Math.min( expected.length(), actual.length() );

            int i = 0;
            for ( ; i < end; i++ )
            {
                if ( expected.charAt( i ) != actual.charAt( i ) )
                {
                    break;
                }
            }
            int j = expected.length() - 1;
            int k = actual.length() - 1;
            for ( ; k >= i && j >= i; k--, j-- )
            {
                if ( expected.charAt( j ) != actual.charAt( k ) )
                {
                    break;
                }
            }

            // equal strings
            if ( j < i && k < i )
            {
                throw new IllegalArgumentException( "expected and actual cannot be the same" );
            }
            else
            {
                expectedValue = expected.substring( i, j + 1 );
                actualValue = actual.substring( i, k + 1 );
                if ( i <= end && i > 0 )
                {
                    expectedValue = "..." + expectedValue;
                    actualValue = "..." + actualValue;
                }

                if ( j < expected.length() - 1 )
                {
                    expectedValue = expectedValue + "...";
                }
                if ( k < actual.length() - 1 )
                {
                    actualValue = actualValue + "...";
                }
            }
        }
        return formatMismatch( message, expectedValue, actualValue );
    }

    /**
     * Format a message for a comparison failure.
     *
     * @param message  the message to go with the failure
     * @param expected the expected value
     * @param actual   the actual value
     * @return the formatted string
     */
    static String formatMismatch( String message, Object expected, Object actual )
    {
        String formatted = "";

        if ( message != null )
        {
            formatted = message + " ";
        }

        // TODO! i18n
        return formatted + "expected:<" + expected + "> but was:<" + actual + ">";
    }
}
