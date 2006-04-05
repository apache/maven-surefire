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

import junit.framework.TestCase;
import org.apache.maven.surefire.Surefire;

public class BatteryAssert extends TestCase
{
    public static void verify( boolean condition )
    {
        String detailMsg = Surefire.getResources().getString( "conditionFalse" );

        verify( condition, detailMsg );
    }

    public static void verify( boolean condition, String message )
    {

        if ( message == null )
        {
            throw new NullPointerException( "message is null" );
        }

        if ( !condition )
        {
            throw new BatteryTestFailedException( message );
        }
    }

    public static void fail()
    {
        throw new BatteryTestFailedException();
    }

    public static void fail( String message )
    {
        if ( message == null )
        {
            throw new NullPointerException( "message is null" );
        }
        throw new BatteryTestFailedException( message );
    }

    public static void fail( String message, Throwable cause )
    {

        if ( message == null )
        {
            throw new NullPointerException( "message is null" );
        }

        if ( cause == null )
        {
            throw new NullPointerException( "cause is null" );
        }

        throw new BatteryTestFailedException( message, cause );
    }

    public static void fail( Throwable cause )
    {
        if ( cause == null )
        {
            throw new NullPointerException( "cause is null" );
        }

        throw new BatteryTestFailedException( cause.toString(), cause );
    }

    // ----------------------------------------------------------------------
    // JUnit type assertions
    // ----------------------------------------------------------------------

    public static void assertTrue( String message, boolean condition )
    {
        if ( !condition )
        {
            fail( message );
        }
    }

    public static void assertTrue( boolean condition )
    {
        assertTrue( null, condition );
    }

    public static void assertFalse( String message, boolean condition )
    {
        assertTrue( message, !condition );
    }

    public static void assertFalse( boolean condition )
    {
        assertFalse( null, condition );
    }

    public static void assertEquals( String message, Object expected, Object actual )
    {
        if ( expected == null && actual == null )
        {
            return;
        }
        if ( expected != null && expected.equals( actual ) )
        {
            return;
        }

        failNotEquals( message, expected, actual );
    }

    public static void assertEquals( Object expected, Object actual )
    {
        assertEquals( null, expected, actual );
    }

    public static void assertEquals( String message, String expected, String actual )
    {
        if ( expected == null && actual == null )
        {
            return;
        }

        if ( expected != null && expected.equals( actual ) )
        {
            return;
        }

        throw new BatteryComparisonFailure( message, expected, actual );
    }

    public static void assertEquals( String expected, String actual )
    {
        assertEquals( null, expected, actual );
    }

    public static void assertEquals( String message, double expected, double actual, double delta )
    {
        // handle infinity specially since subtracting to infinite values gives NaN and the
        // the following test fails
        if ( Double.isInfinite( expected ) )
        {
            if ( !( expected == actual ) )
            {
                failNotEquals( message, new Double( expected ), new Double( actual ) );
            }
        }
        else if ( !( Math.abs( expected - actual ) <= delta ) ) // Because comparison with NaN always returns false
        {
            failNotEquals( message, new Double( expected ), new Double( actual ) );
        }
    }

    public static void assertEquals( double expected, double actual, double delta )
    {
        assertEquals( null, expected, actual, delta );
    }

    public static void assertEquals( String message, float expected, float actual, float delta )
    {
        // handle infinity specially since subtracting to infinite values gives NaN and the
        // the following test fails
        if ( Float.isInfinite( expected ) )
        {
            if ( !( expected == actual ) )
            {
                failNotEquals( message, new Float( expected ), new Float( actual ) );}

        }
        else if ( !( Math.abs( expected - actual ) <= delta ) )
        {
            failNotEquals( message, new Float( expected ), new Float( actual ) );
        }
    }

    public static void assertEquals( float expected, float actual, float delta )
    {
        assertEquals( null, expected, actual, delta );
    }

    public static void assertEquals( String message, long expected, long actual )
    {
        assertEquals( message, new Long( expected ), new Long( actual ) );
    }

    public static void assertEquals( long expected, long actual )
    {
        assertEquals( null, expected, actual );
    }

    public static void assertEquals( String message, boolean expected, boolean actual )
    {
        assertEquals( message, new Boolean( expected ), new Boolean( actual ) );
    }

    public static void assertEquals( boolean expected, boolean actual )
    {
        assertEquals( null, expected, actual );
    }

    public static void assertEquals( String message, byte expected, byte actual )
    {
        assertEquals( message, new Byte( expected ), new Byte( actual ) );
    }

    public static void assertEquals( byte expected, byte actual )
    {
        assertEquals( null, expected, actual );
    }

    public static void assertEquals( String message, char expected, char actual )
    {
        assertEquals( message, new Character( expected ), new Character( actual ) );
    }

    public static void assertEquals( char expected, char actual )
    {
        assertEquals( null, expected, actual );
    }

    public static void assertEquals( String message, short expected, short actual )
    {
        assertEquals( message, new Short( expected ), new Short( actual ) );
    }

    public static void assertEquals( short expected, short actual )
    {
        assertEquals( null, expected, actual );
    }

    public static void assertEquals( String message, int expected, int actual )
    {
        assertEquals( message, new Integer( expected ), new Integer( actual ) );
    }

    public static void assertEquals( int expected, int actual )
    {
        assertEquals( null, expected, actual );
    }

    public static void assertNotNull( Object object )
    {
        assertNotNull( null, object );
    }

    public static void assertNotNull( String message, Object object )
    {
        assertTrue( message, object != null );
    }

    public static void assertNull( Object object )
    {
        assertNull( null, object );
    }

    public static void assertNull( String message, Object object )
    {
        assertTrue( message, object == null );
    }

    public static void assertSame( String message, Object expected, Object actual )
    {
        if ( expected == actual )
        {
            return;
        }

        failNotSame( message, expected, actual );
    }

    public static void assertSame( Object expected, Object actual )
    {
        assertSame( null, expected, actual );
    }

    public static void assertNotSame( String message, Object expected, Object actual )
    {
        if ( expected == actual )
        {
            failSame( message );
        }
    }

    public static void assertNotSame( Object expected, Object actual )
    {
        assertNotSame( null, expected, actual );
    }

    static private void failSame( String message )
    {
        String formatted = "";

        if ( message != null )
        {
            formatted = message + " ";
        }

        fail( formatted + "expected not same" );
    }

    static private void failNotSame( String message, Object expected, Object actual )
    {
        String formatted = "";

        if ( message != null )
        {
            formatted = message + " ";
        }

        fail( formatted + "expected same:<" + expected + "> was not:<" + actual + ">" );
    }

    static private void failNotEquals( String message, Object expected, Object actual )
    {
        fail( formatMismatch( message, expected, actual ) );
    }

    static String formatMismatch( String message, Object expected, Object actual )
    {
        String formatted = "";

        if ( message != null )
        {
            formatted = message + " ";
        }

        return formatted + "expected:<" + expected + "> but was:<" + actual + ">";
    }
}
