package mho;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Created by IntelliJ IDEA.
 * User: yyit927b
 * Date: 26.05.2010
 * Time: 11:27:42
 * To change this template use File | Settings | File Templates.
 */
public class MySuiteTest3 extends TestCase {

    public static Test suite () {
        TestSuite suite = new TestSuite();

        suite.addTest (new MySuiteTest3("testMe", 1));
        suite.addTest (new MySuiteTest3("testMe", 2));
        suite.addTest (new MySuiteTest3("testMe", 3));

        return suite;
    }

    private int number;

    public MySuiteTest3(String name, int number) {
        super (name);
        this.number = number;
    }

    public void testMe() {
        System.out.println ("### "+ this.getClass().getName()+":"+this.getName()+" - number "+number);
        assertTrue (true);
    }
}
