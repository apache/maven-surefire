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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Base class for file reporters.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractFileReporter
    extends AbstractTextReporter
{
    private File reportsDirectory;

    protected AbstractFileReporter( File reportsDirectory, String format, Boolean trimStackTrace )
    {
        super( format, trimStackTrace );

        this.reportsDirectory = reportsDirectory;
    }

    public void testSetStarting( ReportEntry report )
        throws ReporterException
    {
        super.testSetStarting( report );

        File reportFile = new File( reportsDirectory, report.getName() + ".txt" );

        File reportDir = reportFile.getParentFile();

        reportDir.mkdirs();

        try
        {
            PrintWriter writer = new PrintWriter( new FileWriter( reportFile ) );

            writer.println( "-------------------------------------------------------------------------------" );

            writer.println( "Test set: " + report.getName() );

            writer.println( "-------------------------------------------------------------------------------" );

            setWriter( writer );
        }
        catch ( IOException e )
        {
            throw new ReporterException( "Unable to create file for report: " + e.getMessage(), e );
        }
    }

    public void testSetCompleted( ReportEntry report )
        throws ReporterException
    {
        super.testSetCompleted( report );

        writer.flush();

        writer.close();

        writer = null;
    }
}
