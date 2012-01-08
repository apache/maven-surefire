package surefire812;

import org.apache.log4j.Logger;

public class TestLogging
{
    static {
        Logger.getLogger( TestLogging.class ).debug( "fud");
    }

    public void testCanLogAMessage()
    {
        Logger.getLogger( TestLogging.class ).warn( "hey you" );
    }
}
