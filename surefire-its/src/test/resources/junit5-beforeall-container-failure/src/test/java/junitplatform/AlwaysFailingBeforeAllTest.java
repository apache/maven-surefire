package junitplatform;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AlwaysFailingBeforeAllTest {

    @BeforeAll
    public static void setup() {
        throw new RuntimeException("BeforeAll always fails");
    }

    @Test
    public void testPassingTest() {
        // this test should be skipped or fail due to beforeAll failure
    }
}
