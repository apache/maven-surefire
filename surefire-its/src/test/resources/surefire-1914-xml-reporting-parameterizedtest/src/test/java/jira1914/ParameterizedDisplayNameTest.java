package jira1914;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName( "theDisplayNameOfTheClass" )
class ParameterizedDisplayNameTest
{
    @ParameterizedTest
    @DisplayName( "theDisplayNameOfTestMethod1" )
    @ValueSource(strings = {"a", "b", "c"})
    void test1(String parameter)
    {
        // not relevant
    }

    @ParameterizedTest(name = "with param {0}")
    @DisplayName( "theDisplayNameOfTestMethod2" )
    @ValueSource(strings = {"a", "b", "c"})
    void test2(String parameter)
    {
        // not relevant
    }

}
