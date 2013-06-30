package forkMode;

import java.io.IOException;

import junit.framework.TestCase;

public class Test3
    extends TestCase
{

    public void test3() throws IOException {
        Test1.dumpPidFile(this);
    }

}
