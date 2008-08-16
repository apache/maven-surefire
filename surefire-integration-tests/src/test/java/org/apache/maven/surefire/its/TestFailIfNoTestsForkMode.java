package org.apache.maven.surefire.its;


import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.reporting.MavenReportException;

/**
 * Test failIfNoTests with various forkModes.
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class TestFailIfNoTestsForkMode
    extends AbstractSurefireIT
{
    public void testFailIfNoTestsForkModeAlways () throws Exception
    {
        doTest("always", true);
    }
    
    public void testFailIfNoTestsForkModeNever() throws Exception 
    {
        doTest( "never", true );
    }
    
    public void testFailIfNoTestsForkModeOnce() throws Exception 
    {
        doTest( "once", true );
    }
    
    public void testDontFailIfNoTestsForkModeAlways () throws Exception
    {
        doTest("always", false);
    }
    
    public void testDontFailIfNoTestsForkModeNever() throws Exception 
    {
        doTest( "never", false );
    }
    
    public void testDontFailIfNoTestsForkModeOnce() throws Exception 
    {
        doTest( "once", false );
    }
    
    private void doTest(String forkMode, boolean failIfNoTests)
        throws IOException, VerificationException, MavenReportException
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/default-configuration-classWithNoTests" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List goals = this.getInitialGoals();
        goals.add( "test" );
        goals.add( "-DforkMode=" + forkMode );
        goals.add( "-DfailIfNoTests=" + failIfNoTests );
        if (failIfNoTests)
        {
            try 
            {
                verifier.executeGoals( goals );
                verifier.resetStreams();
                verifier.verifyErrorFreeLog();
                fail( "Build did not fail, but it should have" );
            } catch (VerificationException e )
            {
                // this is what we expected
            }
        }
        else
        {
            verifier.executeGoals( goals );
            verifier.resetStreams();
            verifier.verifyErrorFreeLog();
            HelperAssertions.assertTestSuiteResults( 0, 0, 0, 0, testDir );
        }
    }
}
