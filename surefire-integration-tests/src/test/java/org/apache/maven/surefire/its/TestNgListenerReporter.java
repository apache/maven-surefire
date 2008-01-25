package org.apache.maven.surefire.its;


import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * Test simple TestNG listener and reporter
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class TestNgListenerReporter
    extends TestCase
{
    public void testTestNgListenerReporter ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/testng-listener-reporter" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, testDir );
        File targetDir = new File( testDir, "target" );
        assertFileExists ( new File( targetDir, "resultlistener-output.txt" ) );
        assertFileExists ( new File( targetDir, "suitelistener-output.txt" ) );
        assertFileExists ( new File( targetDir, "reporter-output.txt" ) );
    }
    
    private void assertFileExists( File file )
    {
        assertTrue( "File doesn't exist: " + file.getAbsolutePath(), file.exists() );
    }
}
