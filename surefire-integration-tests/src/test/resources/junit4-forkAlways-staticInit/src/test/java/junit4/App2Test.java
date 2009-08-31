package junit4;

import java.util.Properties;

import junit.framework.TestCase;

/**
 * Unit test for simple App.
 */
public class App2Test 
    extends TestCase
{
    
    static
    {
        System.out.println( "Loading " + App2Test.class.getName() );
        
        Properties p = System.getProperties();
        p.setProperty( "Foo", "Bar2" );
        System.setProperties( p );
    }
    
    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        System.out.println( "Expecting: Bar2\nGot: " + System.getProperty( "Foo" ) );
        assertEquals( "Expecting: Bar2\nGot: " + System.getProperty( "Foo" ), "Bar2", System.getProperty( "Foo" ) );
    }
}
