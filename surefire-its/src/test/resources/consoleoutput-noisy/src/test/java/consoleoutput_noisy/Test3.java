package consoleoutput_noisy;

import org.junit.Test;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.File;

/**
 *
 */
public class Test3
{
    @Test
    public void test() throws Exception
    {
        long t1 = System.currentTimeMillis();
        System.out.println( "t1 = " + t1 );
        for ( int i = 0; i < 320_000; i++ )
        {
            System.out.println( "01234567890123456789012345678901234567890123456789"
                + "01234567890123456789012345678901234567890123456789" );
        }
        long t2 = System.currentTimeMillis();
        System.out.println( "t2 = " + t2 );

        File target = new File( System.getProperty( "user.dir" ) );
        new File( target, ( t2 - t1 ) + "" )
            .createNewFile();
    }
}
