package systemProperties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import java.io.File;

public class BasicTest
    extends TestCase
{


    public void testSetInPom()
    {
        assertEquals("property setInPom not set", "foo", System.getProperty("setInPom"));
    }
    
    public void testSetOnArgLine()
    {
        assertEquals("setOnArgLine property not set", "bar", System.getProperty("setOnArgLine"));
    }

    
    public void testSystemPropertyUsingMavenProjectProperties()
    {
        String actualBuildDirectory = new File( System.getProperty( "basedir" ),"target" ).getAbsolutePath();
        
        String buildDirectoryFromPom = new File( System.getProperty( "buildDirectory") ).getAbsolutePath();
        
        assertEquals( "Pom property not set.", actualBuildDirectory, buildDirectoryFromPom );
    }
    
    public void testSystemPropertyGenerateByOtherPlugin()
        throws Exception
    {
        int  reservedPort1 = Integer.parseInt( System.getProperty(  "reservedPort1" ) );
        int  reservedPort2 = Integer.parseInt( System.getProperty(  "reservedPort2" ) );
        int  reservedPort3 = Integer.parseInt( System.getProperty(  "reservedPort3" ) );
        System.out.println( "reservedPort1: " + reservedPort1 );
        System.out.println( "reservedPort2: " + reservedPort2 );
        System.out.println( "reservedPort3: " + reservedPort3 );
        
        assertTrue( reservedPort1 != reservedPort2 );
        
        //plugin cannot overwrite the default value set in the pom, this is maven bug
        assertEquals( 1, reservedPort3 );
        
    }
    
    
// SUREFIRE-121; someday we should re-enable this    
//    public void testSetOnMavenCommandLine()
//    {
//        assertEquals("property setOnMavenCommandLine not set", "baz", System.getProperty("setOnMavenCommandLine"));
//    }
    
}
