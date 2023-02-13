package junitplatformenginejupiter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BasicJupiterTest
{

    private boolean setUpCalled;

    private static boolean tearDownCalled;

    @BeforeEach
    void setUp()
    {
        setUpCalled = true;
        tearDownCalled = false;
        System.out.println( "Called setUp" );
    }

    @AfterEach
    void tearDown()
    {
        setUpCalled = false;
        tearDownCalled = true;
        System.out.println( "Called tearDown" );
    }

    @Test
    void test(TestInfo info)
    {
        assertTrue( setUpCalled, "setUp was not called" );
        assertEquals( "test(TestInfo)", info.getDisplayName(), "display name mismatch" );
    }

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


    @AfterAll
    static void oneTimeTearDown()
    {
        assertTrue( tearDownCalled );
    }

}
