package org.apache.maven.plugin.surefire.log;

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
import org.codehaus.plexus.logging.Logger;

/**
 * Wrapper logger of miscellaneous implementations of {@link Logger}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20
 * @see ConsoleLogger
 */
public final class PluginConsoleLogger
    implements ConsoleLogger
{
    private final Logger plexusLogger;

    public PluginConsoleLogger( Logger plexusLogger )
    {
        this.plexusLogger = plexusLogger;
    }

    @Override
    public boolean isDebugEnabled()
    {
        return plexusLogger.isDebugEnabled();
    }

    @Override
    public void debug( String message )
    {
        plexusLogger.debug( message );
    }

    public void debug( CharSequence content, Throwable error )
    {
        plexusLogger.debug( content == null ? "" : content.toString(), error );
    }

    @Override
    public boolean isInfoEnabled()
    {
        return plexusLogger.isInfoEnabled();
    }

    @Override
    public void info( String message )
    {
        plexusLogger.info( message );
    }

    @Override
    public boolean isWarnEnabled()
    {
        return plexusLogger.isWarnEnabled();
    }

    @Override
    public void warning( String message )
    {
        plexusLogger.warn( message );
    }

    public void warning( CharSequence content, Throwable error )
    {
        plexusLogger.warn( content == null ? "" : content.toString(), error );
    }

    @Override
    public boolean isErrorEnabled()
    {
        return plexusLogger.isErrorEnabled() || plexusLogger.isFatalErrorEnabled();
    }

    @Override
    public void error( String message )
    {
        plexusLogger.error( message );
    }

    @Override
    public void error( String message, Throwable t )
    {
        plexusLogger.error( message, t );
    }

    @Override
    public void error( Throwable t )
    {
        plexusLogger.error( "", t );
    }
}
