package resultcounting;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class MySuiteTest2 extends TestCase {

    public static Test suite () {
        TestSuite suite = new TestSuite();

        suite.addTest (new MySuiteTest2("testMe" ));
        suite.addTest (new MySuiteTest2("testMe" ));

        return suite;
    }

    public MySuiteTest2( String name ) {
        super (name);
    }

    public void testMe() {
        assertTrue (true);
    }
}
