package org.apache.maven.surefire.its;

import java.io.File;

import org.apache.maven.integrationtests.AbstractMavenIntegrationTestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Test Surefire-224 (XML test reports are not well-formed when failure message contains quotes)
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class Surefire224WellFormedXmlFailuresTest
    extends AbstractMavenIntegrationTestCase
{
    public void testWellFormedXmlFailures ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/surefire-224-wellFormedXmlFailures" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "test" );
        // DGF Don't verify error free log; we expect failures
        // verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        HelperAssertions.assertTestSuiteResults( 3, 0, 3, 0, testDir );
        
    }
}
