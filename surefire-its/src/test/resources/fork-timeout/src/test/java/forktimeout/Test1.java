package forktimeout;

import org.junit.Test;

public class Test1
    extends BaseForkTimeout
{
    @Test
    public void test690()
    {
        dumpStuff( "test690" );
        System.out.println( " with lots of output " );
        System.err.println( "e with lots of output " );
    }
}
