package junitplatform;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Test class with @BeforeAll that always fails.
 * Used to test scenario where @BeforeAll never succeeds even with reruns.
 */
public class AlwaysFailingTest {

    @BeforeAll
    static void setup() {
        System.out.println("Error beforeAll in AlwaysFailingTest");
        throw new IllegalArgumentException("BeforeAll always fails");
    }

    @Test
    public void testThatNeverRuns() {
        System.out.println("This test never runs because beforeAll always fails");
    }
}