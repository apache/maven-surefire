package testng.testrunnerfactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileHelper
{
    public static void writeFile( String fileName, String content )
    {
        try
        {
            File target = new File( System.getProperty("user.dir"), "target" ).getCanonicalFile();
            File listenerOutput = new File( target, fileName );
            FileWriter out = new FileWriter( listenerOutput, true );
            out.write( content );
            out.flush();
            out.close();
        }
        catch ( IOException exception )
        {
            throw new RuntimeException( exception );
        }
    }
}
