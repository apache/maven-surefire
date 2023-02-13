package junitplatformenginevintage;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BasicVintageTest
{
    private static boolean tearDownCalled = false;

    private boolean setUpCalled = false;

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
    }


    @AfterClass
    public static void oneTimeTearDown()
    {
        Assert.assertTrue( "tearDown was not called", tearDownCalled );
    }

}
