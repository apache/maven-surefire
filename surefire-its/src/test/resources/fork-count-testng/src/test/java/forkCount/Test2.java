package forkCount;

import java.io.IOException;

import org.testng.annotations.Test;

public class Test2
{
    @Test
    public void test2() throws IOException {
        Test1.dumpPidFile( "test2" );
    }

}
