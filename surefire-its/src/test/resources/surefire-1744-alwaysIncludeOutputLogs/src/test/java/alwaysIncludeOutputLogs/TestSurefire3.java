package alwaysIncludeOutputLogs;

import org.junit.Test;
import org.apache.log4j.Logger;
public class TestSurefire3
{
    @Test
    public void successfulTestWithLog()
    {

        Logger.getLogger( alwaysIncludeOutputLogs.TestSurefire3.class )
            .info( "Log output should be included in the report" );
        System.out.println( "System-out output should be included in the report" );
    }
}
