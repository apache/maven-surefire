package junit;

import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.BeforeSuite;
import org.junit.platform.suite.api.AfterSuite;

/**
 * @author Kristian Rosenvold
 */
@Suite
@SuiteDisplayName("JUnit Platform Suite Test")
@SelectPackages("junit")
@IncludeClassNamePatterns(".*SuiteTest.*")
public class TestSuite
{
    private static long startedAt;

    @BeforeSuite
    public static void beforeClass()
    {
        startedAt = System.currentTimeMillis();
    }

    @AfterSuite
    public static void afterClass()
    {
        System.out.println( String.format( "%s suite finished after duration=%d", TestSuite.class.getSimpleName(),
                                           System.currentTimeMillis() - startedAt ) );
    }
}
