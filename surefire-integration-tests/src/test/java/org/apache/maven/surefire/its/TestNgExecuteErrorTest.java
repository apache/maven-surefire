package org.apache.maven.surefire.its;


import junit.framework.TestCase;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * Test for checking that the output from a forked suite is properly captured even if the suite encounters a severe error.
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class TestNgExecuteErrorTest
    extends TestCase
{
    public void testExecuteError()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/testng-execute-error" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        try {
            verifier.executeGoal( "test" );
        } catch (VerificationException e) {} // expected 
        verifier.resetStreams();
        assertTrue( new File( testDir, "target/surefire-reports/TestSuite-output.txt" ).length() > 0 );
    }
}
