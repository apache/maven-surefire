package org.apache.maven.surefire.battery.assertion;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Thrown when an assertion equals for Strings failed.
 *
 * Inspired by a patch from Alex Chaffee mailto:alex@purpletech.com
 */
public class BatteryComparisonFailure extends BatteryAssertionFailedError
{
    private String fExpected;
    private String fActual;

    /**
     * Constructs a comparison failure.
     * @param message the identifying message or null
     * @param expected the expected string value
     * @param actual the actual string value
     */
    public BatteryComparisonFailure( String message, String expected, String actual )
    {
        super( message );
        fExpected = expected;
        fActual = actual;
    }

    /**
     * Returns "..." in place of common prefix and "..." in
     * place of common suffix between expected and actual.
     *
     * @see java.lang.Throwable#getMessage()
     */
    public String getMessage()
    {
        if ( fExpected == null || fActual == null )
            return BatteryAssert.formatMismatch( super.getMessage(), fExpected, fActual );

        int end = Math.min( fExpected.length(), fActual.length() );

        int i = 0;
        for ( ; i < end; i++ )
        {
            if ( fExpected.charAt( i ) != fActual.charAt( i ) )
                break;
        }
        int j = fExpected.length() - 1;
        int k = fActual.length() - 1;
        for ( ; k >= i && j >= i; k--, j-- )
        {
            if ( fExpected.charAt( j ) != fActual.charAt( k ) )
                break;
        }
        String actual, expected;

        // equal strings
        if ( j < i && k < i )
        {
            expected = fExpected;
            actual = fActual;
        }
        else
        {
            expected = fExpected.substring( i, j + 1 );
            actual = fActual.substring( i, k + 1 );
            if ( i <= end && i > 0 )
            {
                expected = "..." + expected;
                actual = "..." + actual;
            }

            if ( j < fExpected.length() - 1 )
                expected = expected + "...";
            if ( k < fActual.length() - 1 )
                actual = actual + "...";
        }
        return BatteryAssert.formatMismatch( super.getMessage(), expected, actual );
    }
}
