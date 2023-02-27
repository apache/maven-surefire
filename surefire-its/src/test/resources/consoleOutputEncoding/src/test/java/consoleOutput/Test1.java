package consoleOutput;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;

public class Test1
{
    @Test
    public void testSystemOut()
        throws IOException
    {
        PrintStream out = System.out;
        out.print( getS( "print" ));
        out.write( getS( "utf-8" ).getBytes( Charset.forName( "UTF-8" ) ) );
        out.write( getS( "8859-1" ).getBytes( Charset.forName( "ISO-8859-1" ) ) );
        out.write( getS( "utf-16" ).getBytes( Charset.forName( "UTF-16" ) ) );
    }

    private String getS( String s )
    {
        return " Hell\u00d8 " + s + "\n";
    }
}
