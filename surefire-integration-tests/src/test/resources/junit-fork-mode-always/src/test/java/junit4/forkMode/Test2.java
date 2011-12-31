package junit4.forkMode;

import java.io.IOException;

import org.junit.Test;

public class Test2
{

    @Test
    public void test2()
        throws IOException
    {
        Test1.dumpPidFile( "test2" );
    }

}
