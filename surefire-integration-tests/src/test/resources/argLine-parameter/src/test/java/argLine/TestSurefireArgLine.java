package argLine;

import junit.framework.TestCase;

public class TestSurefireArgLine
    extends TestCase
{

    public void testArgLine()
    {
        String fooProperty = System.getProperty( "foo.property" );
        assertEquals( "incorrect foo.property; " +
        		"Surefire should have passed this in correctly",
        		"foo foo/foo/bar/1.0", fooProperty );
        String barProperty = System.getProperty( "bar.property" );
        assertEquals( "incorrect bar.property; " +
        		"Surefire should have passed this in correctly",
        		"bar bar/foo/bar/2.0", barProperty );
    }

}
