package jiras.surefire1028;

import org.junit.Assert;
import org.junit.Test;

public class SomeTest {

    @Test
    public void test() {
        System.out.println("OK!");
    }

    @Test
    public void filteredOutTest() {
        Assert.fail();
    }
}
