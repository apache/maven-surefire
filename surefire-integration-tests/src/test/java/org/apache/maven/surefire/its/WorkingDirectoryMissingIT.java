package org.apache.maven.surefire.its;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Test when the configured working directory does not exist, SUREFIRE-607
 * 
 * @author <a href="mailto:stephenc@apache.org">Stephen Connolly</a>
 * 
 */
public class WorkingDirectoryMissingIT
    extends AbstractSurefireIntegrationTestClass
{

    private File testDir;

    public void setUp()
        throws IOException
    {
        testDir = ResourceExtractor.simpleExtractResources( getClass(), "/working-directory-missing" );
    }

    public void testWorkingDirectory()
        throws Exception
    {
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        this.executeGoal( verifier, "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

}
