package forkCount;

import java.io.IOException;

import junit.framework.TestCase;

public class Test3
    extends TestCase
{

    public void test3() throws Exception {
        int sleepLength = Integer.valueOf( System.getProperty( "sleepLength", "750" ) );
        Thread.sleep( sleepLength );
        Test1.dumpPidFile(this);
    }

}
