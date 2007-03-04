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

import junit.framework.TestCase;

/**
 * Test the comparison tool string representations.
 */
public class ComparisonToolTest
    extends TestCase
{
    public void testFormatMismatchNoMessage()
    {
        assertEquals( "expected:<foo> but was:<bar>", ComparisonTool.formatMismatch( null, "foo", "bar" ) );
    }

    public void testFormatMismatchWithMessage()
    {
        assertEquals( "msg expected:<foo> but was:<bar>", ComparisonTool.formatMismatch( "msg", "foo", "bar" ) );
    }

    public void testTrimComparisonActualNull()
    {
        assertEquals( "msg expected:<foo> but was:<null>", ComparisonTool.trimComparison( "msg", "foo", null ) );
    }

    public void testTrimComparisonExpectedNull()
    {
        assertEquals( "msg expected:<null> but was:<bar>", ComparisonTool.trimComparison( "msg", null, "bar" ) );
    }

    public void testTrimComparisonBothNull()
    {
        try
        {
            ComparisonTool.trimComparison( "msg", null, null );
            fail( "Should fail to pass in equal values" );
        }
        catch ( IllegalArgumentException e )
        {
            // correct
        }
    }

    public void testTrimComparisonEqual()
    {
        try
        {
            ComparisonTool.trimComparison( "msg", "foo", "foo" );
            fail( "Should fail to pass in equal values" );
        }
        catch ( IllegalArgumentException e )
        {
            // correct
        }
    }

    public void testTrimComparisonNoMatch()
    {
        assertEquals( "msg expected:<foo> but was:<bar>", ComparisonTool.trimComparison( "msg", "foo", "bar" ) );
    }

    public void testTrimComparisonMatchStart()
    {
        assertEquals( "msg expected:<...rah> but was:<...bar>",
                      ComparisonTool.trimComparison( "msg", "foorah", "foobar" ) );
    }

    public void testTrimComparisonMatchStartWholeExpected()
    {
        assertEquals( "msg expected:<...> but was:<...bar>", ComparisonTool.trimComparison( "msg", "foo", "foobar" ) );
    }

    public void testTrimComparisonMatchStartWholeActual()
    {
        assertEquals( "msg expected:<...rah> but was:<...>", ComparisonTool.trimComparison( "msg", "foorah", "foo" ) );
    }

    public void testTrimComparisonMatchEnd()
    {
        assertEquals( "msg expected:<bop...> but was:<foo...>",
                      ComparisonTool.trimComparison( "msg", "bopbar", "foobar" ) );
    }

    public void testTrimComparisonMatchEndWholeExpected()
    {
        assertEquals( "msg expected:<...> but was:<foo...>", ComparisonTool.trimComparison( "msg", "bar", "foobar" ) );
    }

    public void testTrimComparisonMatchEndWholeActual()
    {
        assertEquals( "msg expected:<foo...> but was:<...>", ComparisonTool.trimComparison( "msg", "foorah", "rah" ) );
    }

    public void testTrimComparisonMatchStartAndEnd()
    {
        assertEquals( "msg expected:<...bar...> but was:<...foo...>",
                      ComparisonTool.trimComparison( "msg", "foobarbaz", "foofoobaz" ) );
    }

    public void testTrimComparisonMatchStartAndEndWholeExpected()
    {
        assertEquals( "msg expected:<......> but was:<...def...>",
                      ComparisonTool.trimComparison( "msg", "abcghi", "abcdefghi" ) );
    }

    public void testTrimComparisonMatchStartAndEndWholeActual()
    {
        assertEquals( "msg expected:<...def...> but was:<......>",
                      ComparisonTool.trimComparison( "msg", "abcdefghi", "abcghi" ) );
    }
}
