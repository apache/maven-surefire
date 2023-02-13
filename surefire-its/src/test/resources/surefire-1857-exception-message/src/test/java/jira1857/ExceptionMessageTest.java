package jira1857;

import org.junit.jupiter.api.Test;

class ExceptionMessageTest
{
    @Test
    void errorTest() {
        throw new RuntimeException( "error_message" );
    }
}
