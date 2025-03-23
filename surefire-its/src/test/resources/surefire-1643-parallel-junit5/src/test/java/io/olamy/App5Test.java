package io.olamy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for simple App.
 */
public class App5Test {

    @Test
    public void app5One() throws Throwable {
        Thread.sleep(500);
        assertTrue(true);
    }

    @Test
    public void app5Two() throws Throwable {
        Thread.sleep(200);
        assertTrue(false);
    }

    @Test
    public void app5Three() throws Throwable {
        Thread.sleep(1000);
        assertTrue(true);
    }

}
