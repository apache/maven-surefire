package junit47ConsoleOutput;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class Test0 {

    public Test0(){
        System.out.println("Constructor");
    }

    @Test
    public void testT0() throws Exception {
        System.out.println("testT0");
    }

    @Test
    public void testT1() throws Exception {
        System.out.println("testT1");
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        System.out.println("setUpBeforeClass");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        System.out.println("tearDownAfterClass");
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("setUp");
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("tearDown");
    }
}
