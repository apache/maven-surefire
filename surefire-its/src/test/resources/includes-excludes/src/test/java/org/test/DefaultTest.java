package org.test;

import java.io.FileOutputStream;
import org.junit.Test;

public class DefaultTest
{
    @Test
    public void testRun()
        throws Exception
    {
        try ( FileOutputStream fout = new FileOutputStream( "target/defaultTestTouchFile.txt" ) )
        {
            fout.write( '!' );
        }
    }
}
