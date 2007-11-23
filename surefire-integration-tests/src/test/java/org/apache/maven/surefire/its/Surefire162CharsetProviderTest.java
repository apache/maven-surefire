package org.apache.maven.surefire.its;

import java.io.File;

import org.apache.maven.integrationtests.AbstractMavenIntegrationTestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Test charset provider (SUREFIRE-162)
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class Surefire162CharsetProviderTest
    extends AbstractMavenIntegrationTestCase
{
    public void testCharsetProvider ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/surefire-162-charsetProvider" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, testDir );        
    }
}
