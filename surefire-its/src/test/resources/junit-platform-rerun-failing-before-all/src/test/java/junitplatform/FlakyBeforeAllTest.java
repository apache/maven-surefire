package junitplatform;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class FlakyBeforeAllTest {
    private static boolean firstRun = true;

    @BeforeAll
    static void setup() {
        if (firstRun) {
            firstRun = false;
            throw new IllegalStateException("BeforeAll fails once");
        }
    }

    @Test
    public void testPasses() {}
}
