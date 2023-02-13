package jira1914;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ParameterizedJupiterTest
{

    @ParameterizedTest(name = "{0} + {1} = {2}")
    @CsvSource({
                    "0,    1,   1",
                    "1,    2,   3",
                    "49,  51, 100",
                    "1,  100, 101"
    })
    void add(int first, int second, int expectedResult)
    {
        assertEquals(expectedResult, first + second, () -> first + " + " + second + " should equal " + expectedResult);
    }

    @ParameterizedTest
    @CsvSource({
        " 1, 1",
        " 2, 4",
        " 3, 9"
    })
    void square(int number, int expectedResult)
    {
        assertEquals(expectedResult, number * number, () -> number + " * " + number + " should equal " + expectedResult);
    }

    @ParameterizedTest
    @CsvSource({
        "1, 1",
        "2, 8",
        "3, 27"
    })
    void cube(int number, int expectedResult)
    {
        assertEquals(expectedResult, number * number * number, () -> number + " * " + number + " * " + number + " should equal " + expectedResult);
    }

}
