import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * Tests grouping/threading/parallel functionality of TestNG.
 * 
 * @author jkuhnert
 */
public class TestNGTest {

	static int m_testCount = 0;
	
	/**
	 * Sets up testObject
	 */
	@BeforeClass(groups = "functional")
	public void configureTest()
	{
		testObject = new Object();
	}
	
	@AfterSuite(alwaysRun = true, groups = "functional")
	public void check_Test_Count()
	{
		System.out.println("check_Test_Count(): " + m_testCount);
		
		assert m_testCount == 3 : "Expected 3 tests to be run but local count was " + m_testCount;
	}
	
	Object testObject;
	
	/**
	 * Tests reporting an error
	 */
	@Test(groups = {"functional", "notincluded"})
	public void isTestObjectNull()
	{
		m_testCount++;
		assert testObject != null : "testObject is null";
	}
	
	/**
	 * Sample method that shouldn't be run by test suite.
	 */
	@Test(groups = "notincluded")
	public void shouldNotRun()
	{
		assert false == true : "Group specified by test shouldnt be run.";
	}
}