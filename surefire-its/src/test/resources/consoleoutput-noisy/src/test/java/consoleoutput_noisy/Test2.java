package consoleoutput_noisy;

import junit.framework.TestCase;

public class Test2
    extends TestCase
{
    public void test2MillionBytes()
    {
        for ( int i = 0; i < 20 * Test1.thousand; i++ )
        {
            System.out.println(
                "0-2-3-6-8-012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" );
        }
    }

    public static void testHundredThousand()
    {
        for ( int i = 0; i < Test1.thousand; i++ )
        {
            System.out.println(
                "A-A-3-A-A-BBBBBBBBBBCCCCCCCCCCDDDDDDDDDDEEEEEEEEEEFFFFFFFFFFGGGGGGGGGGHHHHHHHHHHIIIIIIIIIIJJJJJJJJJJ" );
        }
    }

    public static void testAnotherHundredThousand()
    {
        for ( int i = 0; i < Test1.thousand; i++ )
        {
            System.out.println(
                "A-A-A-3-3-ABBBBBBBBBCCCCCCCCCCDDDDDDDDDDEEEEEEEEEEFFFFFFFFFFGGGGGGGGGGHHHHHHHHHHIIIIIIIIIIJJJJJJJJJJ" );
        }
    }
}
