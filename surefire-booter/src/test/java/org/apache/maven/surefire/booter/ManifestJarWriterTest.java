package org.apache.maven.surefire.booter;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

public class ManifestJarWriterTest
    extends TestCase
{

    File tempFile;

    public void setUp()
        throws Exception
    {
        tempFile = File.createTempFile( "surefirebooter", "jar" );
        tempFile.deleteOnExit();
    }

    public void testWrite()
        throws IOException
    {
        ManifestJarWriter writer = new ManifestJarWriter( tempFile );
        writer.writeValue( "Main-Class", "Foo" );
        writer.close();
    }

    public void tearDown()
    {
        tempFile.delete();
    }

    public static void main( String[] args )
        throws Exception
    {
        ManifestJarWriterTest t = new ManifestJarWriterTest();
        t.setUp();
        t.testWrite();
        System.out.println( t.tempFile.getAbsolutePath() );
    }
}
