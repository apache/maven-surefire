package jira1748;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

class AssertionsFailEmptyStringParameterJupiterTest
{
    @Test
    void doTest()
    {
        fail( "" );
    }
}
