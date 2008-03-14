package org.apache.maven.surefire.its;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.plugins.surefire.report.ReportTestSuite;

/**
 * Test running two test cases; confirms reporting works correctly
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 */
public class TwoTestCasesTest
    extends TestCase
{
    public void testTwoTestCases()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/junit-twoTestCases" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        HelperAssertions.assertTestSuiteResults( 2, 0, 0, 0, testDir );
    }

    /** Runs two tests encapsulated in a suite */
    public void testTwoTestCaseSuite()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/junit-twoTestCaseSuite" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        List reports = HelperAssertions.extractReports( (new File[] { testDir }) );
        Set classNames = extractClassNames( reports );
        assertContains( classNames, "junit.twoTestCaseSuite.BasicTest" );
        assertContains( classNames, "junit.twoTestCaseSuite.TestTwo" );
        assertEquals( "wrong number of classes", 2, classNames.size() );
        ITSuiteResults results = HelperAssertions.parseReportList( reports );
        HelperAssertions.assertTestSuiteResults( 2, 0, 0, 0, results );
    }
    
    private void assertContains( Set set, String expected )
    {
        if ( set.contains( expected ) ) return;
        fail( "Set didn't contain " + expected );
    }
    
    private Set extractClassNames( List reports )
    {
        ReportTestSuite suite;
        HashSet classNames = new HashSet();
        for ( int i = 0; i < reports.size(); i++ )
        {
            suite = (ReportTestSuite) reports.get( i );
            classNames.add( suite.getFullClassName() );
        }
        return classNames;
    }

    public void testJunit4Suite()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/junit4-twoTestCaseSuite" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List reports = HelperAssertions.extractReports( (new File[] { testDir }) );
        Set classNames = extractClassNames( reports );
        assertContains( classNames, "twoTestCaseSuite.BasicTest" );
        assertContains( classNames, "twoTestCaseSuite.Junit4TestTwo" );
        assertEquals( "wrong number of classes", 2, classNames.size() );
        ITSuiteResults results = HelperAssertions.parseReportList( reports );
        HelperAssertions.assertTestSuiteResults( 2, 0, 0, 0, results );
    }

    public void testTestNGSuite()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/testng-twoTestCaseSuite" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List reports = HelperAssertions.extractReports( (new File[] { testDir }) );
        Set classNames = extractClassNames( reports );
        assertContains( classNames, "testng.two.TestNGTestTwo" );
        assertContains( classNames, "testng.two.TestNGSuiteTest" );
        assertEquals( "wrong number of classes", 2, classNames.size() );
        ITSuiteResults results = HelperAssertions.parseReportList( reports );
        HelperAssertions.assertTestSuiteResults( 2, 0, 0, 0, results );
    }

}
