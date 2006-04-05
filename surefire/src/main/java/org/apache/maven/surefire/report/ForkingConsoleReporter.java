package org.apache.maven.surefire.report;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * 
 * @version $Id$ 
 */
public class ForkingConsoleReporter
    extends ConsoleReporter
{
    public void println( String message )
    {
        writer.write( ForkingReport.FORKING_PREFIX_STANDARD );

        writer.println( message );

        writer.flush();
    }

    public void print( String message )
    {
        writer.write( ForkingReport.FORKING_PREFIX_STANDARD );

        writer.print( message );

        writer.flush();
    }

    public void runStarting( int testCount )
    {
        writer.print( ForkingReport.FORKING_PREFIX_HEADING );

        writer.println( "" );

        writer.print( ForkingReport.FORKING_PREFIX_HEADING );

        writer.println( "-------------------------------------------------------" );

        writer.print( ForkingReport.FORKING_PREFIX_HEADING );

        writer.println( " T E S T S" );

        writer.print( ForkingReport.FORKING_PREFIX_HEADING );

        writer.println( "-------------------------------------------------------" );

        writer.flush();
    }
}
