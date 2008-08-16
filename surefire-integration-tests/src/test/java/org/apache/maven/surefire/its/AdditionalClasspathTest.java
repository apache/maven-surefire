package org.apache.maven.surefire.its;


import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * Test additionalClasspathElements
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class AdditionalClasspathTest
    extends AbstractSurefireIT
{
    public void testAdditionalClasspath ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/additional-classpath" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        this.executeGoal( verifier, "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, testDir );        
    }
}
