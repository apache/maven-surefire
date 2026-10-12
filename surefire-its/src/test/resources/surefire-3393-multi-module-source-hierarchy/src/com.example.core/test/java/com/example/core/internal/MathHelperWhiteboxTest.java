package com.example.core.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Whitebox test accessing the non-exported internal package of com.example.core.
 */
class MathHelperWhiteboxTest {
    @Test
    void testAdd() {
        assertEquals(0L, MathHelper.add(0L, 0L));
        assertEquals(42L, MathHelper.add(21L, 21L));
    }
}
