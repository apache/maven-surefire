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

import static java.util.Objects.requireNonNull;
import static org.apache.maven.surefire.util.internal.StringUtils.isBlank;
import static org.apache.maven.surefire.booter.MasterProcessCommand.RUN_CLASS;
import static org.apache.maven.surefire.booter.MasterProcessCommand.SHUTDOWN;
import static org.apache.maven.surefire.booter.Shutdown.DEFAULT;

/**
 * Encapsulates data and command sent from master to forked process.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public final class Command
{
    public static final Command TEST_SET_FINISHED = new Command( MasterProcessCommand.TEST_SET_FINISHED );
    public static final Command SKIP_SINCE_NEXT_TEST = new Command( MasterProcessCommand.SKIP_SINCE_NEXT_TEST );
    public static final Command NOOP = new Command( MasterProcessCommand.NOOP );
    public static final Command BYE_ACK = new Command( MasterProcessCommand.BYE_ACK );

    private final MasterProcessCommand command;
    private final String data;

    public Command( MasterProcessCommand command, String data )
    {
        this.command = requireNonNull( command );
        this.data = data;
    }

    public static Command toShutdown( Shutdown shutdownType )
    {
        return new Command( SHUTDOWN, shutdownType.name() );
    }

    public static Command toRunClass( String runClass )
    {
        return new Command( RUN_CLASS, runClass );
    }

    public Command( MasterProcessCommand command )
    {
        this( command, null );
    }

    public MasterProcessCommand getCommandType()
    {
        return command;
    }

    public String getData()
    {
        return data;
    }

    /**
     * @return {@link Shutdown} or {@link Shutdown#DEFAULT} if {@link #getData()} is null or blank string
     * @throws IllegalArgumentException if string data {@link #getData()} is not applicable to enum {@link Shutdown}
     */
    public Shutdown toShutdownData()
    {
        if ( !isType( SHUTDOWN ) )
        {
            throw new IllegalStateException( "expected MasterProcessCommand.SHUTDOWN" );
        }
        return isBlank( data ) ? DEFAULT : Shutdown.valueOf( data );
    }

    public boolean isType( MasterProcessCommand command )
    {
        return command == this.command;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }

        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        Command arg = (Command) o;

        return command == arg.command && ( data == null ? arg.data == null : data.equals( arg.data ) );
    }

    @Override
    public int hashCode()
    {
        int result = command.hashCode();
        result = 31 * result + ( data != null ? data.hashCode() : 0 );
        return result;
    }
}
