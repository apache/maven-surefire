package consoleoutput_noisy;

import org.junit.Test;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public class Test1
{

    public static final int thousand = Integer.parseInt( System.getProperty( "thousand", "1000" ) );

    @Test
    public void test1MillionBytes()
    {
        System.out.println( "t1 = " + System.currentTimeMillis() );
        for ( int i = 0; i < ( 10 * thousand ); i++ )
        {
            System.out.println( "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" );
        }
        System.out.println( "t2 = " + System.currentTimeMillis() );
    }

    @Test
    public void testHundredThousand()
    {
        printAlot();
    }

    private static void printAlot()
    {
        for ( int i = 0; i < thousand; i++ )
        {
            System.out.println( "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDDEEEEEEEEEEFFFFFFFFFFGGGGGGGGGGHHHHHHHHHHIIIIIIIIIIJJJJJJJJJJ" );
        }
    }

    @Test
    public void testAnotherHundredThousand()
    {
        printAlot();
    }

    @Before
    public void before()
    {
        printAlot();
    }

    @BeforeClass
    public static void beforeClass()
    {
        printAlot();
    }

    @After
    public void after()
    {
        printAlot();
    }

    @AfterClass
    public static void afterClass()
    {
        printAlot();
    }
}
