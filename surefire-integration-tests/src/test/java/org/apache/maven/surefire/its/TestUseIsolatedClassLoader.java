package org.apache.maven.surefire.its;


import java.io.File;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Test useSystemClassLoader option
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class TestUseIsolatedClassLoader
    extends AbstractSurefireIT
{
    public void testUseSystemClassLoader ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/isolated-classloader" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        this.executeGoal( verifier, "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, testDir );        
    }
}
