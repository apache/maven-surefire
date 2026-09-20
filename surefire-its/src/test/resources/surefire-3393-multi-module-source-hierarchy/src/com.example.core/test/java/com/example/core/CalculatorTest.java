package com.example.core;

import jakarta.json.JsonValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculatorTest {
    @Test
    void testAdd() {
        assertEquals(42L, new Calculator().add(40L, 2L));
    }

    @Test
    void testKindOfJsonValue() {
        // exercises the jakarta.json module dependency on the module path
        assertEquals("TRUE", new Calculator().kindOf(JsonValue.TRUE));
    }
}
