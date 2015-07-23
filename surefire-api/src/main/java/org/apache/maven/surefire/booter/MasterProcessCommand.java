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

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static org.apache.maven.surefire.util.internal.StringUtils.FORK_STREAM_CHARSET_NAME;
import static org.apache.maven.surefire.util.internal.StringUtils.encodeStringForForkCommunication;
import static org.apache.maven.surefire.util.internal.StringUtils.requireNonNull;
import static java.lang.String.format;

/**
 * Commands which are sent from plugin to the forked jvm.
 * Support and methods related to the commands.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public enum MasterProcessCommand
{
    RUN_CLASS( 0, String.class ),
    TEST_SET_FINISHED( 1, Void.class ),
    STOP_ON_NEXT_TEST( 2, Void.class ),
    SHUTDOWN( 3, Void.class ),
    /** To tell a forked process that the master process is still alive. Repeated after 30 seconds. */
    NOOP( 4, Void.class );

    private final int id;

    private final Class<?> dataType;

    MasterProcessCommand( int id, Class<?> dataType )
    {
        this.id = id;
        this.dataType = requireNonNull( dataType, "dataType cannot be null" );
    }

    public int getId()
    {
        return id;
    }

    public Class<?> getDataType()
    {
        return dataType;
    }

    @SuppressWarnings( "checkstyle:magicnumber" )
    public byte[] encode( String data )
    {
        if ( getDataType() != String.class )
        {
            throw new IllegalArgumentException( "Data type can be only " + getDataType() );
        }
        byte[] dataBytes = fromDataType( data );
        byte[] encoded = new byte[8 + dataBytes.length];
        int command = getId();
        int len = dataBytes.length;
        setCommandAndDataLength( command, len, encoded );
        System.arraycopy( dataBytes, 0, encoded, 8, dataBytes.length );
        return encoded;
    }

    @SuppressWarnings( "checkstyle:magicnumber" )
    public byte[] encode()
    {
        if ( getDataType() != Void.class )
        {
            throw new IllegalArgumentException( "Data type can be only " + getDataType() );
        }
        byte[] encoded = new byte[8];
        int command = getId();
        setCommandAndDataLength( command, 0, encoded );
        return encoded;
    }

    public static Command decode( DataInputStream is )
        throws IOException
    {
        MasterProcessCommand command = resolve( is.readInt() );
        if ( command == null )
        {
            return null;
        }
        else
        {
            int dataLength = is.readInt();
            if ( dataLength > 0 )
            {
                byte[] buffer = new byte[dataLength];
                int read = 0;
                int total = 0;
                do
                {
                    total += read;
                    read = is.read( buffer, total, dataLength - total );
                } while ( read > 0 );

                if ( command.getDataType() == Void.class )
                {
                    // must read entire sequence to get to the next command; cannot be above the loop
                    throw new IOException( "Command " + command + " read Void data with length " + dataLength );
                }

                if ( total != dataLength )
                {
                    if ( read == -1 )
                    {
                        throw new EOFException( "stream closed" );
                    }

                    throw new EOFException( format( "%s read %d out of %d bytes",
                                                    MasterProcessCommand.class, total, dataLength ) );
                }

                String data = command.toDataTypeAsString( buffer );
                return new Command( command, data );
            }
            else
            {
                return new Command( command );
            }
        }
    }

    String toDataTypeAsString( byte... data )
    {
        try
        {
            switch ( this )
            {
                case RUN_CLASS:
                    return new String( data, FORK_STREAM_CHARSET_NAME );
                default:
                    return null;
            }
        }
        catch ( UnsupportedEncodingException e )
        {
            throw new IllegalStateException( e );
        }
    }

    byte[] fromDataType( String data )
    {
        switch ( this )
        {
            case RUN_CLASS:
                return encodeStringForForkCommunication( data );
            default:
                return new byte[0];
        }
    }

    static MasterProcessCommand resolve( int id )
    {
        for ( MasterProcessCommand command : values() )
        {
            if ( id == command.id )
            {
                return command;
            }
        }
        return null;
    }

    @SuppressWarnings( "checkstyle:magicnumber" )
    static void setCommandAndDataLength( int command, int dataLength, byte... encoded )
    {
        encoded[0] = (byte) ( command >>> 24 );
        encoded[1] = (byte) ( command >>> 16 );
        encoded[2] = (byte) ( command >>> 8 );
        encoded[3] = (byte) command;
        encoded[4] = (byte) ( dataLength >>> 24 );
        encoded[5] = (byte) ( dataLength >>> 16 );
        encoded[6] = (byte) ( dataLength >>> 8 );
        encoded[7] = (byte) dataLength;
    }
}
