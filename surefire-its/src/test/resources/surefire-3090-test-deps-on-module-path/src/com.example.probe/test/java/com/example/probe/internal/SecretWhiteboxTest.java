package com.example.probe.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Whitebox test accessing the non-exported internal package of com.example.probe.
 */
class SecretWhiteboxTest {
    @Test
    void testReveal() {
        assertEquals("42", Secret.reveal());
    }
}
