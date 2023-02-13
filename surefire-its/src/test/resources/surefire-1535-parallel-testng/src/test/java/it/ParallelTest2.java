package it;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

@Test
public class ParallelTest2
{
    public void test() throws Exception
    {
        System.out.println( "test 2" );
        TimeUnit.SECONDS.sleep( 1L );
    }
}
