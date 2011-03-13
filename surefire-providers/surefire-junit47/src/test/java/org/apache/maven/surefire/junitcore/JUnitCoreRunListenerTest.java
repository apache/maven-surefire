package org.apache.maven.surefire.junitcore;

import junit.framework.TestCase;
import org.apache.maven.surefire.report.MulticastingReporter;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

import java.util.Collections;
import java.util.HashMap;

import static junit.framework.Assert.assertEquals;

/**
 * @author Kristian Rosenvold
 */
public class JUnitCoreRunListenerTest  extends TestCase
{
    public void testTestRunStarted()
        throws Exception
    {
        RunListener jUnit4TestSetReporter = new JUnitCoreRunListener( new MulticastingReporter( Collections.emptyList() ),
                                                                      new HashMap<String, TestSet>(  ) );
        JUnitCore core = new JUnitCore();
        core.addListener(  jUnit4TestSetReporter );
        Result result = core.run( new Computer(), STest1.class, STest2.class);
        core.removeListener(  jUnit4TestSetReporter );
        assertEquals(2, result.getRunCount());
    }

    public void testFailedAssumption()
        throws Exception
    {
        RunListener jUnit4TestSetReporter = new JUnitCoreRunListener( new MulticastingReporter( Collections.emptyList() ),
                                                                      new HashMap<String, TestSet>(  ) );
        JUnitCore core = new JUnitCore();
        core.addListener(  jUnit4TestSetReporter );
        Result result = core.run( new Computer(), TestWithAssumptionFailure.class);
        core.removeListener(  jUnit4TestSetReporter );
        assertEquals(1, result.getRunCount());
    }


    public static class STest1
    {
        @Test
        public void testSomething(){

        }
    }
    public static class STest2
    {
        @Test
        public void testSomething2(){

        }
    }

    public static class TestWithAssumptionFailure
    {
        @Test
        public void testSomething2()
        {
            Assume.assumeTrue(false);

        }
    }

}
