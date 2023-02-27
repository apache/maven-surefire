package jira1857;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

class AssertionFailureMessageTest
{
    @Test
    void failedTest()
    {
        fail( "fail_message" );
    }
}
