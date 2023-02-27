import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Provided to ensure both junit and testng tests can run together.
 *
 * @author jkuhnert
 * @author agudian
 */
@RunWith(JUnit4.class)
public class Junit4SimpleRunWithTest {

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
