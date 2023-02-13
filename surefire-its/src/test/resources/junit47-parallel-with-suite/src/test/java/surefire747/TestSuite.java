package surefire747;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Kristian Rosenvold
 */
@RunWith(Suite.class)
@Suite.SuiteClasses(
{
	SuiteTest1.class,
	SuiteTest2.class
})
public class TestSuite
{
    private static long startedAt;

    @BeforeClass
    public static void beforeClass()
    {
        startedAt = System.currentTimeMillis();
    }

    @AfterClass
    public static void afterClass()
    {
        System.out.println( String.format( "%s suite finished after duration=%d", TestSuite.class.getSimpleName(),
                                           System.currentTimeMillis() - startedAt ) );
    }
}
