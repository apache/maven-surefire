package forkCount;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Random;

import org.testng.annotations.Test;

public class Test1
{

	private static final Random RANDOM = new Random();
	
    @Test
    public void test1()
        throws IOException, InterruptedException
    {
        int sleepLength = Integer.valueOf( System.getProperty( "sleepLength", "750" ));
        Thread.sleep(sleepLength);
        dumpPidFile( "test1" );
    }

    public static void dumpPidFile( String name )
        throws IOException
    {
        String fileName = name + "-pid";
        File target = new File( "target" ).getCanonicalFile();
        if ( !( target.exists() && target.isDirectory() ) )
        {
            target = new File( "." );
        }
        File pidFile = new File( target, fileName );
        try ( FileWriter fw = new FileWriter( pidFile ) )
        {
            // DGF little known trick... this is guaranteed to be unique to the PID
            // In fact, it usually contains the pid and the local host name!
            String pid = ManagementFactory.getRuntimeMXBean().getName();
            fw.write( pid );
            fw.write( " " );
            fw.write( System.getProperty( "testProperty", String.valueOf( RANDOM.nextLong() ) ) );
        }
    }
}
