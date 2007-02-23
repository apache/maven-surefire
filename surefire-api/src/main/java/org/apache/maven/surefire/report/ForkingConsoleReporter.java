package org.apache.maven.surefire.report;

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
 * Surefire reporter that will prefix surefire output to make it easier to parse when forking tests
 *
 * @version $Id$
 */
public class ForkingConsoleReporter
    extends ConsoleReporter
{
    /**
     * Surefire output lines not part of header or footer, nor test output will start with this value.
     * eg. "Running org.foo.BarTest" or "Tests run: ..."
     */
    public static final String FORKING_PREFIX_STANDARD = "@SL";

    /**
     * Surefire output lines part of the header will start with this value
     */
    public static final String FORKING_PREFIX_HEADING = "@HL";

    /**
     * Surefire output lines part of the footer will start with this value
     */
    public static final String FORKING_PREFIX_FOOTER = "@FL";

    public ForkingConsoleReporter( Boolean trimStackTrace )
    {
        super( trimStackTrace );
    }

    /**
     * Write a header line prepending {@link #FORKING_PREFIX_HEADING}
     */
    public void writeHeading( String message )
    {
        writer.print( FORKING_PREFIX_HEADING );

        super.writeHeading( message );
    }

    /**
     * Write a footer line prepending {@link #FORKING_PREFIX_FOOTER}
     */
    public void writeFooter( String footer )
    {
        writer.print( FORKING_PREFIX_FOOTER );

        // Deliberately set to writeMessage
        super.writeMessage( footer );
    }

    /**
     * Write a surefire message line prepending {@link #FORKING_PREFIX_STANDARD}
     */
    public void writeMessage( String message )
    {
        writer.print( FORKING_PREFIX_STANDARD );

        super.writeMessage( message );
    }
}
