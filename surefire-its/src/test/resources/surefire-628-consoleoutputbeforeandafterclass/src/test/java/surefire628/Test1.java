package surefire628;

import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assume.*;
public class Test1
{
    @Test
    public void test6281() {
        System.out.println( "628Test1 on" + Thread.currentThread().getName());
    }

    @BeforeClass
    public static void testWithFailingAssumption2() {
        System.out.println( "Before628Test1 on" + Thread.currentThread().getName());
    }
    
    @AfterClass
    public static void testWithFailingAssumption3() {
        System.out.println( "After628Test1 on" + Thread.currentThread().getName());
    }

}
