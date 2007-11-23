package org.apache.maven.surefire.its;

import java.io.File;

import org.apache.maven.integrationtests.AbstractMavenIntegrationTestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Test JUnit test that contains inner classes
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class Surefire42NotExtendingTestCase
    extends AbstractMavenIntegrationTestCase
{
    public void testInnerClass ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/junit-notExtendingTestCase" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, testDir );        
    }
}
