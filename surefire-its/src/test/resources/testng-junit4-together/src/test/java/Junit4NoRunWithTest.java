import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

/**
 * Provided to ensure both junit and testng tests can run together.
 *
 * @author jkuhnert
 * @author agudian
 */
public class Junit4NoRunWithTest {

	Object testObject;

	/**
	 * Creates an object instance
	 */
	@Before
	public void setUp()
	{
		testObject = new Object();
	}

	/**
	 * Tests that object created in setup
	 * isn't null.
	 */
	@Test
	public void isJunitObject()
	{
		assertNotNull(testObject);
	}
}
