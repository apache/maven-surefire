package junit44Dep;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class BasicTest
{

    private boolean setUpCalled = false;

    private static boolean tearDownCalled = false;

    @Before
    public void setUp()
    {
        setUpCalled = true;
        tearDownCalled = false;
        System.out.println( "Called setUp" );
    }

    @After
    public void tearDown()
    {
        setUpCalled = false;
        tearDownCalled = true;
        System.out.println( "Called tearDown" );
    }

    @Test
    public void testSetUp()
    {
        Assert.assertTrue( "setUp was not called", setUpCalled );
        Assert.assertThat( true, Is.is( true ) );
    }

    @AfterClass
    public static void oneTimeTearDown()
    {
        Assert.assertTrue( "tearDown was not called", tearDownCalled );
    }

}
