package org.apache.maven.surefire.booter;

import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.Commandline;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import junit.framework.TestCase;

public class ForkConfigurationTest
    extends TestCase
{

    public void testCreateCommandLine_UseSystemClassLoaderForkOnce_ShouldConstructManifestOnlyJar()
        throws IOException, SurefireBooterForkException
    {
        ForkConfiguration config = new ForkConfiguration();
        config.setForkMode( ForkConfiguration.FORK_ONCE );
        config.setUseSystemClassLoader( true );
        config.setWorkingDirectory( new File( "." ).getCanonicalFile() );
        config.setJvmExecutable( "java" );

        File cpElement = File.createTempFile( "ForkConfigurationTest.", ".file" );
        cpElement.deleteOnExit();

        Commandline cli = config.createCommandLine( Collections.singletonList( cpElement.getAbsolutePath() ), config.isUseSystemClassLoader() );

        String line = StringUtils.join( cli.getCommandline(), " " );
        assertTrue( line.indexOf( "-jar" ) > -1 );
    }

}
