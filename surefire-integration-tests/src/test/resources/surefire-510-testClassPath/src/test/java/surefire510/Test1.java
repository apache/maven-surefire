package surefire510;

import junit.framework.TestCase;

import java.io.IOException;

public class Test1
    extends TestCase
{

    public void test1()
        throws IOException
    {
        String tcp = System.getProperty( "surefire.test.class.path" );
        if ( tcp != null )
        {
            System.out.println( "tcp is set" );
        }
    }
}
