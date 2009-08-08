package org.apache.maven.surefire.its;


import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.List;

/**
 * Test report aggregation
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class AggregateReportTest
    extends AbstractSurefireIT
{
    public void testAggregateReport ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/aggregate-report" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List goals = this.getInitialGoals();
        goals.add( "surefire-report:report" );
        verifier.executeGoals( goals );
        //DGF even though the build will succeed, the log will contain errors (from the failure)
        //verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        File surefireReportHtml = new File( testDir, "target/site/surefire-report.html");
        assertTrue( "surefire report missing: " + surefireReportHtml.getAbsolutePath(), surefireReportHtml.exists() );
        
        // TODO HtmlUnit tests on the surefire report
        
        File[] testDirs = new File[2];
        testDirs[0] = new File( testDir, "child1" );
        testDirs[1] = new File( testDir, "child2" );
        ITSuiteResults suite = HelperAssertions.parseTestResults( testDirs );
        HelperAssertions.assertTestSuiteResults( 2, 0, 1, 0, suite );        
    }
}
