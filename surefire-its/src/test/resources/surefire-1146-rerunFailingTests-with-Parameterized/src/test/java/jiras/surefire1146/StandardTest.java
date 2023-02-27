package jiras.surefire1146;

import junit.runner.Version;
import org.junit.Test;

import static org.junit.Assert.*;

public class StandardTest
{
    private static boolean success;

    @Test
    public void flakyTest()
    {
        System.out.println( "Running JUnit " + Version.id() );
        boolean current = success;
        success = !success;
        assertTrue( current );
    }

}
