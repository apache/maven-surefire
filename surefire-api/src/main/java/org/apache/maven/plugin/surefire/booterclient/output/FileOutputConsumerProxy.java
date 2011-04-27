package org.apache.maven.plugin.surefire.booterclient.output;

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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.Reporter;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.util.NestedRuntimeException;

/**
 * Surefire output consumer proxy that writes test output to a {@link File} for each test suite.
 * <p/>
 * This class is not threadsafe, but can be encapsulated with a SynchronizedOutputConsumer. It may still be
 * accessed from different threads (serially).
 *
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 * @since 2.1
 */
public class FileOutputConsumerProxy
    implements Reporter
{

    private static final String LINE_SEPARATOR = System.getProperty( "line.separator" );

    private final File reportsDirectory;

    private final StringBuffer outputBuffer = new StringBuffer();

    private volatile PrintWriter printWriter;

    /**
     * Create a consumer that will write to a {@link File} for each test
     */

    public FileOutputConsumerProxy( ReporterConfiguration reporterConfiguration )
    {
        this.reportsDirectory = reporterConfiguration.getReportsDirectory();
    }

    public void testSetStarting( ReportEntry reportEntry )
    {
        if ( printWriter != null )
        {
            throw new IllegalStateException( "testSetStarting called twice" );
        }

        if ( !reportsDirectory.exists() )
        {
            //noinspection ResultOfMethodCallIgnored
            reportsDirectory.mkdirs();
        }

        File file = new File( reportsDirectory, reportEntry.getName() + "-output.txt" );
        try
        {
            this.printWriter = new PrintWriter( new BufferedWriter( new FileWriter( file ) ) );
        }
        catch ( IOException e )
        {
            throw new NestedRuntimeException( e );
        }
    }

    public void testSetCompleted( ReportEntry report )
        throws ReporterException
    {
        if ( printWriter == null )
        {
            throw new IllegalStateException( "testSetCompleted called before testSetStarting" );
        }
        if ( outputBuffer.length() > 0 )
        {
            printWriter.write( outputBuffer.toString() );
            printWriter.write( LINE_SEPARATOR );
            outputBuffer.setLength( 0 );
        }
        printWriter.close();
        this.printWriter = null;
    }

    public void testStarting( ReportEntry report )
    {
    }

    public void testSucceeded( ReportEntry report )
    {
    }

    public void testError( ReportEntry report )
    {
    }

    public void testFailed( ReportEntry report )
    {
    }

    public void testSkipped( ReportEntry report )
    {
    }

    public void testError( ReportEntry report, String stdOut, String stdErr )
    {
    }

    public void testFailed( ReportEntry report, String stdOut, String stdErr )
    {
    }

    public void writeMessage( String message )
    {
    }

    public void writeMessage( byte[] b, int off, int len )
    {
    }

    public void writeDetailMessage( String message )
    {
    }

    public void reset()
    {
    }


    /**
     * Write the output to the current test file
     * <p/>
     */
    public void consumeOutputLine( String line )
    {
        if ( printWriter == null )
        {
            outputBuffer.append( line );
            outputBuffer.append( LINE_SEPARATOR );
            return;
        }

        if ( outputBuffer.length() > 0 )
        {
            printWriter.write( outputBuffer.toString() );
            printWriter.write( LINE_SEPARATOR );
            outputBuffer.setLength( 0 );
        }
        printWriter.write( line );
        printWriter.write( LINE_SEPARATOR );
    }

}
