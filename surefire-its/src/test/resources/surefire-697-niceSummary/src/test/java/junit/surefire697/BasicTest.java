package junit.surefire697;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class BasicTest
    extends TestCase
{

    public void testShort()
    {
        throw new RuntimeException( "A very short message" );
    }

    public void testShortMultiline()
    {
        throw new RuntimeException( "A very short multiline message\nHere is line 2" );
    }

    public void testLong()
    {
        throw new RuntimeException( "A very long single line message"
            + "012345678900123456789001234567890012345678900123456789001234567890"
            + "012345678900123456789001234567890012345678900123456789001234567890" );
    }

    public void testLongMultiLineNoCr()
    {
        throw new RuntimeException( "A very long multi line message"
            + "012345678900123456789001234567890012345678900123456789001234567890"
            + "012345678900123456789001234567890012345678900123456789001234567890\n"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ" );
    }

    public void testLongMultiLine()
    {
        throw new RuntimeException( "A very long single line message"
            + "012345678900123456789001234567890012345678900123456789001234567890"
            + "012345678900123456789001234567890012345678900123456789001234567890\n"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ\n" );
    }
}
