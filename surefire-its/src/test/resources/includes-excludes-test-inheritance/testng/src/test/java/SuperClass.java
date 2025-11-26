package testng;

import org.testng.annotations.Test;

public abstract class SuperClass {

    @Test
    public void testInSuperClass() {
        System.out.println("TestNG: SuperClass#testInSuperClass executed");
    }

}
