import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SurefireTest3
    extends TestCase
{

    public SurefireTest3( )
    {
        super( );
    }

    public SurefireTest3( String name )
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
