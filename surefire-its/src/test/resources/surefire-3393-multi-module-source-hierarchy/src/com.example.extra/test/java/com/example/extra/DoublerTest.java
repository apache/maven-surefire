package com.example.extra;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DoublerTest {
    @Test
    void testDoubled() {
        assertEquals(42L, new Doubler().doubled(21L));
    }
}
