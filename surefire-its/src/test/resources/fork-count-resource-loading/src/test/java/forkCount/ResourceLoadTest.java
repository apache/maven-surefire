package forkCount;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class ResourceLoadTest
    extends TestCase
{

    public void testGetResourceUrl() throws IOException {
        final URL resource = this.getClass().getClassLoader().getResource( "myFile.txt" );
        assertNotNull(  resource );
    }

    public void testGetResource() throws IOException {
        final InputStream resource = this.getClass().getClassLoader().getResourceAsStream( "myFile.txt" );
        assertNotNull(  resource );
    }

    public void testGetResourceThreadLoader() throws IOException {
        final InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream( "myFile.txt" );
        assertNotNull(  resource );
    }
}
