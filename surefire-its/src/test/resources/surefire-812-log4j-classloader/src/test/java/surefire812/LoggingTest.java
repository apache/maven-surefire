package surefire812;

import org.apache.log4j.Logger;

import junit.framework.TestCase;

public class LoggingTest extends TestCase
{
    //static {
//        Logger.getLogger( LoggingTest.class ).debug( "fud");
//    }

    public void testCanLogAMessage()
    {
        Logger.getLogger( LoggingTest.class ).warn( "hey you" );
    }
}
