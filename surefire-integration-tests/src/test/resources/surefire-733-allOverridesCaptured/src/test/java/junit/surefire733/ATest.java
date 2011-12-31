package junit.surefire733;

import junit.framework.TestCase;

public class ATest
    extends TestCase
{
    public void testConsoleOut()
    {
        System.out.write( (int) 'a' );
        final byte[] bytes = "bc".getBytes();
        System.out.write( bytes, 0, bytes.length );
        System.out.write( '\n' );
        System.out.println( "ABC" );
        System.out.println( (String) null );

        final byte[] errbytes = "ef".getBytes();
        System.err.write( (int) 'z' );
        System.err.write( errbytes, 0, bytes.length );
        System.err.write( '\n' );

        System.err.println( "XYZ" );
        System.err.println( (String) null );

    }
}
