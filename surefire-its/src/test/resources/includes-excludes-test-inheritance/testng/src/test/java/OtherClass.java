package testng;

import org.testng.annotations.Test;

public class OtherClass {

    @Test
    public void testIncluded() {
        System.out.println("TestNG: OtherClass#testIncluded executed");
    }

    @Test
    public void testNotIncluded() {
        System.out.println("TestNG: OtherClass#testNotIncluded executed");
    }

}
