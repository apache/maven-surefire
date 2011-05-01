package org.apache.maven.surefire.booter;

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
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.surefire.report.BriefConsoleReporter;
import org.apache.maven.surefire.report.BriefFileReporter;
import org.apache.maven.surefire.report.ConsoleOutputDirectReporter;
import org.apache.maven.surefire.report.ConsoleOutputFileReporter;
import org.apache.maven.surefire.report.ConsoleReporter;
import org.apache.maven.surefire.report.DetailedConsoleReporter;
import org.apache.maven.surefire.report.FileReporter;
import org.apache.maven.surefire.report.XMLReporter;

/**
 * All the parameters used to construct reporters
 * <p/>
 * TODO: Move out of API module
 *
 * @author Kristian Rosenvold
 */
public class StartupReportConfiguration
{
    private final boolean useFile;

    private final boolean printSummary;

    private final String reportFormat;

    private final boolean redirectTestOutputToFile;

    private final boolean disableXmlReport;

    private final File reportsDirectory;


    public static final String BRIEF_REPORT_FORMAT = "brief";

    public static final String PLAIN_REPORT_FORMAT = "plain";

    public StartupReportConfiguration( boolean useFile, boolean printSummary, String reportFormat,
                                       boolean redirectTestOutputToFile, boolean disableXmlReport,
                                       File reportsDirectory )
    {
        this.useFile = useFile;
        this.printSummary = printSummary;
        this.reportFormat = reportFormat;
        this.redirectTestOutputToFile = redirectTestOutputToFile;
        this.disableXmlReport = disableXmlReport;
        this.reportsDirectory = reportsDirectory;
    }

    public static StartupReportConfiguration defaultValue()
    {
        File target = new File( "./target" );
        return new StartupReportConfiguration( true, true, "PLAIN", false, false, target );
    }

    public static StartupReportConfiguration defaultNoXml()
    {
        File target = new File( "./target" );
        return new StartupReportConfiguration( true, true, "PLAIN", false, true, target );
    }

    public boolean isUseFile()
    {
        return useFile;
    }

    public boolean isPrintSummary()
    {
        return printSummary;
    }

    public String getReportFormat()
    {
        return reportFormat;
    }

    public boolean isRedirectTestOutputToFile()
    {
        return redirectTestOutputToFile;
    }

    public boolean isDisableXmlReport()
    {
        return disableXmlReport;
    }

    public File getReportsDirectory()
    {
        return reportsDirectory;
    }

    public String getXmlReporterName()
    {
        if ( !isDisableXmlReport() )
        {
            return XMLReporter.class.getName();
        }
        return null;
    }

    public String getFileReporter()
    {
        if ( isUseFile() )
        {
            if ( BRIEF_REPORT_FORMAT.equals( getReportFormat() ) )
            {
                return BriefFileReporter.class.getName();
            }
            else if ( PLAIN_REPORT_FORMAT.equals( getReportFormat() ) )
            {
                return FileReporter.class.getName();
            }
        }
        return null;
    }

    /**
     * Returns the reporter that will write to the console
     *
     * @return a console reporter of null if no console reporting
     */
    public String getConsoleReporter()
    {
        if ( isUseFile() )
        {
            return isPrintSummary() ? ConsoleReporter.class.getName() : null;
        }
        else if ( isRedirectTestOutputToFile() || BRIEF_REPORT_FORMAT.equals( getReportFormat() ) )
        {
            return BriefConsoleReporter.class.getName();
        }
        else if ( PLAIN_REPORT_FORMAT.equals( getReportFormat() ) )
        {
            return DetailedConsoleReporter.class.getName();
        }
/*        if (isRedirectTestOutputToFile())
        {
            return null;
        }*/
        return null;
    }

    public String getConsoleOutputFileReporterName()
    {
        if ( isRedirectTestOutputToFile() )
        {
            return ConsoleOutputFileReporter.class.getName();
        }
        else
        {
            return ConsoleOutputDirectReporter.class.getName();
        }
    }


    /**
     * A list of classnames representing runnable reports for this test-run.
     *
     * @return A list of strings, each string is a classname of a class
     *         implementing the org.apache.maven.surefire.report.Reporter interface
     */
    public List getReports()
    {
        ArrayList reports = new ArrayList();
        addIfNotNull( reports, getConsoleReporter() );
        addIfNotNull( reports, getFileReporter() );
        addIfNotNull( reports, getXmlReporterName() );
        addIfNotNull( reports, getConsoleOutputFileReporterName() );
        return reports;
    }

    private void addIfNotNull( ArrayList reports, String reporter )
    {
        if ( reporter != null )
        {
            reports.add( reporter );
        }
    }


}
