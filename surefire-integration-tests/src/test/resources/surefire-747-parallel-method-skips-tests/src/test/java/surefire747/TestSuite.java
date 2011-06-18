package surefire747;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Kristian Rosenvold
 */
@RunWith(Suite.class)
@Suite.SuiteClasses(
{
	ParallelTest.class,
	ParallelTest2.class
})
public class TestSuite
{
}
