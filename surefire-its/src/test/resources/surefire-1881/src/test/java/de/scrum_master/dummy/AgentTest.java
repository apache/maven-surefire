package de.scrum_master.dummy;

import org.junit.Test;

import java.io.PrintStream;
import java.net.URL;

public class AgentTest
{
    @Test
    public void test()
    {
        for ( int i = 0; i < 5000; i++ )
        {
            System.out.println( "[Test OUT] Hello Maven!" );
            System.err.println( "[Test ERR] Hello Maven!" );
        }
    }
}
