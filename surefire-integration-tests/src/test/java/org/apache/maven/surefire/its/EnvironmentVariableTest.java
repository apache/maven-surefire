package org.apache.maven.surefire.its;


import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * Test basic default configuration, runs the JUnit 3 test in the src/test directory.
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class EnvironmentVariableTest
    extends TestCase
{
    public void testEnvironmentVariable ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/junit44-environment" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, testDir );        
    }
}
