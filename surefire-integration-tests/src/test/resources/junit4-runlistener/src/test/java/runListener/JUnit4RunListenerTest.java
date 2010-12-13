package runListener;

import org.junit.Assert;
import org.junit.Test;

public class JUnit4RunListenerTest {

	@Test
	public void simpleTest()
	{
		Assert.assertEquals( 2, 1 + 1 );
	}
}
