package testng.beforeMethodFailure;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class TestNGSuiteTest {

    private boolean beforeMethod = false;
    
    @BeforeMethod
    public void beforeMethod() {
        Assert.fail();
    }
	@Test
	public void testBeforeMethod()
	{
		Assert.assertTrue( beforeMethod );
	}
}