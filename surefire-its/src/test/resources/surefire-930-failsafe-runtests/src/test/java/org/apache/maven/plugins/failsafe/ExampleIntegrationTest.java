package org.apache.maven.plugins.failsafe;

import org.junit.Assert;
import org.testng.annotations.Test;

@Test(groups = { TestConstants.IntegrationTest })
public class ExampleIntegrationTest {

    public void shouldRun() {
        System.out.println("Hello from Integration-Test");
        Assert.fail("this will not be executed");
    }
}
