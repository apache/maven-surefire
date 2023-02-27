package junitplatform_1_0_0;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

class JUnitPlatformTest
{
    @Test
    void test(TestInfo info)
    {
        assertEquals( "test(TestInfo)", info.getDisplayName(), "display name mismatch" );
    }
}
