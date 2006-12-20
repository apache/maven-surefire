import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * Simple test
 * 
 * @author jkuhnert
 */
public class TestNGJunitTest {

	/**
	 * Sets up testObject
	 */
	@BeforeClass
	public void configureTest()
	{
		testObject = new Object();
	}
	
	Object testObject;
	
	/**
	 * Tests reporting an error
	 */
	@Test
	public void isTestObjectNull()
	{
		assert testObject != null : "testObject is null";
	}
}
