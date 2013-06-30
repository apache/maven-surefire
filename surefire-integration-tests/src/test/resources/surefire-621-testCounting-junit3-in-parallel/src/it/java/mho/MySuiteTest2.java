package mho;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class MySuiteTest2 extends TestCase {

    public static Test suite () {
        TestSuite suite = new TestSuite();

        suite.addTest (new MySuiteTest2("testMe", 1));
        suite.addTest (new MySuiteTest2("testMe", 2));

        return suite;
    }

    private final int number;

    public MySuiteTest2(String name, int number) {
        super (name);
        this.number = number;
    }

    public void testMe() {
        System.out.println ("### "+ this.getClass().getName()+":"+this.getName()+" - number "+number);
        assertTrue (true);
    }
}
