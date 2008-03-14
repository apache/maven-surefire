package systemProperties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class BasicTest
    extends TestCase
{


    public void testSetInPom()
    {
        assertEquals("property setInPom not set", "foo", System.getProperty("setInPom"));
    }
    
    public void testSetOnArgLine()
    {
        assertEquals("setOnArgLine property not set", "bar", System.getProperty("setOnArgLine"));
    }
    
    public void testSetOnMavenCommandLine()
    {
        assertEquals("property setOnMavenCommandLine not set", "baz", System.getProperty("setOnMavenCommandLine"));
    }
    
}
