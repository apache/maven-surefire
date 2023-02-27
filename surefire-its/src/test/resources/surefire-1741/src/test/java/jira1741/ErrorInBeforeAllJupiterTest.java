package jira1741;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ErrorInBeforeAllJupiterTest
{
    @BeforeAll
    static void oneTimeSetUp()
    {
        throw new RuntimeException( "oneTimeSetUp() encountered an error" );
    }

    @Test
    void test()
    {
    }
}
