package org.apache.maven.surefire.its;

import java.io.File;

import org.apache.maven.integrationtests.AbstractMavenIntegrationTestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Test useSystemClassLoader option
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class TestUseSystemClassLoader
    extends AbstractMavenIntegrationTestCase
{
    public void testDefaultConfiguration ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/system-classloader" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, testDir );        
    }
}
