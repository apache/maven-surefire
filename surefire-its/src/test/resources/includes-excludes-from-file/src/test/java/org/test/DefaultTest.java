package org.test;

import java.io.FileOutputStream;

public class DefaultTest
{
    public void testRun()
        throws Exception
    {
        try ( FileOutputStream fout = new FileOutputStream( "target/defaultTestTouchFile.txt" ) )
        {
            fout.write( '!' );
        }
    }
}
