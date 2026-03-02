package junit;

import org.junit.jupiter.api.Test;

public class OtherClass {

    @Test
    public void testIncluded() {
        System.out.println("JUnit: OtherClass#testIncluded executed");
    }

    @Test
    public void testNotIncluded() {
        System.out.println("JUnit: OtherClass#testNotIncluded executed");
    }

}
