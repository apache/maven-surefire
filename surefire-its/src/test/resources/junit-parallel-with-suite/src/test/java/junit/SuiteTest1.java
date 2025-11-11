package junit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Kristian Rosenvold
 */
public class SuiteTest1
{
    private static final int PERFORMANCE_TEST_MULTIPLICATION_FACTOR = 4;

    private static long startedAt;

    public SuiteTest1()
    {
        System.out.println( "SuiteTest1.constructor" );
    }

    @BeforeAll
    public static void beforeClass()
    {
        startedAt = System.currentTimeMillis();
    }

    @AfterAll
    public static void afterClass()
    {
        System.out.println( String.format( "%s test finished after duration=%d", SuiteTest1.class.getSimpleName(),
                                           System.currentTimeMillis() - startedAt ) );
    }

    @BeforeEach
    public void setUp()
    {
        System.out.println( "SuiteTest1.setUp" );
    }

    @AfterEach
    public void tearDown()
    {
        System.out.println( "SuiteTest1.tearDown" );
    }

    @Test
    public void first()
        throws InterruptedException
    {
        System.out.println( "begin SuiteTest1.first" );
        Thread.sleep( 500 * PERFORMANCE_TEST_MULTIPLICATION_FACTOR );
        System.out.println( "end SuiteTest1.first" );
    }

    @Test
    public void second()
        throws InterruptedException
    {
        System.out.println( "begin SuiteTest1.second" );
        Thread.sleep( 500 * PERFORMANCE_TEST_MULTIPLICATION_FACTOR );
        System.out.println( "end SuiteTest1.second" );
    }

    @Test
    public void third()
        throws InterruptedException
    {
        System.out.println( "begin SuiteTest1.third" );
        Thread.sleep( 500 * PERFORMANCE_TEST_MULTIPLICATION_FACTOR );
        System.out.println( "end SuiteTest1.third" );
    }
}
