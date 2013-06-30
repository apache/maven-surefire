package resultcounting;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class MySuiteTest3 extends TestCase {

    public static Test suite () {
        TestSuite suite = new TestSuite();

        suite.addTest (new MySuiteTest3("testMe" ));
        suite.addTest (new MySuiteTest3("testMe" ));
        suite.addTest (new MySuiteTest3("testMe" ));

        return suite;
    }

    public MySuiteTest3( String name ) {
        super (name);
    }

    public void testMe() {
        assertTrue (true);
    }
}
