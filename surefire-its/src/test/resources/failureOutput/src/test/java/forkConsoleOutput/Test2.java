package forkConsoleOutput;

import org.junit.Test;

public class Test2
{
    @Test
    public void test6281()
    {
        System.out.println( "sout: I am talking to you" );
        System.out.println( "sout: Will Fail soon" );
        System.err.println( "serr: And you too" );
        System.err.println( "serr: Will Fail now" );
        throw new RuntimeException( "FailHere" );
    }
}
