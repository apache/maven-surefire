package forkCount;

import java.io.IOException;

import org.testng.annotations.Test;

public class Test3
{

    @Test
    public void test3() throws IOException {
        Test1.dumpPidFile( "test3" );
    }

}
