package junit4;

import java.util.Properties;

import junit.framework.TestCase;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    
    static
    {
        System.out.println( "Loading " + AppTest.class.getName() );
        
        Properties p = System.getProperties();
        p.setProperty( "Foo", "Bar" );
        System.setProperties( p );
    }
    
    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        System.out.println( "Expecting: Bar\nGot: " + System.getProperty( "Foo" ) );
        assertEquals( "Expecting: Bar\nGot: " + System.getProperty( "Foo" ), "Bar", System.getProperty( "Foo" ) );
    }
}
