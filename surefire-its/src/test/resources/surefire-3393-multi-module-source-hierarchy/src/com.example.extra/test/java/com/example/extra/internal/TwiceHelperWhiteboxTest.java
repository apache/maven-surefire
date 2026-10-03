package com.example.extra.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Whitebox test accessing the non-exported internal package of com.example.extra.
 */
class TwiceHelperWhiteboxTest {
    @Test
    void testTwice() {
        assertEquals(0L, TwiceHelper.twice(0L));
        assertEquals(42L, TwiceHelper.twice(21L));
    }
}
