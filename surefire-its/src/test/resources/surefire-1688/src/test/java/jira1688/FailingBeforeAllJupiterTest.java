package jira1688;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FailingBeforeAllJupiterTest
{
    @BeforeAll
    static void oneTimeSetUp()
    {
        fail( "oneTimeSetUp() failed" );
    }

    @Test
    void test()
    {
    }
}
