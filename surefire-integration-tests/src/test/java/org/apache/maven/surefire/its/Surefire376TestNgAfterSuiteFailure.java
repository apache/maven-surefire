package org.apache.maven.surefire.its;


import java.io.File;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Test Surefire-376 (TestNG @AfterSuite failures are ignored)
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class Surefire376TestNgAfterSuiteFailure
    extends AbstractSurefireIT
{
    public void testAfterSuiteFailure ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/testng-afterSuiteFailure" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        this.executeGoal( verifier, "test" );
        // DGF Don't verify error free log; we expect failures
        // verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        HelperAssertions.assertTestSuiteResults( 2, 0, 1, 0, testDir );
        
    }
}
