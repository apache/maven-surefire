package junit4.forkMode;

import java.io.IOException;

import org.junit.Test;

public class Test3
{

    @Test
    public void test3()
        throws IOException
    {
        Test1.dumpPidFile( "test3" );
    }

}
