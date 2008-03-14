package workingDir;

import junit.framework.TestCase;
import java.io.*;
import java.util.Properties;

public class BasicTest
    extends TestCase
{

    public void testWorkingDir() throws Exception {
        File target = new File( "target" ).getAbsoluteFile();
        File outFile = new File( target, "out.txt" );
        FileOutputStream os = new FileOutputStream(outFile);
        String userDir = System.getProperty("user.dir");
        Properties p = new Properties();
        p.setProperty( "user.dir", userDir );
        p.store( os, "" );
        os.flush();
        os.close();
    }

}
