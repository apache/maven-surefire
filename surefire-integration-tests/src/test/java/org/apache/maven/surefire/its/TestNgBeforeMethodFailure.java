package org.apache.maven.surefire.its;


import junit.framework.TestCase;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * Test failures in @BeforeMethod annotation on TestNg suite
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class TestNgBeforeMethodFailure
    extends TestCase
{
    public void testTestNgBeforeMethodFailure ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/testng-beforeMethodFailure" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        try
        {
            verifier.executeGoal( "test" );
            verifier.resetStreams();
            verifier.verifyErrorFreeLog();
            fail( "Build didn't fail, but it should" );
        }
        catch ( VerificationException e )
        {
            // as expected
        }
        finally
        {
            verifier.resetStreams();
        }
        
        HelperAssertions.assertTestSuiteResults( 2, 0, 1, 1, testDir );
    }
}
