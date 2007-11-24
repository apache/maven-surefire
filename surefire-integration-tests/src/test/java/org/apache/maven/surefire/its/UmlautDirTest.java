package org.apache.maven.surefire.its;



import java.io.File;

import org.apache.maven.integrationtests.AbstractMavenIntegrationTestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Test a directory with an umlaut ‹
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class UmlautDirTest
    extends AbstractMavenIntegrationTestCase
{
    public void testUmlaut ()
        throws Exception
    {
        String tempDirPath = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File tempDir = new File(tempDirPath);
        File targetDir = new File("target");
        if (targetDir.exists() && targetDir.isDirectory()) {
            tempDir = targetDir;
        }
        File testDir = new File( tempDir, "/junit-pathWith‹mlaut" );
        FileUtils.deleteDirectory( testDir );
        testDir = ResourceExtractor.extractResourcePath(getClass(), "/junit-pathWithUmlaut", testDir, true);

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, testDir );
    }
}
