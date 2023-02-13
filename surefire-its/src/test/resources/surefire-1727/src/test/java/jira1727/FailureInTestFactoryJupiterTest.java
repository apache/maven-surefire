package jira1727;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

class FailureInTestFactoryJupiterTest
{
    @TestFactory
    Stream<DynamicTest> testFactory() {
        fail( "Encountered failure in TestFactory testFactory()" );
        return Stream.of();
    }
}
