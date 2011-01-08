package org.apache.maven.surefire.common.junit3;

import junit.framework.TestCase;
import junit.framework.TestResult;
import org.apache.maven.surefire.testset.TestSetFailedException;

/**
 * @author Kristian Rosenvold
 */
public class JUnit3TestCheckerTest
    extends TestCase {
    JUnit3TestChecker jUnit3TestChecker = new JUnit3TestChecker( this.getClass().getClassLoader() );

    public void testValidJunit4Annotated()
        throws TestSetFailedException
    {
        assertTrue( jUnit3TestChecker.accept( JUnit3TestCheckerTest.class ) );
    }

    public void testValidJunit4itsAJunit3Test()
        throws TestSetFailedException
    {
        assertTrue( jUnit3TestChecker.accept( AlsoValid.class ) );
    }

    public void testValidJunitSubclassWithoutOwnTestmethods()
        throws TestSetFailedException
    {
        assertTrue( jUnit3TestChecker.accept( SubClassWithoutOwnTestMethods.class ) );
    }

    public void testInvalidTest()
        throws TestSetFailedException
    {
        assertFalse( jUnit3TestChecker.accept( NotValidTest.class ) );
    }

    public void testDontAcceptAbstractClasses()
    {
        assertFalse( jUnit3TestChecker.accept( BaseClassWithTest.class ) );
    }

    public void testSuiteOnlyTest()
    {
        assertTrue( jUnit3TestChecker.accept( SuiteOnlyTest.class ) );
    }

    public void testCustomSuiteOnlyTest()
    {
        assertTrue( jUnit3TestChecker.accept( CustomSuiteOnlyTest.class ) );
    }

    public void testIinnerClassNotAutomaticallyTc(){
        assertTrue( jUnit3TestChecker.accept( NestedTC.class));
        assertFalse( jUnit3TestChecker.accept( NestedTC.Inner.class));
    }


    public static class AlsoValid
        extends TestCase
    {
        public void testSomething()
        {

        }
    }

    public static class SuiteOnlyTest
    {
        public static junit.framework.Test suite()
        {
            return null;
        }
    }

    public static class CustomSuiteOnlyTest
    {
        public static MySuite2 suite()
        {
            return null;
        }
    }

    public static class MySuite2
        implements junit.framework.Test
    {
        public int countTestCases()
        {
            return 0;
        }

        public void run( TestResult testResult )
        {
        }
    }


    public static class NotValidTest
    {
        /** @noinspection UnusedDeclaration*/
        public void testSomething()
        {
        }
    }

    public abstract static class BaseClassWithTest extends TestCase
    {
        /** @noinspection UnusedDeclaration*/
        public void testWeAreAlsoATest()
        {
        }
    }

    public static class SubClassWithoutOwnTestMethods
        extends BaseClassWithTest
    {
    }

    class NestedTC extends TestCase {
        public class Inner {

        }
    }

}
