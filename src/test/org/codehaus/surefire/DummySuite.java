package org.codehaus.surefire;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

/**
 *
 * 
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 *
 * @version $Id$
 */
public class DummySuite
    extends TestCase
{
    public static Test suite()
    {
        return new TestSuite( DummySuite.class );
    }

    public void testDummy()
    {
        assertTrue( true );
    }
}
