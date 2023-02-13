package jira2117;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName( "Display name of the main test class" )
class NestedDisplayNameTest
{
    @Nested
    @DisplayName( "Display name of level 1 nested class A" )
    class A
    {
        @Test
        void level1_test_without_display_name()
        {
        }

        @Test
        @DisplayName( "Display name of level 1 test method" )
        void level1_test_with_display_name()
        {
        }
    }

    @Nested
    @DisplayName( "Display name of level 1 nested class B" )
    class B
    {
        @Nested
        @DisplayName( "Display name of level 2 nested class C" )
        class C
        {
            @Test
            @DisplayName( "Display name of non-parameterized level 2 test method" )
            void level2_test_nonparameterized()
            {
            }

            @ParameterizedTest
            @ValueSource(strings = {"paramValue1", "paramValue2"})
            @DisplayName( "Display name of parameterized level 2 test method" )
            void level2_test_parameterized(String paramValue)
            {
            }
        }
    }
}
