package org.codehaus;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class SomeFailedTest {
	
	@Test
	public void failedTest() {
		Assert.assertFalse(true);
	}
}
