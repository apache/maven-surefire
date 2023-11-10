package additionalClasspath;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;

import junit.framework.TestCase;

public class BasicTest
    extends TestCase
{

    public void testExtraResource()
    {
        assertNotNull( BasicTest.class.getResourceAsStream( "/test.txt" ) );
        assertNotNull( getClass().getClassLoader().getResourceAsStream( "test.txt" ) );
        assertNotNull( BasicTest.class.getResourceAsStream( "/test2.txt" ) );
        assertNotNull( getClass().getClassLoader().getResourceAsStream( "test2.txt" ) );
    }

    public void testExtraResourceOrder() throws IOException
    {
        Enumeration<URL> resources = getClass().getClassLoader().getResources("order-test.txt");
        assertTrue( resources.hasMoreElements() );
        URL url = resources.nextElement();
        InputStream is = url.openStream();
        assertNotNull( is );
        try ( InputStream i = is; InputStreamReader r = new InputStreamReader(is); BufferedReader br = new BufferedReader(r) ) {
            assertEquals("1", br.readLine());
        }
        assertTrue( resources.hasMoreElements() );
        url = resources.nextElement();
        is = url.openStream();
        assertNotNull( is );
        try ( InputStream i = is; InputStreamReader r = new InputStreamReader(is); BufferedReader br = new BufferedReader(r) ) {
            assertEquals("2", br.readLine());
        }
        assertFalse( resources.hasMoreElements() );
    }
}
