package it;

import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

import junit.framework.TestCase;

public class BasicTest
    extends TestCase
{

    public void testTestClassesBeforeMainClasses()
    {
        Properties props = getProperties( "/surefire-classpath-order.properties" );
        assertEquals( "test-classes", props.getProperty( "Surefire" ) );
    }

    public void testMainClassesBeforeDependencies()
    {
        Properties props = getProperties( "/surefire-report.properties" );
        assertEquals( "classes", props.getProperty( "Surefire" ) );
    }

    private Properties getProperties(String resource)
    {
        InputStream in = getClass().getResourceAsStream( resource );
        assertNotNull( in );
        try
        {
	        Properties props = new Properties();
	        props.load( in );
	        return props;
        }
        catch (IOException e)
        {
            fail(e.toString());
            return null;
        }
    }

}
