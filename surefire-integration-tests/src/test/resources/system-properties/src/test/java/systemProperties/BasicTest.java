package systemProperties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class BasicTest
    extends TestCase
{


    public void testFoo()
    {
        assertEquals("foo property not set", "foo", System.getProperty("foo"));
    }
    
    public void testBar()
    {
        assertEquals("bar property not set", "bar", System.getProperty("bar"));
    }
    
    public void testBaz()
    {
        assertEquals("baz property not set", "baz", System.getProperty("baz"));
    }
    
}
