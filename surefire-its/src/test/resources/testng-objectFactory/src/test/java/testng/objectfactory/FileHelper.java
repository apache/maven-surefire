package testng.objectfactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileHelper
{
    public static void writeFile( String fileName, String content )
    {
        try
        {
            File target = new File( System.getProperty( "user.dir" ), "target" ).getCanonicalFile();
            File listenerOutput = new File( target, fileName );
            try (FileWriter out = new FileWriter( listenerOutput, true )  )
            {
                out.write( content );
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
}
