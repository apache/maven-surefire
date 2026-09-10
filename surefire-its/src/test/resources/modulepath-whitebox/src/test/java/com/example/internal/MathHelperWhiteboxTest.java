package com.example.internal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Whitebox test that accesses the non-exported internal package.
 * This works because surefire patches test classes into the module via --patch-module.
 */
class MathHelperWhiteboxTest {
    @Test
    void testAdd() {
        assertEquals(0L, MathHelper.add(0L, 0L));
        assertEquals(42L, MathHelper.add(21L, 21L));
    }

    @Test
    void testMultiply() {
        assertEquals(0L, MathHelper.multiply(0L, 1L));
        assertEquals(42L, MathHelper.multiply(2L, 21L));
    }
}
