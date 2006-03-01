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
    extends AbstractReporter
{
    protected StringBuffer reportContent;

    protected PrintWriter writer;

    public void batteryStarting( ReportEntry report )
        throws IOException
    {
        super.batteryStarting( report );

        reportContent = new StringBuffer();

        File reportFile = new File( getReportsDirectory(), report.getName() + ".txt" );

        File reportDir = reportFile.getParentFile();

        reportDir.mkdirs();

        writer = new PrintWriter( new FileWriter( reportFile ) );

        writer.println( "-------------------------------------------------------------------------------" );

        writer.println( "Battery: " + report.getName() );

        writer.println( "-------------------------------------------------------------------------------" );
    }

}
