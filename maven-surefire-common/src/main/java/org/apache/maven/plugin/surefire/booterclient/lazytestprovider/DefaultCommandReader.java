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

import org.apache.maven.surefire.api.booter.Command;

import java.io.IOException;

/**
 * Reader stream sends commands to forked jvm std-{@link java.io.InputStream input-stream}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 * @see Command
 */
public abstract class DefaultCommandReader
        extends AbstractCommandReader
{
    /**
     * Opposite to {@link #isClosed()}.
     * @return {@code true} if not closed
     */
    protected boolean canContinue()
    {
        return !isClosed();
    }

    /**
     * Possibly waiting for next command (see {@link #nextCommand()}) unless the stream is atomically
     * closed (see {@link #isClosed()} returns {@code true}) before this method has returned.
     *
     * @throws IOException stream error while waiting for notification regarding next test required by forked jvm
     */
    protected void beforeNextCommand()
        throws IOException
    {
    }

    protected abstract Command nextCommand();

    /**
     * Used by single thread in StreamFeeder class.
     *
     * @return {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    @Override
    public Command readNextCommand()
        throws IOException
    {
        if ( isClosed() )
        {
            return null;
        }

        if ( !canContinue() )
        {
            close();
            return null;
        }

        beforeNextCommand();

        if ( isClosed() )
        {
            return null;
        }

        return nextCommand();
    }
}
