package org.apache.maven.surefire.api.booter;

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

import org.apache.maven.surefire.api.stream.AbstractStreamDecoder.Segment;

import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Objects.requireNonNull;

/**
 * Commands which are sent from plugin to the forked jvm.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public enum MasterProcessCommand
{
    RUN_CLASS( "run-testclass", String.class ),
    TEST_SET_FINISHED( "testset-finished", Void.class ),
    SKIP_SINCE_NEXT_TEST( "skip-since-next-test", Void.class ),
    SHUTDOWN( "shutdown", String.class ),

    /** To tell a forked process that the master process is still alive. Repeated after 10 seconds. */
    NOOP( "noop", Void.class ),
    BYE_ACK( "bye-ack", Void.class );

    // due to have fast and thread-safe Map
    public static final Map<Segment, MasterProcessCommand> COMMAND_TYPES = segmentsToCmds();

    private final String opcode;
    private final byte[] opcodeBinary;
    private final Class<?> dataType;

    MasterProcessCommand( String opcode, Class<?> dataType )
    {
        this.opcode = requireNonNull( opcode, "value cannot be null" );
        opcodeBinary = opcode.getBytes( US_ASCII );
        this.dataType = requireNonNull( dataType, "dataType cannot be null" );
    }

    public byte[] getOpcodeBinary()
    {
        return opcodeBinary;
    }

    public int getOpcodeLength()
    {
        return opcodeBinary.length;
    }

    public Class<?> getDataType()
    {
        return dataType;
    }

    public boolean hasDataType()
    {
        return dataType != Void.class;
    }

    @Override
    public String toString()
    {
        return opcode;
    }

    private static Map<Segment, MasterProcessCommand> segmentsToCmds()
    {
        Map<Segment, MasterProcessCommand> commands = new HashMap<>();
        for ( MasterProcessCommand command : MasterProcessCommand.values() )
        {
            byte[] array = command.toString().getBytes( US_ASCII );
            commands.put( new Segment( array, 0, array.length ), command );
        }
        return commands;
    }
}
