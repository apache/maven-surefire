package org.test;

import java.io.FileOutputStream;

public class NotIncludedByDefault
{
    public void testRun()
        throws Exception
    {
        try ( FileOutputStream fout = new FileOutputStream( "target/testTouchFile.txt" ) )
        {
            fout.write( '!' );
        }
    }
}
