package junit;

import org.junit.jupiter.api.Test;

public class SubClass extends SuperClass {

    @Test
    public void testInSubClass() {
        System.out.println("JUnit: SubClass#testInSubClass executed");
    }

}
