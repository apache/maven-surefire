package org.apache.maven.surefire.booter.shell;

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

import org.apache.maven.surefire.util.NestedRuntimeException;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.Commandline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p/>
 * Class with patches copied from plexus-utils with fix for PLX-161,
 * as we can not upgrade plexus-utils until it's upgraded in core Maven
 * </p>
 * <p/>
 * TODO deprecate when plexus-utils 1.2 can be used
 *
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 */
public class Shell
{
    private String shellCommand;

    private String[] shellArgs;

    /**
     * Set the command to execute the shell (eg. COMMAND.COM, /bin/bash,...)
     *
     * @param shellCommand
     */
    public void setShellCommand( String shellCommand )
    {
        this.shellCommand = shellCommand;
    }

    /**
     * Get the command to execute the shell
     *
     * @return
     */
    public String getShellCommand()
    {
        return shellCommand;
    }

    /**
     * Set the shell arguments when calling a command line (not the executable arguments)
     * (eg. /X /C for CMD.EXE)
     *
     * @param shellArgs
     */
    public void setShellArgs( String[] shellArgs )
    {
        this.shellArgs = shellArgs;
    }

    /**
     * Get the shell arguments
     *
     * @return
     */
    public String[] getShellArgs()
    {
        return shellArgs;
    }

    /**
     * Get the command line for the provided executable and arguments in this shell
     *
     * @param executable executable that the shell has to call
     * @param arguments  arguments for the executable, not the shell
     * @return List with one String object with executable and arguments quoted as needed
     */
    public List getCommandLine( String executable, String[] arguments )
    {

        List commandLine = new ArrayList();
        try
        {
            StringBuffer sb = new StringBuffer();

            if ( executable != null )
            {
                sb.append( Commandline.quoteArgument( executable ) );
            }
            for ( int i = 0; i < arguments.length; i++ )
            {
                sb.append( " " );
                sb.append( Commandline.quoteArgument( arguments[i] ) );
            }

            commandLine.add( sb.toString() );
        }
        catch ( CommandLineException e )
        {
            throw new NestedRuntimeException( e );
        }

        return commandLine;
    }

    /**
     * Get the full command line to execute, including shell command, shell arguments,
     * executable and executable arguments
     *
     * @param executable executable that the shell has to call
     * @param arguments  arguments for the executable, not the shell
     * @return List of String objects, whose array version is suitable to be used as argument
     *         of Runtime.getRuntime().exec()
     */
    public List getShellCommandLine( String executable, String[] arguments )
    {

        List commandLine = new ArrayList();

        if ( getShellCommand() != null )
        {
            commandLine.add( getShellCommand() );
        }

        if ( getShellArgs() != null )
        {
            commandLine.addAll( Arrays.asList( getShellArgs() ) );
        }

        commandLine.addAll( getCommandLine( executable, arguments ) );

        return commandLine;

    }

}
