package surefire747;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Kristian Rosenvold
 */
public class SuiteTest2
{
    private static final int PERFORMANCE_TEST_MULTIPLICATION_FACTOR = 4;

    private static long startedAt;

    public SuiteTest2()
    {
        System.out.println( "SuiteTest2.constructor" );
    }

    @BeforeClass
    public static void beforeClass()
    {
        startedAt = System.currentTimeMillis();
    }

    @AfterClass
    public static void afterClass()
    {
        System.out.println( String.format( "%s test finished after duration=%d", SuiteTest2.class.getSimpleName(),
                                           System.currentTimeMillis() - startedAt ) );
    }

    @Before
    public void setUp()
    {
        System.out.println( "SuiteTest2.setUp" );
    }

    @After
    public void tearDown()
    {
        System.out.println( "SuiteTest2.tearDown" );
    }

    @Test
    public void first()
        throws InterruptedException
    {
        System.out.println( "begin SuiteTest2.first" );
        Thread.sleep( 500 * PERFORMANCE_TEST_MULTIPLICATION_FACTOR );
        System.out.println( "end SuiteTest2.first" );
    }

    @Test
    public void second()
        throws InterruptedException
    {
        System.out.println( "begin SuiteTest2.second" );
        Thread.sleep( 500 * PERFORMANCE_TEST_MULTIPLICATION_FACTOR );
        System.out.println( "end SuiteTest2.second" );
    }

    @Test
    public void third()
        throws InterruptedException
    {
        System.out.println( "begin SuiteTest2.third" );
        Thread.sleep( 500 * PERFORMANCE_TEST_MULTIPLICATION_FACTOR );
        System.out.println( "end SuiteTest2.third" );
    }
}
