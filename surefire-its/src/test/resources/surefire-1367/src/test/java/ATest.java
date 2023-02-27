import org.junit.Test;

import static org.junit.Assume.assumeTrue;

public class ATest {

    @Test
    public void test() {
        System.out.println("Hi");
        System.out.println();
        System.out.println("There!");

        System.err.println("Hello");
        System.err.println();
        System.err.println("What's up!");

        assumeTrue( false );
    }
}
