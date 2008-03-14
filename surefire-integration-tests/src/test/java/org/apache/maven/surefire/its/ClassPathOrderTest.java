package org.apache.maven.surefire.its;


import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * Test for checking the order of class path elements
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class ClassPathOrderTest
    extends TestCase
{
    public void testClassPathOrder ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/classpath-order" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        HelperAssertions.assertTestSuiteResults( 2, 0, 0, 0, testDir );        
    }
}
