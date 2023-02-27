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
public class SimpleParameterizedTest
{

    private static boolean success;

    public SimpleParameterizedTest( String test )
    {

    }

    @Parameters
    public static List getParameters()
    {
        List parameters = new ArrayList();
        parameters.add( new String[]{ "Test1" } );
        parameters.add( new String[]{ "Test2" } );
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
