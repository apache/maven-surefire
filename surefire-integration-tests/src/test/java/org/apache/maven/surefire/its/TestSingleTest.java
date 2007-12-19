package org.apache.maven.surefire.its;


import junit.framework.TestCase;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Test running a single test with -Dtest=BasicTest
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class TestSingleTest
    extends TestCase
{
    public void testSingleTest ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/default-configuration" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List goals = new ArrayList();
        goals.add( "test" );
        goals.add( "-Dtest=BasicTest" );
        verifier.executeGoals( goals );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, testDir );        
    }
    
    public void testSingleTestDotJava()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/default-configuration" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List goals = new ArrayList();
        goals.add( "test" );
        goals.add( "-Dtest=BasicTest.java" );
        verifier.executeGoals( goals );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, testDir );
    }

    public void testSingleTestNonExistent()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/default-configuration" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List goals = new ArrayList();
        goals.add( "test" );
        goals.add( "-Dtest=DoesNotExist" );

        try
        {
            verifier.executeGoals( goals );
            verifier.verifyErrorFreeLog();
            fail( "Build should have failed" );
        }
        catch ( VerificationException e )
        {
            // as expected
        }
        finally
        {
            verifier.resetStreams();
        }
        
        File reportsDir = new File( testDir, "target/surefire-reports" );
        assertFalse ( "Unexpected reports directory", reportsDir.exists() );
    }

    public void testSingleTestNonExistentOverride()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/default-configuration" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List goals = new ArrayList();
        goals.add( "test" );
        goals.add( "-Dtest=DoesNotExist" );
        goals.add( "-DfailIfNoTests=false" );
        verifier.executeGoals( goals );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        File reportsDir = new File( testDir, "target/surefire-reports" );
        assertFalse ( "Unexpected reports directory", reportsDir.exists() );
    }
}
