package org.apache.maven.surefire.booter;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Configuration for forking tests.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ForkConfiguration
{
    public static final String FORK_ONCE = "once";

    public static final String FORK_PERTEST = "pertest";

    public static final String FORK_NONE = "none";

    private String forkMode;

    private boolean childDelegation;

    private Properties systemProperties;

    private String jvmExecutable;

    private String argLine;

    private Map environmentVariables;

    private File workingDirectory;

    private boolean debug;

    public void setForkMode( String forkMode )
    {
        if ( forkMode.equals( FORK_NONE ) || forkMode.equals( FORK_ONCE ) || forkMode.equals( FORK_PERTEST ) )
        {
            this.forkMode = forkMode;
        }
        else
        {
            throw new IllegalArgumentException( "Fork mode " + forkMode + " is not a legal value" );
        }
    }

    public void setChildDelegation( boolean childDelegation )
    {
        this.childDelegation = childDelegation;
    }

    public boolean isChildDelegation()
    {
        return childDelegation;
    }

    public boolean isForking()
    {
        return !FORK_NONE.equals( forkMode );
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

    public Commandline createCommandLine( List classPath )
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

        cli.createArgument().setValue( "-classpath" );

        cli.createArgument().setValue( StringUtils.join( classPath.iterator(), File.pathSeparator ) );

        cli.createArgument().setValue( SurefireBooter.class.getName() );

        cli.setWorkingDirectory( workingDirectory.getAbsolutePath() );

        return cli;
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
