package org.apache.maven.surefire.booter.output;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.util.NestedRuntimeException;

/**
 * Surefire output consumer proxy that writes test output to a {@link File} for each test suite.
 * 
 * @since 2.1
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public class FileOutputConsumerProxy
    extends OutputConsumerProxy
{

    private static final String USER_DIR = System.getProperty( "user.dir" );

    private static final String LINE_SEPARATOR = System.getProperty( "line.separator" );

    private File reportsDirectory;

    private PrintWriter printWriter;

    /**
     * Create a consumer that will write to a {@link File} for each test.
     * Files will be saved in working directory.
     */
    public FileOutputConsumerProxy( OutputConsumer outputConsumer )
    {
        this( outputConsumer, new File( USER_DIR ) );
    }

    /**
     * Create a consumer that will write to a {@link File} for each test
     * 
     * @param reportsDirectory directory where files will be saved 
     */
    public FileOutputConsumerProxy( OutputConsumer outputConsumer, File reportsDirectory )
    {
        super( outputConsumer );
        this.setReportsDirectory( reportsDirectory );
    }

    /**
     * Set the directory where reports will be saved
     * 
     * @param reportsDirectory the directory
     */
    public void setReportsDirectory( File reportsDirectory )
    {
        this.reportsDirectory = reportsDirectory;
    }

    /**
     * Get the directory where reports will be saved
     */
    public File getReportsDirectory()
    {
        return reportsDirectory;
    }

    /**
     * Set the {@link PrintWriter} used for the current test suite
     * 
     * @param writer
     */
    public void setPrintWriter( PrintWriter writer )
    {
        this.printWriter = writer;
    }

    /**
     * Get the {@link PrintWriter} used for the current test suite
     */
    public PrintWriter getPrintWriter()
    {
        return printWriter;
    }

    public void testSetStarting( ReportEntry reportEntry )
    {
        if ( getPrintWriter() != null )
        {
            throw new IllegalStateException( "testSetStarting called twice" );
        }
        File file = new File( getReportsDirectory(), reportEntry.getName() + "-output.txt" );
        try
        {
            setPrintWriter( new PrintWriter( new BufferedWriter( new FileWriter( file ) ) ) );
        }
        catch ( IOException e )
        {
            throw new NestedRuntimeException( e );
        }
        super.testSetStarting( reportEntry );
    }

    public void testSetCompleted()
    {
        if ( getPrintWriter() == null )
        {
            throw new IllegalStateException( "testSetCompleted called before testSetStarting" );
        }
        getPrintWriter().close();
        setPrintWriter( null );
        super.testSetCompleted();
    }

    /**
     * Write the output to the current test file 
     */
    public void consumeOutputLine( String line )
    {
        if ( getPrintWriter() == null )
        {
            throw new IllegalStateException( "consumeOutputLine called before testSetStarting" );
        }
        getPrintWriter().write( line );
        getPrintWriter().write( LINE_SEPARATOR );
    }

}
