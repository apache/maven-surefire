package forkCount;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Random;

import junit.framework.TestCase;

public class Test1
    extends TestCase
{

    private static final Random RANDOM = new Random();

    public void test1()
        throws Exception
    {
        int sleepLength = Integer.valueOf( System.getProperty( "sleepLength", "750" ) );
        Thread.sleep( sleepLength );
        dumpPidFile( this );
    }

    public static void dumpPidFile( TestCase test )
        throws IOException
    {
        String fileName = test.getName() + "-pid";
        File target = new File( "target" ).getCanonicalFile();  // getCanonicalFile required for embedded mode
        if ( !target.exists() )
        {
            target.mkdirs();
        }
        File pidFile = new File( target, fileName );
        if ( pidFile.exists() )
        {
            pidFile.delete();
        }

        try ( FileWriter fw = new FileWriter( pidFile ) )
        {
            // DGF little known trick... this is guaranteed to be unique to the PID
            // In fact, it usually contains the pid and the local host name!
            String pid = ManagementFactory.getRuntimeMXBean().getName();
            fw.write( pid );
            fw.write( " " );
            fw.write( System.getProperty( "testProperty", String.valueOf( RANDOM.nextLong() ) ) );
            System.out.println( "Done Writing pid file" + pidFile.getAbsolutePath() );
        }
    }
}
