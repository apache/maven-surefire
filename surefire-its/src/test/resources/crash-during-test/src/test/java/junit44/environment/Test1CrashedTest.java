package junit44.environment;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.*;
import org.junit.Test;
import uk.me.mjt.CrashJvm;

public class Test1CrashedTest
{
    @Test
    public void testCrashJvm() throws Exception
    {
        MILLISECONDS.sleep( 1500L );

        assertTrue(CrashJvm.loadedOk());
        
        String crashType = System.getProperty("crashType");
        assertNotNull(crashType);
        if ( crashType.equals( "exit" ) )
        {
            CrashJvm.exit();
        }
        else if ( crashType.equals( "abort" ) )
        {
            CrashJvm.abort();
        }
        else if (crashType.equals( "segfault" ))
        {
            CrashJvm.segfault();
        }
        else
        {
            fail("Don't recognise crashType " + crashType);
        }
    }
}
