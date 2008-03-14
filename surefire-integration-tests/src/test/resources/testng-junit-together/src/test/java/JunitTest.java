import junit.framework.TestCase;

/**
 * Provided to ensure both junit and testng tests can run together.
 * 
 * @author jkuhnert
 */
public class JunitTest extends TestCase {

	Object testObject;
	
	/**
	 * Creats an object instance
	 */
	public void setUp()
	{
		testObject = new Object();
	}
	
	/**
	 * Tests that object created in setup 
	 * isn't null.
	 */
	public void testJunitObject()
	{
		assertNotNull(testObject);
	}
}
