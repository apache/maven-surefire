package org.apache.maven.plugins.failsafe;

import org.junit.Assert;
import org.testng.annotations.Test;

@Test(groups = { TestConstants.UnitTest })
public class ExampleTest {

    public void shouldRun() {
        System.out.println("Hello from Unit-Test");
        Assert.assertTrue(true);
    }
}
