package jira1741;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import static org.junit.jupiter.api.Assertions.fail;

class FailureInParameterizedSourceJupiterTest
{
    static Stream<Arguments> args() {
        fail( "args() method source failed" );
        return Stream.of();
    }

    @ParameterizedTest
    @MethodSource("args")
    void doTest() {
    }
}
