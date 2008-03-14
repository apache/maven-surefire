package aggregateReport;

import junit.framework.TestCase;

public class FailingTest
    extends TestCase
{

    public void testFailure()
    {
        fail( "This test is supposed to fail" );
    }

}
