package org.apache.maven.surefire.its;


import org.apache.maven.integrationtests.AbstractMavenIntegrationTestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Test a directory with an umlaut ‹
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class UmlautDirTest
    extends AbstractMavenIntegrationTestCase
{
    File testDir;

    public void testUmlaut ()
        throws Exception
    {
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, testDir );
    }

    public void testUmlautIsolatedClassLoader ()
        throws Exception
    {
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        ArrayList goals = new ArrayList();
        goals.add( "test" );
        goals.add( "-DuseSystemClassLoader=false" );
        verifier.executeGoals( goals );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, testDir );
    }

    public void setUp()
        throws IOException
    {
        String tempDirPath = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File tempDir = new File(tempDirPath);
        File targetDir = new File("target").getAbsoluteFile();
        if (targetDir.exists() && targetDir.isDirectory()) {
            tempDir = targetDir;
        }
        testDir = new File( tempDir, "/junit-pathWith‹mlaut" );
        FileUtils.deleteDirectory( testDir );
        testDir = ResourceExtractor.extractResourcePath(getClass(), "/junit-pathWithUmlaut", testDir, true);
    }
}
