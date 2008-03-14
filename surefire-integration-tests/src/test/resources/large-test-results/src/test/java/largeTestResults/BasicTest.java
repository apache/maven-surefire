package largeTestResults;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class BasicTest
    extends TestCase
{

    private int number;

    public BasicTest( String name , int number)
    {
        super( name );
        this.number = number;
    }

    public static Test suite()
    {
        int tests = Integer.parseInt(System.getProperty("numTests", "20"));
        TestSuite suite = new TestSuite();
        for (int i = 0; i < tests; i++)
        {
            if ( i % 4 == 0)
            {
                suite.addTest( new BasicTest( "testPass", i ) );
            }
            else
            {
                suite.addTest( new BasicTest( "testFail", i ) );
            }
        }
        return suite;
    }

    public void testFail() {
        fail( "failure " + number );
    }
    
    public void testPass() {}

}
