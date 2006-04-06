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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Commandline class copied from plexus-utils with fix for PLX-161, as we can not upgrade plexus-utils until it's upgraded in core Maven
 * 
 * TODO deprecate when plexus-utils 1.2 can be used
 * 
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 */
public class Commandline
    extends org.codehaus.plexus.util.cli.Commandline
{

    private String shell = null;

    private String[] shellArgs = null;

    public Commandline()
    {
        super();
        setDefaultShell();
    }

    /**
     * <p>Sets the shell or command-line interpretor for the detected operating system,
     * and the shell arguments.</p>
     */
    private void setDefaultShell()
    {
        String os = System.getProperty( OS_NAME );

        //If this is windows set the shell to command.com or cmd.exe with correct arguments.
        if ( os.indexOf( WINDOWS ) != -1 )
        {
            if ( os.indexOf( "95" ) != -1 || os.indexOf( "98" ) != -1 || os.indexOf( "Me" ) != -1 )
            {
                setShell( "COMMAND.COM" );
                setShellArgs( new String[] { "/C" } );
            }
            else
            {
                setShell( "CMD.EXE" );
                setShellArgs( new String[] { "/X", "/C" } );
            }
        }
    }

    /**
     * Returns the shell, executable and all defined arguments.
     */
    public String[] getShellCommandline()
    {
        List commandLine = new ArrayList();

        if ( shell != null )
        {
            commandLine.add( getShell() );
        }

        if ( getShellArgs() != null )
        {
            commandLine.addAll( Arrays.asList( getShellArgs() ) );
        }

        if ( getShell() == null )
        {
            if ( executable != null )
            {
                commandLine.add( executable );
            }
            commandLine.addAll( Arrays.asList( getArguments() ) );
        }
        else
        {
            /* When using a shell we need to quote the full command */
            StringBuffer sb = new StringBuffer();
            sb.append( "\"" );
            if ( executable != null )
            {
                sb.append( "\"" );
                sb.append( executable );
                sb.append( "\"" );
            }
            for ( int i = 0; i < getArguments().length; i++ )
            {
                sb.append( " \"" );
                sb.append( getArguments()[i] );
                sb.append( "\"" );
            }
            sb.append( "\"" );
            commandLine.add( sb.toString() );
        }

        return (String[]) commandLine.toArray( new String[0] );
    }

    /**
     * <p>
     * Set the shell command to use. If not set explicitly the class will autodetect it from the operating system name
     * </p>
     * <p>
     * eg. <code>COMMAND.COM</code> in Win9x and WinMe or <code>CMD.EXE</code> in WinNT, Win2000 or WinXP
     * </p>
     * @since 1.2
     * @param shell shell command
     */
    public void setShell( String shell )
    {
        this.shell = shell;
    }

    /**
     * Get the shell command to use
     * @since 1.2
     * @return
     */
    public String getShell()
    {
        return shell;
    }

    /**
     * <p>
     * Shell arguments to use when using a shell command. If not set explicitly the class will autodetect it from the operating system name
     * </p>
     * <p>
     * eg. <code>/C</code> for <code>COMMAND.COM</code> and <code>/X /C</code> for <code>CMD.EXE</code>
     * </p>
     * @see setShell
     * @since 1.2
     * @param shellArgs
     */
    public void setShellArgs( String[] shellArgs )
    {
        this.shellArgs = shellArgs;
    }

    /**
     * Get the shell arguments to use with the shell command
     * @since 1.2
     * @return the arguments
     */
    public String[] getShellArgs()
    {
        return shellArgs;
    }

}
