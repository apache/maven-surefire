package org.apache.maven.plugin.surefire.log.api;

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

/**
 * Decorator around {@link ConsoleLogger}.
 * This class is loaded in the isolated ClassLoader and the child logger in the in-plugin ClassLoader.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20
 */
public final class ConsoleLoggerDecorator
        implements ConsoleLogger
{
    private final Object logger;

    public ConsoleLoggerDecorator( Object logger )
    {
        if ( logger == null )
        {
            throw new NullPointerException( "logger argument is null in " + ConsoleLoggerDecorator.class );
        }
        this.logger = logger;
    }

    @Override
    public boolean isDebugEnabled()
    {
        return invokeReturnedBoolean( "isDebugEnabled" );
    }

    @Override
    public void debug( String message )
    {
        invokeWithMessage( message, "debug" );
    }

    @Override
    public boolean isInfoEnabled()
    {
        return invokeReturnedBoolean( "isInfoEnabled" );
    }

    @Override
    public void info( String message )
    {
        invokeWithMessage( message, "info" );
    }

    @Override
    public boolean isWarnEnabled()
    {
        return invokeReturnedBoolean( "isWarnEnabled" );
    }

    @Override
    public void warning( String message )
    {
        invokeWithMessage( message, "warning" );
    }

    @Override
    public boolean isErrorEnabled()
    {
        return invokeReturnedBoolean( "isErrorEnabled" );
    }

    @Override
    public void error( String message )
    {
        invokeWithMessage( message, "error" );
    }

    @Override
    public void error( String message, Throwable t )
    {
        try
        {
            logger.getClass()
                    .getMethod( "error", String.class, Throwable.class )
                    .invoke( logger, message, t );
        }
        catch ( Exception e )
        {
            throw new IllegalStateException( e.getLocalizedMessage(), e );
        }
    }

    @Override
    public void error( Throwable t )
    {
        try
        {
            logger.getClass()
                    .getMethod( "error", Throwable.class )
                    .invoke( logger, t );
        }
        catch ( Exception e )
        {
            throw new IllegalStateException( e.getLocalizedMessage(), e );
        }
    }

    private boolean invokeReturnedBoolean( String isErrorEnabled )
    {
        try
        {
            return (Boolean) logger.getClass()
                    .getMethod( isErrorEnabled )
                    .invoke( logger );
        }
        catch ( Exception e )
        {
            throw new IllegalStateException( e.getLocalizedMessage(), e );
        }
    }

    private void invokeWithMessage( String message, String error )
    {
        try
        {
            logger.getClass()
                    .getMethod( error, String.class )
                    .invoke( logger, message );
        }
        catch ( Exception e )
        {
            throw new IllegalStateException( e.getLocalizedMessage(), e );
        }
    }
}
