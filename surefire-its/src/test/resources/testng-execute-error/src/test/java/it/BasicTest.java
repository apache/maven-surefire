package it;

import static org.testng.Assert.*;

import org.testng.annotations.*;

/*
 * Intentionally misconfigured (cycle) to cause an error before the suite is actually run.
 */
@Test(groups = { "test" }, dependsOnGroups = { "test" })
public class BasicTest {

	public void testTrue() {
		assertTrue(true);
	}

}
