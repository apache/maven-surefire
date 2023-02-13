package it;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

@Test
public class ParallelTest1
{
    public void test() throws Exception
    {
        System.out.println( "test 1" );
        TimeUnit.SECONDS.sleep( 1L );
    }
}
