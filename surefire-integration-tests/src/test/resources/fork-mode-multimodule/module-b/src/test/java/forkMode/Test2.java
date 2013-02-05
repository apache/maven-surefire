package forkMode;

import java.io.IOException;

import junit.framework.TestCase;

public class Test2
    extends TestCase
{

    public void test2() throws IOException, InterruptedException {
        int sleepLength = Integer.valueOf( System.getProperty( "sleepLength", "1500" ));
        Thread.sleep(sleepLength);
        Test1.dumpPidFile(this);
    }

}
