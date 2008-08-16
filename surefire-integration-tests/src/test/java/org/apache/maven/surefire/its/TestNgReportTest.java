package org.apache.maven.surefire.its;

import java.io.File;

import junit.framework.Assert;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Test surefire-report on TestNG test
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class TestNgReportTest
    extends AbstractSurefireIT
{
    public void testTestNgReport ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/testng-simple" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        this.executeGoal( verifier, "surefire-report:report" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, testDir );
        File reportHtml = new File(testDir, "target/site/surefire-report.html");
        Assert.assertTrue( "surefire-report is missing", reportHtml.exists() );
    }
}
