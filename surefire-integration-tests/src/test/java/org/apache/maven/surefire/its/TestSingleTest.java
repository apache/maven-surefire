package org.apache.maven.surefire.its;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.integrationtests.AbstractMavenIntegrationTestCase;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Test running a single test with -Dtest=BasicTest
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class TestSingleTest
    extends AbstractMavenIntegrationTestCase
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
        verifier.executeGoals( goals );
        verifier.resetStreams();
        try {
            verifier.verifyErrorFreeLog();
            fail( "Build should have failed" );
        } catch (VerificationException e) {
            // as expected
        }
        
        File reportsDir = new File( testDir, "target/surefire-reports" );
        assertFalse ( "Unexpected reports directory", reportsDir.exists() );
    }
    
}
