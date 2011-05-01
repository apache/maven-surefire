package mho;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class MySuiteTest3 extends TestCase {

    public static Test suite () {
        TestSuite suite = new TestSuite();

        suite.addTest (new MySuiteTest3("testMe", 1));
        suite.addTest (new MySuiteTest3("testMe", 2));
        suite.addTest (new MySuiteTest3("testMe", 3));

        return suite;
    }

    private final int number;

    public MySuiteTest3(String name, int number) {
        super (name);
        this.number = number;
    }

    public void testMe() {
        System.out.println ("### "+ this.getClass().getName()+":"+this.getName()+" - number "+number);
        assertTrue (true);
    }
}
