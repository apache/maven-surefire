package org.apache.maven.plugin.surefire.report;

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

import org.apache.maven.plugin.surefire.booterclient.output.InPluginProcessDumpSingleton;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.TestSetReportEntry;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.maven.plugin.surefire.report.FileReporter.getReportFile;
import static org.apache.maven.surefire.util.internal.StringUtils.NL;

/**
 * Surefire output consumer proxy that writes test output to a {@link java.io.File} for each test suite.
 *
 * @author Kristian Rosenvold
 * @author Carlos Sanchez
 */
public class ConsoleOutputFileReporter
    implements TestcycleConsoleOutputReceiver
{
    private static final int STREAM_BUFFER_SIZE = 64 * 1024;
    private static final int OPEN = 0;
    private static final int CLOSED_TO_REOPEN = 1;
    private static final int CLOSED = 2;

    private final File reportsDirectory;
    private final String reportNameSuffix;
    private final boolean usePhrasedFileName;
    private final Integer forkNumber;
    private final String encoding;

    private final AtomicStampedReference<FilterOutputStream> fileOutputStream =
            new AtomicStampedReference<>( null, OPEN );

    private final ReentrantLock lock = new ReentrantLock();

    private volatile String reportEntryName;

    public ConsoleOutputFileReporter( File reportsDirectory, String reportNameSuffix, boolean usePhrasedFileName,
                                      Integer forkNumber, String encoding )
    {
        this.reportsDirectory = reportsDirectory;
        this.reportNameSuffix = reportNameSuffix;
        this.usePhrasedFileName = usePhrasedFileName;
        this.forkNumber = forkNumber;
        this.encoding = encoding;
    }

    @Override
    public void testSetStarting( TestSetReportEntry reportEntry )
    {
        lock.lock();
        try
        {
            closeNullReportFile( reportEntry );
        }
        finally
        {
            lock.unlock();
        }
    }

    @Override
    public void testSetCompleted( TestSetReportEntry report )
    {
    }

    @Override
    public void close()
    {
        // The close() method is called in main Thread T2.
        lock.lock();
        try
        {
            closeReportFile();
        }
        finally
        {
            lock.unlock();
        }
    }

    @Override
    public void writeTestOutput( String output, boolean newLine, boolean stdout )
    {
        lock.lock();
        try
        {
            // This method is called in single thread T1 per fork JVM (see ThreadedStreamConsumer).
            // The close() method is called in main Thread T2.
            int[] status = new int[1];
            FilterOutputStream os = fileOutputStream.get( status );
            if ( status[0] != CLOSED )
            {
                if ( os == null )
                {
                    if ( !reportsDirectory.exists() )
                    {
                        //noinspection ResultOfMethodCallIgnored
                        reportsDirectory.mkdirs();
                    }
                    File file = getReportFile( reportsDirectory, reportEntryName, reportNameSuffix, "-output.txt" );
                    os = new BufferedOutputStream( new FileOutputStream( file ), STREAM_BUFFER_SIZE );
                    fileOutputStream.set( os, OPEN );
                }

                if ( output == null )
                {
                    output = "null";
                }
                Charset charset = Charset.forName( encoding );
                os.write( output.getBytes( charset ) );
                if ( newLine )
                {
                    os.write( NL.getBytes( charset ) );
                }
            }
        }
        catch ( IOException e )
        {
            dumpException( e );
            // todo use UncheckedIOException in Java 8
            throw new RuntimeException( e );
        }
        finally
        {
            lock.unlock();
        }
    }

    @SuppressWarnings( "checkstyle:emptyblock" )
    private void closeNullReportFile( ReportEntry reportEntry )
    {
        try
        {
            // close null-output.txt report file
            close( true );
        }
        catch ( IOException e )
        {
            dumpException( e );
        }
        finally
        {
            // prepare <class>-output.txt report file
            reportEntryName = usePhrasedFileName ? reportEntry.getSourceText() : reportEntry.getSourceName();
        }
    }

    @SuppressWarnings( "checkstyle:emptyblock" )
    private void closeReportFile()
    {
        try
        {
            close( false );
        }
        catch ( IOException e )
        {
            dumpException( e );
        }
    }

    private void close( boolean closeReattempt )
            throws IOException
    {
        int[] status = new int[1];
        FilterOutputStream os = fileOutputStream.get( status );
        if ( status[0] != CLOSED )
        {
            fileOutputStream.set( null, closeReattempt ? CLOSED_TO_REOPEN : CLOSED );
            if ( os != null && status[0] == OPEN )
            {
                os.close();
            }
        }
    }

    private void dumpException( IOException e )
    {
        if ( forkNumber == null )
        {
            InPluginProcessDumpSingleton.getSingleton()
                    .dumpException( e, e.getLocalizedMessage(), reportsDirectory );
        }
        else
        {
            InPluginProcessDumpSingleton.getSingleton()
                    .dumpException( e, e.getLocalizedMessage(), reportsDirectory, forkNumber );
        }
    }
}
