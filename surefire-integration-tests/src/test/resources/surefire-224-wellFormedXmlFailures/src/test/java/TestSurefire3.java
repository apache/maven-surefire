import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestSurefire3
    extends TestCase
{

    public TestSurefire3( )
    {
        super( );
    }

    public TestSurefire3( String name )
    {
        super( name );
    }


    public void testQuote()
    {
        fail( "\"" );
    }

    public void testLower()
    {
        fail( "<" );
    }

    public void testGreater()
    {
        fail( ">" );
    }

}
