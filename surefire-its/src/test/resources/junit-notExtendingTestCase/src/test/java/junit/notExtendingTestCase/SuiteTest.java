package junit.notExtendingTestCase;

import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;


public class SuiteTest
    extends TestSuite
{
    public static Test suite() {
        SuiteTest suite = new SuiteTest();
        suite.addTest( new Test() {

            public int countTestCases()
            {
                System.out.println( this.getClass().getName() + "#countTestCases" );
                return 1;
            }

            public void run( TestResult result )
            {
                System.out.println( this.getClass().getName() + "#run" );
                result.startTest( this );
                result.endTest( this );
                
            }
            
        } );
        return suite;
    }
}
