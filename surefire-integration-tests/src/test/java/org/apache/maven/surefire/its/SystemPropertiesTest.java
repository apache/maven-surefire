package org.apache.maven.surefire.its;


import java.io.File;
import java.util.ArrayList;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Test system properties
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 *
 */
public class SystemPropertiesTest
    extends AbstractSurefireIT
{
    public void testSystemProperties ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/system-properties" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        ArrayList goals = getInitialGoals();
        goals.add( "test" );
        // SUREFIRE-121... someday we should re-enable this
        // goals.add( "-DsetOnMavenCommandLine=baz" );
        verifier.executeGoals( goals );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        HelperAssertions.assertTestSuiteResults( 5, 0, 0, 0, testDir );
    }

    public void testSystemPropertiesNoFork()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/system-properties" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        ArrayList goals = getInitialGoals();
        goals.add( "test" );
        goals.add( "-DforkMode=never" );
        // SUREFIRE-121... someday we should re-enable this
        // goals.add( "-DsetOnMavenCommandLine=baz" );
        // DGF fake the argLine, since we're not forking
        goals.add( "-DsetOnArgLine=bar" );
        verifier.executeGoals( goals );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        HelperAssertions.assertTestSuiteResults( 5, 0, 0, 0, testDir );
    }
}
