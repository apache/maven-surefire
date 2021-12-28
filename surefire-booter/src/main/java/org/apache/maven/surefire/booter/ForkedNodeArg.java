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

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.log.api.NullConsoleLogger;
import org.apache.maven.surefire.api.booter.DumpErrorSingleton;
import org.apache.maven.surefire.api.fork.ForkNodeArguments;

import javax.annotation.Nonnull;
import java.io.File;

/**
 *
 */
public final class ForkedNodeArg implements ForkNodeArguments
{
    private final int forkChannelId;
    private final ConsoleLogger logger;
    private final boolean isDebug;

    public ForkedNodeArg( int forkChannelId, boolean isDebug )
    {
        this.forkChannelId = forkChannelId;
        logger = new NullConsoleLogger();
        this.isDebug = isDebug;
    }

    @Nonnull
    @Override
    public String getSessionId()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getForkChannelId()
    {
        return forkChannelId;
    }

    @Override
    @Nonnull
    public File dumpStreamText( @Nonnull String text )
    {
        return DumpErrorSingleton.getSingleton().dumpStreamText( text );
    }

    @Nonnull
    @Override
    public File dumpStreamException( @Nonnull Throwable t )
    {
        return DumpErrorSingleton.getSingleton().dumpStreamException( t, t.getLocalizedMessage() );
    }

    @Override
    public void logWarningAtEnd( @Nonnull String text )
    {
        // do nothing - the log message of forked VM already goes to the dump file
    }

    @Nonnull
    @Override
    public ConsoleLogger getConsoleLogger()
    {
        return logger;
    }

    @Nonnull
    @Override
    public Object getConsoleLock()
    {
        return logger;
    }

    @Override
    public File getEventStreamBinaryFile()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public File getCommandStreamBinaryFile()
    {
        return isDebug ? DumpErrorSingleton.getSingleton().getCommandStreamBinaryFile() : null;
    }
}
