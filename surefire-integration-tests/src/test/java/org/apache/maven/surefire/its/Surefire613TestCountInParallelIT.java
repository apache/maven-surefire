package org.apache.maven.surefire.its;


import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * SUREFIRE-613 Asserts proper test counts when running in parallel
 *
 * @author Kristian Rosenvold
 *
 */
public class Surefire613TestCountInParallelIT
    extends AbstractSurefireIntegrationTestClass
{
    public void testPaallelBuildResultCount()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/surefire-613-testCount-in-parallel" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        String[] opts = {"-fn"};
        verifier.setCliOptions( new ArrayList(Arrays.asList( opts)) );
        this.executeGoal( verifier, "test" );
        verifier.resetStreams();

        HelperAssertions.assertTestSuiteResults( 7, 1, 2, 1, testDir );
    }
}