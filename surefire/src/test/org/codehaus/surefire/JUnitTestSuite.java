package org.codehaus.surefire;

import junit.framework.TestSuite;
import junit.framework.TestCase;
import junit.framework.Test;

/**
 *
 * 
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 *
 * @version $Id$
 */
public class JUnitTestSuite
    extends TestCase
{
    public static Test suite()
        throws Exception
    {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest( DummySuite.suite() );

        return testSuite;
    }
}
