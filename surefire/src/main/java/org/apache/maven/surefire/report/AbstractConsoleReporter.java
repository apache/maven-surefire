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

import java.io.BufferedOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Base class for console reporters.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractConsoleReporter
    extends AbstractReporter
{
    protected static final int BUFFER_SIZE = 4096;

    protected PrintWriter writer;

    protected long batteryStartTime;

    protected AbstractConsoleReporter()
    {
        writer = new PrintWriter( new OutputStreamWriter( new BufferedOutputStream( System.out, BUFFER_SIZE ) ) );
    }

    public void batteryStarting( ReportEntry report )
    {
        batteryStartTime = System.currentTimeMillis();

        writer.println( "[surefire] Running " + report.getName() );
        writer.flush();
    }

    public void writeMessage( String message )
    {
        writer.println( message );
        writer.flush();
    }

    public void runStarting( int testCount )
    {
        writer.println();
        writer.println( "-------------------------------------------------------" );
        writer.println( " T E S T S" );
        writer.println( "-------------------------------------------------------" );
        writer.flush();
    }

    protected StringBuffer getBatterySummary()
    {
        StringBuffer batterySummary = new StringBuffer();

        batterySummary.append( "[surefire] Tests run: " );
        batterySummary.append( completedCount );
        batterySummary.append( ", Failures: " );
        batterySummary.append( failures );
        batterySummary.append( ", Errors: " );
        batterySummary.append( errors );
        batterySummary.append( ", Time elapsed: " );
        batterySummary.append( elapsedTimeAsString( System.currentTimeMillis() - batteryStartTime ) );
        batterySummary.append( " sec" );
        return batterySummary;
    }
}
