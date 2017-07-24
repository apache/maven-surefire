package org.apache.maven.surefire.junitcore;

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

import java.io.PrintStream;

/**
 * @author <a href="mailto:kristian@zenior.no">Kristian Rosenvold</a>
 */
public class DefaultConsoleReporter
    implements ConsoleLogger
{
    private final PrintStream systemOut;

    public DefaultConsoleReporter( PrintStream systemOut )
    {
        this.systemOut = systemOut;
    }

    @Override
    public void debug( String message )
    {

    }

    @Override
    public void info( String message )
    {
        systemOut.println( message );
    }

    @Override
    public void warning( String message )
    {

    }

    @Override
    public void error( String message )
    {

    }

    @Override
    public void error( String message, Throwable t )
    {

    }

    @Override
    public void error( Throwable t )
    {

    }
}
