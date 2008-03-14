package testng.beforeMethod;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class TestNGSuiteTest {

    private boolean beforeMethod = false;
    
    @BeforeMethod
    public void beforeMethod() {
        beforeMethod = true;
    }
	@Test
	public void testBeforeMethod()
	{
		Assert.assertTrue( beforeMethod );
	}
}