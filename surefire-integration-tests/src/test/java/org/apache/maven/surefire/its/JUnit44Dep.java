package org.apache.maven.surefire.its;

import java.io.File;

import org.apache.maven.integrationtests.AbstractMavenIntegrationTestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Test project using JUnit4.4 -dep.  junit-dep includes only junit.* classes, and depends explicitly on hamcrest-core
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class JUnit44Dep
    extends AbstractMavenIntegrationTestCase
{
    public void testJUnit44Dep ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/junit44-dep" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, testDir );
    }
}
