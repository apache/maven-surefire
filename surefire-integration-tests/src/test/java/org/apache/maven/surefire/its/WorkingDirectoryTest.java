package org.apache.maven.surefire.its;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Test working directory configuration, SUREFIRE-416
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class WorkingDirectoryTest
    extends TestCase
{
    
    private File testDir;
    private File childTestDir;
    private File targetDir;
    private File outFile;

    public void setUp() throws IOException {
        testDir = ResourceExtractor.simpleExtractResources( getClass(), "/working-directory" );
        childTestDir = new File( testDir, "child" );
        targetDir = new File( childTestDir, "target" );
        outFile = new File( targetDir, "out.txt" );
        outFile.delete();
    }
    
    public void testWorkingDirectory ()
        throws Exception
    {
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, childTestDir );
        verifyOutputDirectory( childTestDir );
    }
    
    public void verifyOutputDirectory( File childTestDir )
        throws IOException
    {
        assertTrue( "out.txt doesn't exist: " + outFile.getAbsolutePath(), outFile.exists() );
        Properties p = new Properties();
        FileInputStream is = new FileInputStream( outFile );
        p.load( is );
        is.close();
        String userDirPath = p.getProperty( "user.dir" );
        assertNotNull( "user.dir was null in property file", userDirPath );
        File userDir = new File( userDirPath );
        assertEquals( "wrong user.dir", childTestDir.getAbsolutePath(), userDir.getAbsolutePath() );
    }
    
    public void testWorkingDirectoryNoFork()
        throws Exception
    {
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        ArrayList goals = new ArrayList();
        goals.add( "test");
        goals.add( "-DforkMode=never" );
        verifier.executeGoals( goals );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, childTestDir );
        verifyOutputDirectory( childTestDir );
    }
    
    public void testWorkingDirectoryChildOnly()
        throws Exception
    {   
        Verifier verifier = new Verifier( childTestDir.getAbsolutePath() );
        verifier.executeGoal( "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, childTestDir );
        verifyOutputDirectory( childTestDir );
    }

    
    
    public void testWorkingDirectoryChildOnlyNoFork()
        throws Exception
    {
        
        Verifier verifier = new Verifier( childTestDir.getAbsolutePath() );
        ArrayList goals = new ArrayList();
        goals.add( "test");
        goals.add( "-DforkMode=never" );
        verifier.executeGoals( goals );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, childTestDir );
        verifyOutputDirectory( childTestDir );
    }
}
