package surefire613;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Test1
{
    @Test
    public void test2_1()
        throws InterruptedException
    {
        Thread.currentThread().sleep( 1200 );
    }

    @Test
    public void test2_2()
        throws InterruptedException
    {
        Thread.currentThread().sleep( 1200 );
        assertTrue(false);
        fail( "We expect failure" );
    }

    @Test
    public void test2_3()
        throws InterruptedException
    {
        Thread.currentThread().sleep( 1200 );
    }
}