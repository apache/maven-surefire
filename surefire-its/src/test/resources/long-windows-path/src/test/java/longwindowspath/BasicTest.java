package longwindowspath;

import org.junit.Test;

public class BasicTest
{
    @Test
    public void test()
    {
        System.out.println( "SUREFIRE-1400 user.dir="
                                    + System.getProperty( "user.dir" ) );

        System.out.println( "SUREFIRE-1400 surefire.real.class.path="
                                    + System.getProperty( "surefire.real.class.path" ) );
    }

}
