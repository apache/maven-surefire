package concurrentjunit47.src.test.java.junit47;

import org.junit.*;

import java.util.concurrent.TimeUnit;

public class BasicTest
{
    private boolean setUpCalled = false;

    @Before
    public void setUp()
    {
        setUpCalled = true;
        System.out.println( "Called setUp" );
    }

    @After
    public void tearDown()
    {
        setUpCalled = false;
        System.out.println( "Called tearDown" );
    }

    @Test
    public void testSetUp()
    {
        Assert.assertTrue( "setUp was not called", setUpCalled );
    }

    @Test
    public void a() throws Exception {
        TimeUnit.SECONDS.sleep( 1 );
    }

    @Test
    public void b() throws Exception {
        TimeUnit.SECONDS.sleep( 1 );
    }

    @Test
    public void c() throws Exception {
        TimeUnit.SECONDS.sleep( 1 );
    }

    @AfterClass
    public static void oneTimeTearDown()
    {

    }

}
