package testng.groupThreadParallel;

import org.testng.Assert;
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
		Assert.assertTrue( m_testCount == 3,  "Expected 3 tests to be run but local count was " + m_testCount );
	}
	
	Object testObject;
	
	@Test(groups = {"functional", "notincluded"})
	public void test1() throws InterruptedException
	{
		incrementTestCount();
		System.out.println("running test");
		Assert.assertTrue( testObject != null , "testObject is null" );
		waitForTestCountToBeThree();
	}
	
	private synchronized void incrementTestCount() {
	    m_testCount++;
	}
	
	@Test(groups = {"functional", "notincluded"})
    public void test2() throws InterruptedException {
	    test1();
	}
	
	@Test(groups = {"functional", "notincluded"})
    public void test3() throws InterruptedException {
        test1();
    }
	
	private void waitForTestCountToBeThree()
        throws InterruptedException
    {
        if ( m_testCount == 3 ) return;
        long now = System.currentTimeMillis();
        long timeout = 5 * 1000;
        long finish = now + timeout;
        while ( m_testCount < 3 && System.currentTimeMillis() < finish )
        {
            Thread.sleep( 10 );
        }
        Assert.assertTrue(m_testCount >= 3);
    }
	
	/**
	 * Sample method that shouldn't be run by test suite.
	 */
	@Test(groups = "notincluded")
	public void shouldNotRun()
	{
		Assert.fail( "Group specified by test shouldnt be run." );
	}
}