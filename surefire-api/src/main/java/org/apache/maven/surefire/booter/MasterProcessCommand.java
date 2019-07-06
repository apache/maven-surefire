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

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Objects.requireNonNull;

/**
 * Commands which are sent from plugin to the forked jvm.
 * Support and methods related to the commands.
 * <br>
 *     <br>
 * magic number : opcode [: opcode specific data]*
 * <br>
 *     or data encoded with Base64
 * <br>
 * magic number : opcode [: Base64(opcode specific data)]*
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

    private static final String MAGIC_NUMBER = ":maven-surefire-std-out:";

    private final String opcodeName;

    private final Class<?> dataType;

    MasterProcessCommand( String opcodeName, Class<?> dataType )
    {
        this.opcodeName = opcodeName;
        this.dataType = requireNonNull( dataType, "dataType cannot be null" );
    }

    public String getOpcode()
    {
        return opcodeName;
    }

    public Class<?> getDataType()
    {
        return dataType;
    }

    public boolean hasDataType()
    {
        return dataType != Void.class;
    }

    public static MasterProcessCommand byOpcode( String opcode )
    {
        for ( MasterProcessCommand cmd : values() )
        {
            if ( cmd.opcodeName.equals( opcode ) )
            {
                return cmd;
            }
        }
        return null;
    }

    public byte[] encode( String data )
    {
        if ( !hasDataType() )
        {
            throw new IllegalArgumentException( "cannot use data without data type" );
        }

        if ( getDataType() != String.class )
        {
            throw new IllegalArgumentException( "Data type can be only " + String.class );
        }

        return encode( opcodeName, data )
                .toString()
                .getBytes( US_ASCII );
    }

    public byte[] encode()
    {
        if ( getDataType() != Void.class )
        {
            throw new IllegalArgumentException( "Data type can be only " + getDataType() );
        }

        return encode( opcodeName, null )
                .toString()
                .getBytes( US_ASCII );
    }

    /**
     * Encodes opcode and data.
     *
     * @param operation opcode
     * @param data   data
     * @return encoded command
     */
    private static StringBuilder encode( String operation, String data )
    {
        StringBuilder s = new StringBuilder( 128 )
                .append( MAGIC_NUMBER )
                .append( operation );

        if ( data != null )
        {
            s.append( ':' )
                    .append( data );
        }

        return s.append( ':' );
    }
}
