package io.olamy;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit test for simple App.
 */
public class App1Test {

    @Test
    public void app1One() throws Throwable {
        Thread.sleep(2000);
        assertTrue(true);
    }

    @Test
    public void app1Two() throws Throwable {
        Thread.sleep(2000);
        assertTrue(true);
    }

    @Test
    public void app1Three() throws Throwable {
        Thread.sleep(2000);
        assertTrue(false);
    }

}
