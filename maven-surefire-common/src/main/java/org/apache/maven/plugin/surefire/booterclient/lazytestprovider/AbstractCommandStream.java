package org.apache.maven.plugin.surefire.booterclient.lazytestprovider;

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

import org.apache.maven.surefire.booter.Command;
import org.apache.maven.surefire.booter.MasterProcessCommand;

import java.io.IOException;

/**
 * Reader stream sends commands to forked jvm std-{@link java.io.InputStream input-stream}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @see org.apache.maven.surefire.booter.Command
 * @since 2.19
 */
public abstract class AbstractCommandStream
        extends AbstractForkInputStream
{
    private byte[] currentBuffer;
    private int currentPos;
    private volatile MasterProcessCommand lastCommand;

    protected abstract boolean isClosed();

    /**
     * Unnecessarily opposite to {@link #isClosed()} however may respect
     * {@link #getLastCommand() last command} and {@link #isClosed()}.
     */
    protected abstract boolean canContinue();

    /**
     * Possibly waiting for next command (see {@link #nextCommand()}) unless the stream is atomically
     * closed (see {@link #isClosed()} returns {@code true}) before this method has returned.
     */
    protected void beforeNextCommand()
            throws IOException
    {
    }

    protected abstract Command nextCommand();

    /**
     * Returns quietly and immediately.
     */
    protected final void invalidateInternalBuffer()
    {
        currentBuffer = null;
        currentPos = 0;
    }

    protected final MasterProcessCommand getLastCommand()
    {
        return lastCommand;
    }

    /**
     * Used by single thread in StreamFeeder class.
     *
     * @return {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    @SuppressWarnings( "checkstyle:magicnumber" )
    @Override
    public int read()
            throws IOException
    {
        if ( isClosed() )
        {
            return -1;
        }

        byte[] buffer = currentBuffer;
        if ( buffer == null )
        {
            tryFlush();

            if ( !canContinue() )
            {
                close();
                return -1;
            }

            beforeNextCommand();

            if ( isClosed() )
            {
                return -1;
            }

            Command cmd = nextCommand();
            System.out.println( getClass().getSimpleName() + " will send " + cmd.getCommandType() );
            lastCommand = cmd.getCommandType();
            buffer = lastCommand.hasDataType() ? lastCommand.encode( cmd.getData() ) : lastCommand.encode();
        }

        int b = buffer[currentPos++] & 0xff;
        if ( currentPos == buffer.length )
        {
            Integer cmd = null;
            Integer len = null;
            int offset = 0;
            if ( currentBuffer.length >= 4 )
            {
                offset = 4;
                cmd = toInt( currentBuffer, 0 );
            }
            if ( currentBuffer.length >= 8 )
            {
                offset = 8;
                len = toInt( currentBuffer, 4 );
            }
            System.out.println( getClass().getSimpleName()
                                        + " last byte sent of cmd "
                                        + lastCommand
                                        + cmd
                                        + " "
                                        + len
                                        + " "
                                        + new String( buffer, offset, currentBuffer.length - offset ) );
            buffer = null;
            currentPos = 0;
        }
        currentBuffer = buffer;
        return b;
    }

    @SuppressWarnings( "checkstyle:magicnumber" )
    private static int toInt( byte[] array, int offset )
    {
        return ( array[ offset ] << 24 ) & 0xff000000
                        | ( array[ offset + 1 ] << 16 ) & 0x00ff0000
                        | ( array[ offset + 2 ] << 8 ) & 0x0000ff00
                        | array[ offset + 3 ] & 0x000000ff;
    }
}
