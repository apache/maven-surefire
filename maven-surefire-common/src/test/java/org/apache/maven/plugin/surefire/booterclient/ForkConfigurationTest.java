package org.apache.maven.plugin.surefire.booterclient;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import junit.framework.TestCase;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.SurefireBooterForkException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.Commandline;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class ForkConfigurationTest
    extends TestCase
{

    public void testCreateCommandLine_UseSystemClassLoaderForkOnce_ShouldConstructManifestOnlyJar()
        throws IOException, SurefireBooterForkException
    {
        ForkConfiguration config = getForkConfiguration();
        File cpElement = getTempClasspathFile();
        config.setForkMode( ForkConfiguration.FORK_ONCE );
        config.setJvmExecutable( "java" );

        Commandline cli = config.createCommandLine( Collections.singletonList( cpElement.getAbsolutePath() ), true );

        String line = StringUtils.join( cli.getCommandline(), " " );
        assertTrue( line.indexOf( "-jar" ) > -1 );
    }

    public void testArglineWithNewline()
        throws IOException, SurefireBooterForkException
    {
        // SUREFIRE-657
        File cpElement = getTempClasspathFile();
        ForkConfiguration forkConfiguration = getForkConfiguration();

        forkConfiguration.setArgLine( "abc\ndef" );

        final Commandline commandLine =
            forkConfiguration.createCommandLine( Collections.singletonList( cpElement.getAbsolutePath() ), false );
        assertTrue( commandLine.toString().contains( "abc def" ) );
    }

    private File getTempClasspathFile()
        throws IOException
    {
        File cpElement = File.createTempFile( "ForkConfigurationTest.", ".file" );
        cpElement.deleteOnExit();
        return cpElement;
    }

    public static ForkConfiguration getForkConfiguration()
        throws IOException
    {
        ForkConfiguration forkConfiguration = new ForkConfiguration( new Classpath() );
        forkConfiguration.setWorkingDirectory( new File( "." ).getCanonicalFile() );
        return forkConfiguration;
    }

}
