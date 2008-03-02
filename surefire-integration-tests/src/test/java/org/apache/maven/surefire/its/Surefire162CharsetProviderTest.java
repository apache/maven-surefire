package org.apache.maven.surefire.its;


import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;

/**
 * Test charset provider (SUREFIRE-162)
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class Surefire162CharsetProviderTest
    extends TestCase
{
    public void testCharsetProvider ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/surefire-162-charsetProvider" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        File jarFile = new File( verifier.getArtifactPath( "jcharset", "jcharset", "1.2.1", "jar" ) );
        jarFile.getParentFile().mkdirs();
        FileUtils.copyFile( new File( testDir, "repo/jcharset/jcharset/1.2.1/jcharset-1.2.1.jar" ), jarFile );
        FileUtils.copyFile( new File( testDir, "repo/jcharset/jcharset/1.2.1/jcharset-1.2.1.pom" ), new File( verifier.getArtifactPath( "jcharset", "jcharset", "1.2.1", "pom" ) ) );
        verifier.executeGoal( "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, testDir );        
    }
}
