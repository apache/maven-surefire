package org.apache.maven.surefire.its;


import java.io.File;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Test basic default configuration, runs the JUnit 3 test in the src/test directory.
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class DefaultConfigurationTest
    extends AbstractSurefireIT
{
    public void testDefaultConfiguration ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/default-configuration" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        this.executeGoal( verifier, "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, testDir );        
    }
}
