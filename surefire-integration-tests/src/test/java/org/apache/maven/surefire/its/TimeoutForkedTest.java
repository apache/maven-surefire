package org.apache.maven.surefire.its;

import java.io.File;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Test 
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class TimeoutForkedTest
    extends AbstractSurefireIT
{
    public void testTimeoutForked()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/timeout-forked" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        try
        {
            this.executeGoal( verifier, "test" );
            verifier.verifyErrorFreeLog();
            fail( "Build didn't fail, but it should have" );
        }
        catch ( VerificationException e )
        {
            // as expected
        }
        finally
        {
            verifier.resetStreams();
        }

    }
}
