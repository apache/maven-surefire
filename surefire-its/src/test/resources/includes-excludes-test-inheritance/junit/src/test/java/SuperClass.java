package junit;

import org.junit.jupiter.api.Test;

public abstract class SuperClass {

    @Test
    public void testInSuperClass() {
        System.out.println("JUnit: SuperClass#testInSuperClass executed");
    }

}
