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
 * Test the surefire assertion class.
 *
 * @noinspection ProhibitedExceptionCaught
 */
public class SurefireAssertTest
    extends TestCase
{
    public void testFailWithNoMessage()
    {
        try
        {
            SurefireAssert.fail( (String) null );
            fail( "Should have thrown a NullPointerException" );
        }
        catch ( NullPointerException e )
        {
            // expected
        }
    }

    public void testFailWithNoCause()
    {
        try
        {
            SurefireAssert.fail( (Throwable) null );
            fail( "Should have thrown a NullPointerException" );
        }
        catch ( NullPointerException e )
        {
            // expected
        }
    }


    public void testFailWithMessageButNoCause()
    {
        try
        {
            SurefireAssert.fail( "msg", null );
            fail( "Should have thrown a NullPointerException" );
        }
        catch ( NullPointerException e )
        {
            // expected
        }
    }

    public void testFailWithCauseButNoMessage()
    {
        try
        {
            SurefireAssert.fail( null, new Exception( "msg" ) );
            fail( "Should have thrown a NullPointerException" );
        }
        catch ( NullPointerException e )
        {
            // expected
        }
    }

    public void testFailWithNoMessageOrCause()
    {
        try
        {
            SurefireAssert.fail();
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertNull( e.getMessage() );
        }
    }

    public void testFailWithMessage()
    {
        try
        {
            SurefireAssert.fail( "msg" );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg", e.getMessage() );
        }
    }

    public void testFailWithCause()
    {
        try
        {
            SurefireAssert.fail( new Exception( "nestedMsg" ) );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "null; nested exception is java.lang.Exception: nestedMsg", e.getMessage() );
            assertEquals( "nestedMsg", e.getCause().getMessage() );
        }
    }

    public void testFailWithMessageAndCause()
    {
        try
        {
            SurefireAssert.fail( "msg", new Exception( "nestedMsg" ) );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg; nested exception is java.lang.Exception: nestedMsg", e.getMessage() );
            assertEquals( "nestedMsg", e.getCause().getMessage() );
        }
    }

    public void testFailAssertTrueWithMessage()
    {
        try
        {
            SurefireAssert.assertTrue( "msg", false );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg", e.getMessage() );
        }
    }

    public void testPassAssertTrueWithMessage()
    {
        SurefireAssert.assertTrue( "msg", true );
    }

    public void testFailAssertTrueWithoutMessage()
    {
        try
        {
            SurefireAssert.assertTrue( false );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertNull( e.getMessage() );
        }
    }

    public void testPassAssertTrueWithoutMessage()
    {
        SurefireAssert.assertTrue( true );
    }

    public void testFailAssertFalseWithMessage()
    {
        try
        {
            SurefireAssert.assertFalse( "msg", true );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg", e.getMessage() );
        }
    }

    public void testPassAssertFalseWithMessage()
    {
        SurefireAssert.assertFalse( "msg", false );
    }

    public void testFailAssertFalseWithoutMessage()
    {
        try
        {
            SurefireAssert.assertFalse( true );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertNull( e.getMessage() );
        }
    }

    public void testPassAssertFalseWithoutMessage()
    {
        SurefireAssert.assertFalse( false );
    }

    public void testFailAssertEqualsStringWithMessage()
    {
        try
        {
            SurefireAssert.assertEquals( "msg", "foo", "bar" );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg expected:<foo> but was:<bar>", e.getMessage() );
        }
    }

    public void testFailAssertEqualsStringExpectedNullWithMessage()
    {
        try
        {
            SurefireAssert.assertEquals( "msg", null, "bar" );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg expected:<null> but was:<bar>", e.getMessage() );
        }
    }

    public void testFailAssertEqualsStringActualNullWithMessage()
    {
        try
        {
            SurefireAssert.assertEquals( "msg", "foo", null );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg expected:<foo> but was:<null>", e.getMessage() );
        }
    }

    public void testPassAssertEqualsStringWithMessage()
    {
        SurefireAssert.assertEquals( "msg", "foo", "foo" );
    }

    public void testPassAssertEqualsStringBothNullWithMessage()
    {
        SurefireAssert.assertEquals( "msg", null, null );
    }

    public void testPassAssertEqualsStringWithoutMessage()
    {
        SurefireAssert.assertEquals( "foo", "foo" );
    }

    public void testFailAssertEqualsStringWithoutMessage()
    {
        try
        {
            SurefireAssert.assertEquals( "foo", "bar" );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "expected:<foo> but was:<bar>", e.getMessage() );
        }
    }

    public void testFailAssertEqualsObjectWithMessage()
    {
        try
        {
            SurefireAssert.assertEquals( "msg", new DummyObject( "foo" ), new DummyObject( "bar" ) );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg expected:<foo> but was:<bar>", e.getMessage() );
        }
    }

    public void testFailAssertEqualsObjectDoesntTrim()
    {
        try
        {
            SurefireAssert.assertEquals( "msg", new DummyObject( "foo" ), new DummyObject( "fobar" ) );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg expected:<foo> but was:<fobar>", e.getMessage() );
        }
    }

    public void testFailAssertEqualsObjectExpectedNullWithMessage()
    {
        try
        {
            SurefireAssert.assertEquals( "msg", null, new DummyObject( "bar" ) );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg expected:<null> but was:<bar>", e.getMessage() );
        }
    }

    public void testFailAssertEqualsObjectActualNullWithMessage()
    {
        try
        {
            SurefireAssert.assertEquals( "msg", new DummyObject( "foo" ), null );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg expected:<foo> but was:<null>", e.getMessage() );
        }
    }

    public void testPassAssertEqualsObjectWithMessage()
    {
        SurefireAssert.assertEquals( "msg", new DummyObject( "foo" ), new DummyObject( "foo" ) );
    }

    public void testPassAssertEqualsObjectBothNullWithMessage()
    {
        SurefireAssert.assertEquals( "msg", null, null );
    }

    public void testPassAssertEqualsObjectWithoutMessage()
    {
        SurefireAssert.assertEquals( new DummyObject( "foo" ), new DummyObject( "foo" ) );
    }

    public void testFailAssertEqualsObjectWithoutMessage()
    {
        try
        {
            SurefireAssert.assertEquals( new DummyObject( "foo" ), new DummyObject( "bar" ) );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "expected:<foo> but was:<bar>", e.getMessage() );
        }
    }

    public void testFailAssertEqualsIntWithMessage()
    {
        try
        {
            SurefireAssert.assertEquals( "msg", 1, 2 );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg expected:<1> but was:<2>", e.getMessage() );
        }
    }

    public void testPassAssertEqualsIntWithMessage()
    {
        SurefireAssert.assertEquals( "msg", 1, 1 );
    }

    public void testPassAssertEqualsIntWithoutMessage()
    {
        SurefireAssert.assertEquals( 1, 1 );
    }

    public void testFailAssertEqualsIntWithoutMessage()
    {
        try
        {
            SurefireAssert.assertEquals( 1, 2 );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "expected:<1> but was:<2>", e.getMessage() );
        }
    }

    public void testFailAssertEqualsLongWithMessage()
    {
        try
        {
            SurefireAssert.assertEquals( "msg", 1L, 2L );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg expected:<1> but was:<2>", e.getMessage() );
        }
    }

    public void testPassAssertEqualsLongWithMessage()
    {
        SurefireAssert.assertEquals( "msg", 1L, 1L );
    }

    public void testPassAssertEqualsLongWithoutMessage()
    {
        SurefireAssert.assertEquals( 1L, 1L );
    }

    public void testFailAssertEqualsLongWithoutMessage()
    {
        try
        {
            SurefireAssert.assertEquals( 1L, 2L );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "expected:<1> but was:<2>", e.getMessage() );
        }
    }

    public void testFailAssertEqualsFloatWithMessage()
    {
        try
        {
            SurefireAssert.assertEquals( "msg", 1.2f, 3.4f, 0.1f );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg expected:<1.2> but was:<3.4>", e.getMessage() );
        }
    }

    public void testPassAssertEqualsFloatWithMessage()
    {
        SurefireAssert.assertEquals( "msg", 1.2f, 1.2f, 0.1f );
    }

    public void testPassAssertEqualsFloatWithoutMessage()
    {
        SurefireAssert.assertEquals( 1.2f, 1.2f, 0.1f );
    }

    public void testFailAssertEqualsFloatWithoutMessage()
    {
        try
        {
            SurefireAssert.assertEquals( 1.2f, 3.4f, 0.1f );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "expected:<1.2> but was:<3.4>", e.getMessage() );
        }
    }

    public void testPassAssertEqualsFloatWithFudge()
    {
        SurefireAssert.assertEquals( 1.2f, 1.3f, 0.5f );
    }

    public void testFailAssertEqualsFloatWithoutFudge()
    {
        try
        {
            SurefireAssert.assertEquals( 1.2f, 1.3f, 0.05f );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "expected:<1.2> but was:<1.3>", e.getMessage() );
        }
    }

    public void testFailAssertEqualsFloatExpectedIsInfinite()
    {
        try
        {
            SurefireAssert.assertEquals( Float.POSITIVE_INFINITY, 1.3f, 0.05f );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "expected:<Infinity> but was:<1.3>", e.getMessage() );
        }
    }

    public void testFailAssertEqualsFloatActualIsInfinite()
    {
        try
        {
            SurefireAssert.assertEquals( 1.2f, Float.POSITIVE_INFINITY, 0.05f );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "expected:<1.2> but was:<Infinity>", e.getMessage() );
        }
    }

    public void testPassAssertEqualsFloatBothAreInfinite()
    {
        SurefireAssert.assertEquals( Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, 0.05f );
    }

    public void testFailAssertEqualsDoubleWithMessage()
    {
        try
        {
            SurefireAssert.assertEquals( "msg", 1.2, 3.4, 0.1 );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg expected:<1.2> but was:<3.4>", e.getMessage() );
        }
    }

    public void testPassAssertEqualsDoubleWithMessage()
    {
        SurefireAssert.assertEquals( "msg", 1.2, 1.2, 0.1 );
    }

    public void testPassAssertEqualsDoubleWithoutMessage()
    {
        SurefireAssert.assertEquals( 1.2, 1.2, 0.1 );
    }

    public void testFailAssertEqualsDoubleWithoutMessage()
    {
        try
        {
            SurefireAssert.assertEquals( 1.2, 3.4, 0.1 );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "expected:<1.2> but was:<3.4>", e.getMessage() );
        }
    }

    public void testPassAssertEqualsDoubleWithFudge()
    {
        SurefireAssert.assertEquals( 1.2, 1.3, 0.5 );
    }

    public void testFailAssertEqualsDoubleWithoutFudge()
    {
        try
        {
            SurefireAssert.assertEquals( 1.2, 1.3, 0.05 );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "expected:<1.2> but was:<1.3>", e.getMessage() );
        }
    }

    public void testFailAssertEqualsDoubleExpectedIsInfinite()
    {
        try
        {
            SurefireAssert.assertEquals( Double.POSITIVE_INFINITY, 1.3, 0.05 );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "expected:<Infinity> but was:<1.3>", e.getMessage() );
        }
    }

    public void testFailAssertEqualsDoubleActualIsInfinite()
    {
        try
        {
            SurefireAssert.assertEquals( 1.2, Double.POSITIVE_INFINITY, 0.05 );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "expected:<1.2> but was:<Infinity>", e.getMessage() );
        }
    }

    public void testPassAssertEqualsDoubleBothAreInfinite()
    {
        SurefireAssert.assertEquals( Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0.05 );
    }

    public void testFailAssertEqualsByteWithMessage()
    {
        try
        {
            SurefireAssert.assertEquals( "msg", (byte) 1, (byte) 2 );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg expected:<1> but was:<2>", e.getMessage() );
        }
    }

    public void testPassAssertEqualsByteWithMessage()
    {
        SurefireAssert.assertEquals( "msg", (byte) 1, (byte) 1 );
    }

    public void testPassAssertEqualsByteWithoutMessage()
    {
        SurefireAssert.assertEquals( (byte) 1, (byte) 1 );
    }

    public void testFailAssertEqualsByteWithoutMessage()
    {
        try
        {
            SurefireAssert.assertEquals( (byte) 1, (byte) 2 );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "expected:<1> but was:<2>", e.getMessage() );
        }
    }

    public void testFailAssertEqualsBooleanWithMessage()
    {
        try
        {
            SurefireAssert.assertEquals( "msg", true, false );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg expected:<true> but was:<false>", e.getMessage() );
        }
        try
        {
            SurefireAssert.assertEquals( "msg", false, true );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg expected:<false> but was:<true>", e.getMessage() );
        }
    }

    public void testPassAssertEqualsBooleanWithMessage()
    {
        SurefireAssert.assertEquals( "msg", true, true );
        SurefireAssert.assertEquals( "msg", false, false );
    }

    public void testPassAssertEqualsBooleanWithoutMessage()
    {
        SurefireAssert.assertEquals( true, true );
        SurefireAssert.assertEquals( false, false );
    }

    public void testFailAssertEqualsBooleanWithoutMessage()
    {
        try
        {
            SurefireAssert.assertEquals( true, false );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "expected:<true> but was:<false>", e.getMessage() );
        }
        try
        {
            SurefireAssert.assertEquals( false, true );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "expected:<false> but was:<true>", e.getMessage() );
        }
    }

    public void testFailAssertEqualsCharWithMessage()
    {
        try
        {
            SurefireAssert.assertEquals( "msg", '1', '2' );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg expected:<1> but was:<2>", e.getMessage() );
        }
    }

    public void testPassAssertEqualsCharWithMessage()
    {
        SurefireAssert.assertEquals( "msg", '1', '1' );
    }

    public void testPassAssertEqualsCharWithoutMessage()
    {
        SurefireAssert.assertEquals( '1', '1' );
    }

    public void testFailAssertEqualsCharWithoutMessage()
    {
        try
        {
            SurefireAssert.assertEquals( '1', '2' );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "expected:<1> but was:<2>", e.getMessage() );
        }
    }

    public void testFailAssertEqualsShortWithMessage()
    {
        try
        {
            SurefireAssert.assertEquals( "msg", (short) 1, (short) 2 );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg expected:<1> but was:<2>", e.getMessage() );
        }
    }

    public void testPassAssertEqualsShortWithMessage()
    {
        SurefireAssert.assertEquals( "msg", (short) 1, (short) 1 );
    }

    public void testPassAssertEqualsShortWithoutMessage()
    {
        SurefireAssert.assertEquals( (short) 1, (short) 1 );
    }

    public void testFailAssertEqualsShortWithoutMessage()
    {
        try
        {
            SurefireAssert.assertEquals( (short) 1, (short) 2 );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "expected:<1> but was:<2>", e.getMessage() );
        }
    }

    public void testFailAssertNullWithMessage()
    {
        try
        {
            SurefireAssert.assertNull( "msg", new DummyObject( "foo" ) );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg", e.getMessage() );
        }
    }

    public void testPassAssertNullWithMessage()
    {
        SurefireAssert.assertNull( "msg", null );
    }

    public void testFailAssertNullWithoutMessage()
    {
        try
        {
            SurefireAssert.assertNull( new DummyObject( "foo" ) );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertNull( e.getMessage() );
        }
    }

    public void testPassAssertNullWithoutMessage()
    {
        SurefireAssert.assertNull( null );
    }

    public void testFailAssertNotNullWithMessage()
    {
        try
        {
            SurefireAssert.assertNotNull( "msg", null );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg", e.getMessage() );
        }
    }

    public void testPassAssertNotNullWithMessage()
    {
        SurefireAssert.assertNotNull( "msg", new DummyObject( "foo" ) );
    }

    public void testFailAssertNotNullWithoutMessage()
    {
        try
        {
            SurefireAssert.assertNotNull( null );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertNull( e.getMessage() );
        }
    }

    public void testPassAssertNotNullWithoutMessage()
    {
        SurefireAssert.assertNotNull( new DummyObject( "foo" ) );
    }

    public void testFailAssertSameWithMessage()
    {
        try
        {
            SurefireAssert.assertSame( "msg", new DummyObject( "foo" ), new DummyObject( "foo" ) );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg expected same:<foo> was not:<foo>", e.getMessage() );
        }
    }

    public void testFailAssertSameDoesntTrim()
    {
        try
        {
            SurefireAssert.assertSame( "msg", new DummyObject( "foo" ), new DummyObject( "fobar" ) );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg expected same:<foo> was not:<fobar>", e.getMessage() );
        }
    }

    public void testFailAssertSameExpectedNullWithMessage()
    {
        try
        {
            SurefireAssert.assertSame( "msg", null, new DummyObject( "bar" ) );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg expected same:<null> was not:<bar>", e.getMessage() );
        }
    }

    public void testFailAssertSameActualNullWithMessage()
    {
        try
        {
            SurefireAssert.assertSame( "msg", new DummyObject( "foo" ), null );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg expected same:<foo> was not:<null>", e.getMessage() );
        }
    }

    public void testPassAssertSameWithMessage()
    {
        DummyObject value = new DummyObject( "foo" );
        SurefireAssert.assertSame( "msg", value, value );
    }

    public void testPassAssertSameBothNullWithMessage()
    {
        SurefireAssert.assertSame( "msg", null, null );
    }

    public void testPassAssertSameWithoutMessage()
    {
        DummyObject value = new DummyObject( "foo" );
        SurefireAssert.assertSame( value, value );
    }

    public void testFailAssertSameWithoutMessage()
    {
        try
        {
            SurefireAssert.assertSame( new DummyObject( "foo" ), new DummyObject( "foo" ) );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "expected same:<foo> was not:<foo>", e.getMessage() );
        }
    }

    public void testFailAssertNotSameWithMessage()
    {
        try
        {
            DummyObject value = new DummyObject( "foo" );
            SurefireAssert.assertNotSame( "msg", value, value );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg expected not same", e.getMessage() );
        }
    }

    public void testFailAssertNotSameExpectedNullWithMessage()
    {
        SurefireAssert.assertNotSame( "msg", null, new DummyObject( "bar" ) );
    }

    public void testFailAssertNotSameActualNullWithMessage()
    {
        SurefireAssert.assertNotSame( "msg", new DummyObject( "foo" ), null );
    }

    public void testPassAssertNotSameWithMessage()
    {
        SurefireAssert.assertNotSame( "msg", new DummyObject( "foo" ), new DummyObject( "foo" ) );
    }

    public void testPassAssertNotSameBothNullWithMessage()
    {
        try
        {
            SurefireAssert.assertNotSame( "msg", null, null );
            fail( "Should not be the same" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "msg expected not same", e.getMessage() );
        }
    }

    public void testPassAssertNotSameWithoutMessage()
    {
        SurefireAssert.assertNotSame( new DummyObject( "foo" ), new DummyObject( "foo" ) );
    }

    public void testFailAssertNotSameWithoutMessage()
    {
        try
        {
            DummyObject value = new DummyObject( "foo" );
            SurefireAssert.assertNotSame( value, value );
            fail( "Should have failed" );
        }
        catch ( SurefireAssertionFailedException e )
        {
            // expected
            assertEquals( "expected not same", e.getMessage() );
        }
    }

    private static class DummyObject
    {
        private final String value;

        private DummyObject( String value )
        {
            this.value = value;
        }

        public boolean equals( Object obj )
        {
            if ( this == obj )
            {
                return true;
            }
            if ( obj == null || getClass() != obj.getClass() )
            {
                return false;
            }

            DummyObject that = (DummyObject) obj;

            return !( value != null ? !value.equals( that.value ) : that.value != null );

        }

        public int hashCode()
        {
            return value != null ? value.hashCode() : 0;
        }

        public String toString()
        {
            return value;
        }
    }
}
