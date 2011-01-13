package surefire500;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class ExplodingTest {

	static {
		if (true) {
			throw new java.lang.NoClassDefFoundError("whoops!"); 
		}
	}
	
	@Test
	public void testPass() throws Exception {
		assertTrue(true);
	}
	
	public void testFail() throws Exception {
		fail("fail");
	}
}
