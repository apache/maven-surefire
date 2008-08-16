package org.apache.maven.surefire.its;


import java.io.File;
import java.util.ArrayList;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Test library using a conflicting version of plexus-utils
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 *
 */
public class PlexusConflictTest
    extends AbstractSurefireIT
{
    public void testPlexusConflict ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/plexus-conflict" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        this.executeGoal( verifier, "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, testDir );
    }

    public void testPlexusConflictIsolatedClassLoader ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/plexus-conflict" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        ArrayList goals = this.getInitialGoals();
        goals.add( "test" );
        goals.add( "-Dsurefire.useSystemClassLoader=false" );
        verifier.executeGoals( goals );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, testDir );
    }
}