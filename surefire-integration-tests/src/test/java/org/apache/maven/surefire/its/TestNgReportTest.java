package org.apache.maven.surefire.its;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * Test surefire-report on TestNG test
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class TestNgReportTest
    extends TestCase
{
    public void testTestNgReport ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/testng-simple" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "surefire-report:report" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, testDir );
        File reportHtml = new File(testDir, "target/site/surefire-report.html");
        Assert.assertTrue( "surefire-report is missing", reportHtml.exists() );
    }
}
