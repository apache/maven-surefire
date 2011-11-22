package org.apache.maven.surefire.util;

import java.util.ArrayList;
import java.util.List;
import org.apache.maven.surefire.testset.RunOrderParameters;

import junit.framework.TestCase;

/**
 * @author Kristian Rosenvold
 */
public class RunOrderCalculatorTest
    extends TestCase
{

    public void testOrderTestClasses()
        throws Exception
    {
        getClassesToRun();
        TestsToRun testsToRun = new TestsToRun( getClassesToRun() );
        RunOrderCalculator runOrderCalculator = new DefaultRunOrderCalculator( RunOrderParameters.ALPHABETICAL(), 1 );
        final TestsToRun testsToRun1 = runOrderCalculator.orderTestClasses( testsToRun );
        assertEquals( A.class, testsToRun1.iterator().next() );

    }

    private List getClassesToRun()
    {
        List classesToRun = new ArrayList();
        classesToRun.add( B.class );
        classesToRun.add( A.class );
        return classesToRun;
    }

    class A
    {

    }

    class B
    {

    }


}
