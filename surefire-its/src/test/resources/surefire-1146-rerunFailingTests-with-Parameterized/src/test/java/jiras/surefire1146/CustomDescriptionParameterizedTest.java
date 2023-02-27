package jiras.surefire1146;

import java.util.ArrayList;
import java.util.List;

import junit.runner.Version;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.*;

@RunWith( Parameterized.class )
public class CustomDescriptionParameterizedTest
{

    private static boolean success;

    public CustomDescriptionParameterizedTest( String test1, String test2, String test3 )
    {

    }

    @Parameters( name = "{index}: ({0}); {1}; {2};" )
    public static List getParameters()
    {
        List parameters = new ArrayList();
        parameters.add( new String[]{ "Test11", "Test12", "Test13" } );
        parameters.add( new String[]{ "Test21", "Test22", "Test23" } );
        return parameters;
    }

    @Test
    public void flakyTest()
    {
        System.out.println( "Running JUnit " + Version.id() );
        boolean current = success;
        success = !success;
        assertTrue( current );
    }

}
