package de.scrum_master.dummy;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class MyTest {
    @Test
    public void test() {
        System.out.println("[Test OUT] Hello Maven!");
        System.err.println("[Test ERR] Hello Maven!");
        assertTrue(true);
    }
}
