package io.olamy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for simple App.
 */
public class App3Test {

    @Test
    public void app3One() throws Throwable {
        Thread.sleep(500);
        assertTrue(true);
    }

    @Test
    public void app3Two() throws Throwable {
        Thread.sleep(7000);
        assertTrue(false);
    }

    @Test
    public void app3Three() throws Throwable {
        Thread.sleep(1000);
        assertTrue(false);
    }

}
