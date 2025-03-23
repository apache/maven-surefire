package io.olamy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for simple App.
 */
public class App2Test {

    @Test
    public void app2One() throws Throwable {
        Thread.sleep(3000);
        assertTrue(true);
    }

    @Test
    public void app2Two() throws Throwable {
        Thread.sleep(1000);
        assertTrue(true);
    }

    @Test
    public void app2Three() throws Throwable {
        Thread.sleep(2000);
        assertTrue(true);
    }

}
