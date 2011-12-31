package test.surefiretoolchains;

import org.junit.Test;

import static org.junit.Assert.*;

public class AppTest
{

    @Test
    public void testApp()
    {
        // 1.5.0_19-b02
        assertEquals( "1.5.0_19", System.getProperty( "java.version" ) );
    }

}
