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
        int  reservedPort1 = Integer.parseInt( System.getProperty( "reservedPort1" ) );
        int  reservedPort2 = Integer.parseInt( System.getProperty( "reservedPort2" ) );
        System.out.println( "reservedPort1: " + reservedPort1 );
        System.out.println( "reservedPort2: " + reservedPort2 );
        
        assertTrue( reservedPort1 != reservedPort2 );
    }
    
    public void testEmptySystemProperties()
    {
        assertNull( "Null property is not null", System.getProperty( "nullProperty" ) );
        assertNull( "Blank property is not null", System.getProperty( "blankProperty" ) );
    }    

    /**
     * work around for SUREFIRE-121
     */
    public void testSetOnArgLineWorkAround()
    {
        assertEquals("property setOnArgLineWorkAround not set", "baz", System.getProperty( "setOnArgLineWorkAround" ) );
    }
    
    public void testSetOnMavenCommandLine()
    {
        assertEquals("property setOnMavenCommandLine not set", "baz", System.getProperty("setOnMavenCommandLine"));
    }
    
}
