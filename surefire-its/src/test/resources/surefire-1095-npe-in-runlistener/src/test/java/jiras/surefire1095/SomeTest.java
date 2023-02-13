package jiras.surefire1095;

import junit.runner.Version;
import org.junit.Test;

public class SomeTest {
    @Test
    public void test()
    {
        System.out.println( "Running JUnit " + Version.id() );
    }
}
