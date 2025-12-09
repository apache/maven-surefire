package org.test;

import java.io.FileOutputStream;
import org.junit.Test;

public class NotIncludedByDefault
{
    @Test
    public void testRun()
        throws Exception
    {
        try ( FileOutputStream fout = new FileOutputStream( "target/testTouchFile.txt" ) )
        {
            fout.write( '!' );
        }
    }
}
