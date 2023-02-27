package forkConsoleOutput;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

public class Test1
{
    @Test
    public void test6281() {
        System.out.println( "Test1 on" + Thread.currentThread().getName());
    }

    @Test
    public void nullPointerInLibrary() {
        new File((String)null);
    }

    @Test
    public void failInMethod() {
        innerFailure();
    }

    @Test
    public void failInLibInMethod() {
        new File((String)null);
    }


    @Test
    public void failInNestedLibInMethod() {
        nestedLibFailure();
    }

    @Test
    public void assertion1() {
        Assert.assertEquals("Bending maths", "123", "312");
    }

    @Test
    public void assertion2() {
        Assert.assertFalse("True is false", true);
    }

    private void innerFailure(){
        throw new NullPointerException("Fail here");
    }

    private void nestedLibFailure(){
        new File((String) null);
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
