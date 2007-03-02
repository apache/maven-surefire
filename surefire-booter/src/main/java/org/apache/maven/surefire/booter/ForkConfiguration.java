package org.apache.maven.surefire.booter;

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

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.Commandline;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Configuration for forking tests.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a>
 */
public class ForkConfiguration
{
    public static final String FORK_ONCE = "once";

    public static final String FORK_ALWAYS = "always";

    public static final String FORK_NEVER = "never";

    private String forkMode;

    private Properties systemProperties;

    private String jvmExecutable;

    private String argLine;

    private Map environmentVariables;

    private File workingDirectory;

    private boolean debug;

    public void setForkMode( String forkMode )
    {
        if ( "pertest".equalsIgnoreCase( forkMode ) )
        {
            this.forkMode = FORK_ALWAYS;
        }
        else if ( "none".equalsIgnoreCase( forkMode ) )
        {
            this.forkMode = FORK_NEVER;
        }
        else if ( forkMode.equals( FORK_NEVER ) || forkMode.equals( FORK_ONCE ) || forkMode.equals( FORK_ALWAYS ) )
        {
            this.forkMode = forkMode;
        }
        else
        {
            throw new IllegalArgumentException( "Fork mode " + forkMode + " is not a legal value" );
        }
    }

    public boolean isForking()
    {
        return !FORK_NEVER.equals( forkMode );
    }

    public void setSystemProperties( Properties systemProperties )
    {
        this.systemProperties = (Properties) systemProperties.clone();
    }

    public void setJvmExecutable( String jvmExecutable )
    {
        this.jvmExecutable = jvmExecutable;
    }

    public void setArgLine( String argLine )
    {
        this.argLine = argLine;
    }

    public void setEnvironmentVariables( Map environmentVariables )
    {
        this.environmentVariables = new HashMap( environmentVariables );
    }

    public void setWorkingDirectory( File workingDirectory )
    {
        this.workingDirectory = workingDirectory;
    }

    public String getForkMode()
    {
        return forkMode;
    }

    public Properties getSystemProperties()
    {
        return systemProperties;
    }

    /**
     * @throws SurefireBooterForkException
     * @deprecated use the 2-arg alternative.
     */
    public Commandline createCommandLine( List classPath )
        throws SurefireBooterForkException
    {
        return createCommandLine( classPath, false );
    }

    public Commandline createCommandLine( List classPath, boolean useJar )
        throws SurefireBooterForkException
    {
        Commandline cli = new Commandline();

        cli.setExecutable( jvmExecutable );

        if ( argLine != null )
        {
            cli.addArguments( StringUtils.split( argLine, " " ) );
        }

        if ( environmentVariables != null )
        {
            Iterator iter = environmentVariables.keySet().iterator();

            while ( iter.hasNext() )
            {
                String key = (String) iter.next();

                String value = (String) environmentVariables.get( key );

                cli.addEnvironment( key, value );
            }
        }

        if ( System.getProperty( "maven.surefire.debug" ) != null )
        {
            cli.createArgument().setLine(
                "-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005" );
        }

        if ( useJar )
        {
            File jarFile;
            try
            {
                jarFile = createJar( classPath );
            }
            catch ( IOException e )
            {
                throw new SurefireBooterForkException( "Error creating archive file", e );
            }
            catch ( ManifestException e )
            {
                throw new SurefireBooterForkException( "Error creating manifest", e );
            }
            catch ( ArchiverException e )
            {
                throw new SurefireBooterForkException( "Error creating archive", e );
            }

            cli.createArgument().setValue( "-jar" );

            cli.createArgument().setValue( jarFile.getAbsolutePath() );
        }
        else
        {
            cli.createArgument().setValue( "-classpath" );

            cli.createArgument().setValue( StringUtils.join( classPath.iterator(), File.pathSeparator ) );

            cli.createArgument().setValue( SurefireBooter.class.getName() );
        }

        cli.setWorkingDirectory( workingDirectory.getAbsolutePath() );

        return cli;
    }

    /**
     * Create a jar with just a manifest containing a Main-Class entry for SurefireBooter and a Class-Path entry
     * for all classpath elements.
     *
     * @param classPath List&lt;String> of all classpath elements.
     * @return
     * @throws IOException
     * @throws ManifestException
     * @throws ArchiverException
     */
    private static File createJar( List classPath )
        throws IOException, ManifestException, ArchiverException
    {
        JarArchiver jar = new JarArchiver();
        jar.setCompress( false ); // for speed
        File file = File.createTempFile( "surefirebooter", ".jar" );
        file.deleteOnExit();
        jar.setDestFile( file );

        Manifest manifest = new Manifest();

        // we can't use StringUtils.join here since we need to add a '/' to
        // the end of directory entries - otherwise the jvm will ignore them.
        String cp = "";
        for ( Iterator it = classPath.iterator(); it.hasNext(); )
        {
            String el = (String) it.next();
            cp += " " + el + ( new File( el ).isDirectory() ? "/" : "" );
        }

        Manifest.Attribute attr = new Manifest.Attribute( "Class-Path", cp.trim() );
        manifest.addConfiguredAttribute( attr );

        attr = new Manifest.Attribute( "Main-Class", SurefireBooter.class.getName() );
        manifest.addConfiguredAttribute( attr );

        jar.addConfiguredManifest( manifest );

        jar.createArchive();

        return file;
    }

    public void setDebug( boolean debug )
    {
        this.debug = debug;
    }

    public boolean isDebug()
    {
        return debug;
    }
}
