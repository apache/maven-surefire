package org.apache.maven.surefire.its;


import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * Test passing an argLine parameter
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class ArgLineIT
    extends AbstractSurefireIntegrationTestClass
{
    public void testArgLine ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/argLine-parameter" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        this.executeGoal( verifier, "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, testDir );        
    }
}
