package org.apache.maven.surefire.junitcore;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import org.apache.maven.surefire.testset.TestSetFailedException;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.Result;

import static junit.framework.Assert.assertEquals;

/**
 * @author Kristian Rosenvold
 * @author nkeyval
 */
public class Surefire813IncorrectResultTest
{


    @Test
    public void dcount()
        throws TestSetFailedException, ExecutionException
    {
        JUnitCoreTester jUnitCoreTester = new JUnitCoreTester();
        final Result run = jUnitCoreTester.run( true, Test6.class );
        assertEquals(0, run.getFailureCount());
    }

    public static class Test6 {
      private final CountDownLatch latch = new CountDownLatch( 1 );

      @Test
      public void test61() throws Exception {
          System.out.println("Hey");
      }

      @After
      public void tearDown() throws Exception {
          new MyThread().start();
          latch.await();
      }

      private static final String s = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

      public void synchPrint(){
        for (int i=0; i<1000; ++i){ // Increase this number if it does no fail
          System.out.println(i+":"+s);
        }
      }

        private class MyThread extends Thread {
        @Override
        public void run() {
          System.out.println(s);
          latch.countDown();
          synchPrint();
        }
      }
    }

}
