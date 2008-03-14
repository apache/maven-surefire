package forkMode;

import java.io.IOException;

import junit.framework.TestCase;

public class Test2
    extends TestCase
{

    public void test2() throws IOException {
        Test1.dumpPidFile(this);
    }

}
