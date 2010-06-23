package surefire613;

import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assume.*;
import static junit.framework.Assert.fail;

/**
 * @author Kristian Rosenvold
 */
public class Test2
{
    @Test
    public void testAllok(){
        System.out.println("testAllok to stdout");
        System.err.println("testAllok to stderr");
    }


    @Ignore
    @Test
    public void testWithIgnore1(){
    }

    @Ignore
    @Test
    public void testWithIgnore2(){
    }

    @Test
    public void testiWithFail1(){
        fail("We excpect this");
    }
    @Test
    public void testiWithFail2(){
        fail("We excpect this");
    }
    @Test
    public void testiWithFail3(){
        fail("We excpect this");
    }
    @Test
    public void testiWithFail4(){
        fail("We excpect this");
    }

    @Test
    public void testWithException1() {
        System.out.println("testWithException1 to stdout");
        System.err.println("testWithException1 to stderr");
        throw new RuntimeException("We expect this");
    }
    @Test
    public void testWithException2() {
        throw new RuntimeException("We expect this");
    }
    @Test
    public void testWithException3() {
        throw new RuntimeException("We expect this");
    }
    @Test
    public void testWithException4() {
        throw new RuntimeException("We expect this");
    }
    @Test
    public void testWithException5() {
        throw new RuntimeException("We expect this");
    }
    @Test
    public void testWithException6() {
        throw new RuntimeException("We expect this");
    }
    @Test
    public void testWithException7() {
        throw new RuntimeException("We expect this");
    }
    @Test
    public void testWithException8() {
        throw new RuntimeException("We expect this");
    }

}