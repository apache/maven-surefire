package testng.testrunnerfactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class FileHelper
{
    public static void writeFile( String fileName, String content )
    {
        Writer writer = null;
        try
        {
            writer = new FileWriter( new File( new File( System.getProperty( "user.dir" ),
                                                         "target" ).getCanonicalFile(), fileName ), true );

            writer.write( content );
            writer.close();
            writer = null;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
        finally
        {
            if ( writer != null )
            {
                try
                {
                    writer.close();
                }
                catch ( IOException e )
                {
                    // Suppressed, so that the exception thrown in the try block will be propagated.
                }
            }
        }
    }
}
