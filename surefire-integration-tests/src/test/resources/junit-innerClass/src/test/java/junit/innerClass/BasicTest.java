package junit.innerClass;
import junit.framework.TestCase;

public class BasicTest
    extends TestCase
{

    public void testFoo() {
        new Foo("x", "y");
    }
    
    public class Foo {
        public Foo(String x, String y) {};
    }
}
