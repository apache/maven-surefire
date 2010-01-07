package org.apache.maven.surefire.its;


import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * Test support for POJO tests.
 * 
 * @author Benjamin Bentmann
 */
public class PojoSimpleIT
    extends AbstractSurefireIntegrationTestClass
{

    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/pojo-simple" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        executeGoal( verifier, "test" );
        verifier.resetStreams();
        
        HelperAssertions.assertTestSuiteResults( 2, 0, 1, 0, testDir );        
    }

}
