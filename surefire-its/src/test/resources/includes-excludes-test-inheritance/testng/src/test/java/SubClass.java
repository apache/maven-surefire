package testng;

import org.testng.annotations.Test;

public class SubClass extends SuperClass {

    @Test
    public void testInSubClass() {
        System.out.println("TestNG: SubClass#testInSubClass executed");
    }

}
