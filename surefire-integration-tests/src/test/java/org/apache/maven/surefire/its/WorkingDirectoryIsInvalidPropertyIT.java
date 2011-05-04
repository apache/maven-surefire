package org.apache.maven.surefire.its;

import java.io.File;
import java.io.IOException;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Test when the configured working directory is an invalid property, SUREFIRE-715
 */
public class WorkingDirectoryIsInvalidPropertyIT
    extends AbstractSurefireIntegrationTestClass
{
    private File testDir;

    public void setUp()
        throws IOException
    {
        testDir = ResourceExtractor.simpleExtractResources( getClass(), "/working-directory-is-invalid-property" );
    }

    public void testWorkingDirectory()
        throws Exception
    {
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        try
        {
            executeGoal( verifier, "test" );
        }
        catch ( VerificationException e )
        {
        }
        verifier.verifyTextInLog( "workingDirectory cannot be null" );
        verifier.resetStreams();
    }
}