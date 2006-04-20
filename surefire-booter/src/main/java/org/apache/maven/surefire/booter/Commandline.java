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

import org.apache.maven.surefire.booter.shell.CmdShell;
import org.apache.maven.surefire.booter.shell.CommandShell;
import org.apache.maven.surefire.booter.shell.Shell;

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

    private Shell shell;

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
                setShell( new CommandShell() );
            }
            else
            {
                setShell( new CmdShell() );
            }
        }
    }

    /**
     * Returns the shell, executable and all defined arguments.
     */
    public String[] getShellCommandline()
    {

        if ( getShell() == null )
        {
            if ( executable != null )
            {
                List commandLine = new ArrayList();
                commandLine.add( executable );
                commandLine.addAll( Arrays.asList( getArguments() ) );
                return (String[]) commandLine.toArray( new String[0] );
            }
            else
            {
                return getArguments();
            }

        }
        else
        {
            return (String[]) getShell().getShellCommandLine( executable, getArguments() ).toArray( new String[0] );
        }
    }

    /**
     * Allows to set the shell to be used in this command line.
     *
     * @param shell
     * @since 1.2
     */
    public void setShell( Shell shell )
    {
        this.shell = shell;
    }

    /**
     * Get the shell to be used in this command line.
     *
     * @since 1.2
     */
    public Shell getShell()
    {
        return shell;
    }

}
