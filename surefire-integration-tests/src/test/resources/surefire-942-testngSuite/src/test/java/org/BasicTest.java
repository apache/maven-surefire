package org;

import org.testng.annotations.*;
import org.testng.Assert;

public class BasicTest
{

    private boolean setUpCalled = false;


    @BeforeTest
    public void setUp()
    {
        setUpCalled = true;
    }

    @AfterTest
    public void tearDown()
    {
        setUpCalled = false;
    }

    @Test
    public void testSetUp()
    {
        Assert.assertTrue( setUpCalled );
    }

    @Test
    public void testSuccessOne()
    {
        Assert.assertTrue( true );
    }

    @Test
    public void testSuccessTwo()
    {
        Assert.assertTrue( true );
    }

    @AfterClass
    public static void oneTimeTearDown()
    {

    }

}
