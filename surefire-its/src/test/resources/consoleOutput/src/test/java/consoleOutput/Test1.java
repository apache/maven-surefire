package consoleOutput;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import static org.junit.Assert.fail;

public class Test1
{
    public Test1()
    {
       System.out.println("In constructor");
    }

    @Before
    public void t1()
    {
        System.out.println( "t1 = " + System.currentTimeMillis() );
    }

    @After
    public void t2()
    {
        System.out.println( "t2 = " + System.currentTimeMillis() );
    }

    @Test
    public void testStdOut()
    {
        char c = 'C';
        System.out.print( "Sout" );
        System.out.print( "Again" );
        System.out.print( "\n" );
        System.out.print( c );
        System.out.println( "SoutLine" );
        System.out.println( "äöüß" );
        System.out.println( "" );
        System.out.println( "==END==" );

        fail( "failing with ü" );
    }
        
    @Test
    public void testStdErr()
    {
        char e = 'E';
        System.err.print( "Serr" );
        System.err.print( "\n" );
        System.err.print( e );
        System.err.println( "SerrLine" );
        System.err.println( "äöüß" );
        System.err.println( "" );
        System.err.println( "==END==" );
    }
}
