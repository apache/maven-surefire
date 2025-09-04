package io.olamy;

import org.junit.jupiter.api.*;

abstract class BaseNestedTest {
    @Test
    void outerTest() {
    }

    @Nested
    class Inner {

        @Test
        void innerTest() {
        }
    }
}

class FirstNestedTest extends BaseNestedTest {
}

class SecondNestedTest extends BaseNestedTest {
}
