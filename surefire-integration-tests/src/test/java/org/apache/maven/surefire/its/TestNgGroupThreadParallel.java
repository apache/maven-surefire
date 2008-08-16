package org.apache.maven.surefire.its;


import java.io.File;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Test TestNG groups, together with TestNG parallelism
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class TestNgGroupThreadParallel
    extends AbstractSurefireIT
{
    public void testTestNgGroupThreadParallel ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/testng-group-thread-parallel" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        this.executeGoal( verifier, "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        HelperAssertions.assertTestSuiteResults( 3, 0, 0, 0, testDir );
        
    }
}
