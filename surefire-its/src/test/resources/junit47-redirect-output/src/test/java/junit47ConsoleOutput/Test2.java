package junit47ConsoleOutput;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class Test2
{
    @Test
    public void test6281() {
        System.out.println( "Test2 on" + Thread.currentThread().getName());
    }

    @BeforeClass
    public static void testWithFailingAssumption2() {
        System.out.println( "BeforeTest2 on" + Thread.currentThread().getName());
    }
    
    @AfterClass
    public static void testWithFailingAssumption3() {
        System.out.println( "AfterTest2 on" + Thread.currentThread().getName());
    }

}
