package listenReport;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileHelper
{
    public static void writeFile(String fileName, String content)
    {
        try
        {
            File target = new File( "target" ).getAbsoluteFile();
            File listenerOutput = new File( target, fileName );
            FileWriter out = new FileWriter(listenerOutput);
            out.write( content );
            out.flush();
            out.close();
        }
        catch ( IOException e )
        {
            throw new RuntimeException(e);
        }
    }
}
