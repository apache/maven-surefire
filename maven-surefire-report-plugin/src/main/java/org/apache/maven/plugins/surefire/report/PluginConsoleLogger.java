package org.apache.maven.plugins.surefire.report;

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

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.shared.utils.logging.MessageBuilder;

import static java.lang.Integer.numberOfLeadingZeros;
import static org.apache.maven.shared.utils.logging.MessageUtils.buffer;

/**
 * Wrapper logger of miscellaneous implementations of {@link Log}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20
 * @see ConsoleLogger
 */
final class PluginConsoleLogger
    implements ConsoleLogger
{
    private final Log mojoLogger;

    PluginConsoleLogger( Log mojoLogger )
    {
        this.mojoLogger = mojoLogger;
    }

    @Override
    public boolean isDebugEnabled()
    {
        return mojoLogger.isDebugEnabled();
    }

    @Override
    public void debug( String message )
    {
        mojoLogger.debug( createAnsiBuilder( message ).a( message ).toString() );
    }

    public void debug( CharSequence content, Throwable error )
    {
        mojoLogger.debug( content, error );
    }

    @Override
    public boolean isInfoEnabled()
    {
        return mojoLogger.isInfoEnabled();
    }

    @Override
    public void info( String message )
    {
        mojoLogger.info( createAnsiBuilder( message ).a( message ).toString() );
    }

    @Override
    public boolean isWarnEnabled()
    {
        return mojoLogger.isWarnEnabled();
    }

    @Override
    public void warning( String message )
    {
        mojoLogger.warn( createAnsiBuilder( message ).warning( message ).toString() );
    }

    public void warn( CharSequence content, Throwable error )
    {
        mojoLogger.warn( content, error );
    }

    @Override
    public boolean isErrorEnabled()
    {
        return mojoLogger.isErrorEnabled();
    }

    @Override
    public void error( String message )
    {
        mojoLogger.error( createAnsiBuilder( message ).failure( message ).toString() );
    }

    @Override
    public void error( String message, Throwable t )
    {
        mojoLogger.error( message, t );
    }

    @Override
    public void error( Throwable t )
    {
        mojoLogger.error( t );
    }

    private static MessageBuilder createAnsiBuilder( CharSequence message )
    {
        return buffer( bufferSize( message ) );
    }

    private static int bufferSize( CharSequence message )
    {
        return 32 - numberOfLeadingZeros( message.length() );
    }
}
