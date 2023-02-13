package jira1741;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

class ErrorInParameterizedSourceJupiterTest
{
    static Stream<Arguments> args() {
        throw new RuntimeException( "args() method source encountered an error" );
    }

    @ParameterizedTest
    @MethodSource("args")
    void doTest() {
    }
}
