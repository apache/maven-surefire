package jira1727;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

class ErrorInTestFactoryJupiterTest
{
    @TestFactory
    Stream<DynamicTest> testFactory() {
        throw new RuntimeException( "Encountered error in TestFactory testFactory()" );
    }
}
