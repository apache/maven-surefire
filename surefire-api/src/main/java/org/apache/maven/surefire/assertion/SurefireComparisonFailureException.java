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
 * Thrown when an assertion equals for Strings failed.
 * <p/>
 * Inspired by a patch from Alex Chaffee mailto:alex@purpletech.com
 *
 * @noinspection UncheckedExceptionClass
 */
public class SurefireComparisonFailureException
    extends SurefireAssertionFailedException
{
    private final String expected;

    private final String actual;

    /**
     * Constructs a comparison failure.
     *
     * @param message  the identifying message or null
     * @param expected the expected string value
     * @param actual   the actual string value
     */
    public SurefireComparisonFailureException( String message, String expected, String actual )
    {
        super( message );
        this.expected = expected;
        this.actual = actual;
    }

    /**
     * Returns "..." in place of common prefix and "..." in
     * place of common suffix between expected and actual.
     *
     * @see java.lang.Throwable#getMessage()
     */
    public String getMessage()
    {
        String path;
        if ( expected == null || actual == null )
        {
            path = SurefireAssert.formatMismatch( super.getMessage(), expected, actual );
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

            String actual;
            String expected;

            // equal strings
            if ( j < i && k < i )
            {
                expected = this.expected;
                actual = this.actual;
            }
            else
            {
                expected = this.expected.substring( i, j + 1 );
                actual = this.actual.substring( i, k + 1 );
                if ( i <= end && i > 0 )
                {
                    expected = "..." + expected;
                    actual = "..." + actual;
                }

                if ( j < this.expected.length() - 1 )
                {
                    expected = expected + "...";
                }
                if ( k < this.actual.length() - 1 )
                {
                    actual = actual + "...";
                }
            }
            path = SurefireAssert.formatMismatch( super.getMessage(), expected, actual );
        }
        return path;
    }
}
