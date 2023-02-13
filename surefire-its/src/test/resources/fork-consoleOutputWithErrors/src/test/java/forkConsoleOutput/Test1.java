package forkConsoleOutput;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class Test1
{
    @Test
    public void test6281() {
        System.out.println( "Test1 on" + Thread.currentThread().getName());
    }

    @BeforeClass
    public static void testWithFailingAssumption2() {
        System.out.println( "BeforeTest1 on" + Thread.currentThread().getName());
    }
    
    @AfterClass
    public static void testWithFailingAssumption3() {
        System.out.println( "AfterTest1 on" + Thread.currentThread().getName());
    }

}
