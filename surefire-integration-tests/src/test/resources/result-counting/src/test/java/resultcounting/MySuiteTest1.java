package resultcounting;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class MySuiteTest1 extends TestCase {

    public static Test suite () {
        TestSuite suite = new TestSuite();

        suite.addTest (new MySuiteTest1("testMe" ));

        return suite;
    }

    public MySuiteTest1( String name ) {
        super (name);
    }

    public void testMe() {
        assertTrue (true);
    }
}
