package org.apache.maven.surefire.its;


import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Use -Dtest to run a single TestNG test, overriding the suite XML parameter.
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class TestNgSuiteXmlSingleTest
    extends TestCase
{
    public void testTestNGSuite()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/testng-twoTestCaseSuite" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List goals = new ArrayList();
        goals.add( "test" );
        goals.add( "-Dtest=TestNGTestTwo" );
        verifier.executeGoals( goals );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List reports = HelperAssertions.extractReports( ( new File[] { testDir } ) );
        ITSuiteResults results = HelperAssertions.parseReportList( reports );
        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, results );
    }

}
