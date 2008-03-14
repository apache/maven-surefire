package testng.afterSuiteFailure;

import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;


public class TestNGSuiteTest {

	@Test
	public void doNothing()
	{
		
	}
	
	@AfterSuite
	public void failAfterSuite()
	{
	    Assert.fail();
	}
}