package org.codehaus.surefire;

import junit.framework.TestResult;

/**
 * This class provides the 'suite' and junit.framework.Test API
 * without implementing an interface. It (hopefully) mimicks
 * a sysunit generated testcase.
 */
public class SysUnitTest
{
    public static Object suite()
    {
        return new SysUnitTest();
    }

    public int countTestCases( TestResult tr )
    {
        return 1;
    }

    public void run()
    {
        testFoo();
    }

    public void testFoo()
    {
        System.out.println("No assert available");
    }
}
