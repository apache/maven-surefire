package org.apache.maven.surefire.common.junit4;

import org.junit.runner.notification.Failure;

import java.util.ArrayList;
import java.util.List;

/**
 * Test listener to record all the failures during one run
 *
 * @author Qingzhou Luo
 */
public class JUnitTestFailureListener
    extends org.junit.runner.notification.RunListener
{

    List<Failure> allFailures = new ArrayList<Failure>();

    @Override
    public void testFailure( Failure failure )
        throws java.lang.Exception
    {
        allFailures.add( failure );
    }

    public List<Failure> getAllFailures()
    {
        return allFailures;
    }

    public void reset()
    {
        allFailures.clear();
    }
}
