package org.apache.maven.plugin.surefire;

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
import java.io.PrintStream;
import java.util.Properties;
import org.apache.maven.plugin.surefire.report.ConsoleOutputFileReporter;
import org.apache.maven.plugin.surefire.report.ConsoleReporter;
import org.apache.maven.plugin.surefire.report.DirectConsoleOutput;
import org.apache.maven.plugin.surefire.report.FileReporter;
import org.apache.maven.plugin.surefire.report.StatelessXmlReporter;
import org.apache.maven.plugin.surefire.report.TestcycleConsoleOutputReceiver;
import org.apache.maven.plugin.surefire.runorder.StatisticsReporter;

import javax.annotation.Nonnull;

/**
 * All the parameters used to construct reporters
 * <p/>
 *
 * @author Kristian Rosenvold
 */
public class StartupReportConfiguration
{
    private final PrintStream originalSystemOut;

    private final PrintStream originalSystemErr;

    private final boolean useFile;

    private final boolean printSummary;

    private final String reportFormat;

    private final String reportNameSuffix;

    private final String configurationHash;

    private final boolean requiresRunHistory;

    private final boolean redirectTestOutputToFile;

    private final boolean disableXmlReport;

    private final File reportsDirectory;

    private final boolean trimStackTrace;

    private final Properties testVmSystemProperties = new Properties();

    public static final String BRIEF_REPORT_FORMAT = ConsoleReporter.BRIEF;

    public static final String PLAIN_REPORT_FORMAT = ConsoleReporter.PLAIN;

    public StartupReportConfiguration( boolean useFile, boolean printSummary, String reportFormat,
                                       boolean redirectTestOutputToFile, boolean disableXmlReport,
                                       @Nonnull File reportsDirectory,
                                       boolean trimStackTrace, String reportNameSuffix,
                                       String configurationHash, boolean requiresRunHistory )
    {
        this.useFile = useFile;
        this.printSummary = printSummary;
        this.reportFormat = reportFormat;
        this.redirectTestOutputToFile = redirectTestOutputToFile;
        this.disableXmlReport = disableXmlReport;
        this.reportsDirectory = reportsDirectory;
        this.trimStackTrace = trimStackTrace;
        this.reportNameSuffix = reportNameSuffix;
        this.configurationHash = configurationHash;
        this.requiresRunHistory = requiresRunHistory;
        this.originalSystemOut = System.out;
        this.originalSystemErr = System.err;
    }

    public static StartupReportConfiguration defaultValue()
    {
        File target = new File( "./target" );
        return new StartupReportConfiguration( true, true, "PLAIN", false, false, target, false, null, "TESTHASH",
                                               false );
    }

    public static StartupReportConfiguration defaultNoXml()
    {
        File target = new File( "./target" );
        return new StartupReportConfiguration( true, true, "PLAIN", false, true, target, false, null, "TESTHASHxXML",
                                               false );
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

    public String getReportNameSuffix()
    {
        return reportNameSuffix;
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

    public StatelessXmlReporter instantiateStatelessXmlReporter()
    {
        if ( !isDisableXmlReport() )
        {
            return new StatelessXmlReporter( reportsDirectory, reportNameSuffix, trimStackTrace );
        }
        return null;
    }

    public FileReporter instantiateFileReporter()
    {
        if ( isUseFile() && isBriefOrPlainFormat() )
        {
            return new FileReporter( reportsDirectory, getReportNameSuffix() );
        }
        return null;
    }

    public boolean isBriefOrPlainFormat()
    {
        String fmt = getReportFormat();
        return BRIEF_REPORT_FORMAT.equals( fmt ) || PLAIN_REPORT_FORMAT.equals( fmt );
    }

    public ConsoleReporter instantiateConsoleReporter()
    {
        return shouldReportToConsole() ? new ConsoleReporter( originalSystemOut ) : null;
    }

    private boolean shouldReportToConsole()
    {
        return isUseFile() ? isPrintSummary() : isRedirectTestOutputToFile() || isBriefOrPlainFormat();
    }

    public TestcycleConsoleOutputReceiver instantiateConsoleOutputFileReporter()
    {
        if ( isRedirectTestOutputToFile() )
        {
            return new ConsoleOutputFileReporter( reportsDirectory, getReportNameSuffix() );
        }
        else
        {
            return new DirectConsoleOutput( originalSystemOut, originalSystemErr );
        }
    }

    public StatisticsReporter instantiateStatisticsReporter()
    {
        if ( requiresRunHistory )
        {
            final File target = getStatisticsFile();
            return new StatisticsReporter( target );
        }
        return null;
    }

    public File getStatisticsFile()
    {
        return new File( reportsDirectory.getParentFile().getParentFile(), ".surefire-" + this.configurationHash );
    }


    public Properties getTestVmSystemProperties()
    {
        return testVmSystemProperties;
    }


    public boolean isTrimStackTrace()
    {
        return trimStackTrace;
    }

    public String getConfigurationHash()
    {
        return configurationHash;
    }

    public boolean isRequiresRunHistory()
    {
        return requiresRunHistory;
    }

    public PrintStream getOriginalSystemOut()
    {
        return originalSystemOut;
    }

}
