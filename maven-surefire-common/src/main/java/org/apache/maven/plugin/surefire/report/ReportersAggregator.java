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

import org.apache.maven.plugin.surefire.runorder.StatisticsReporter;
import org.apache.maven.surefire.extensions.ConsoleOutputReportEventListener;
import org.apache.maven.surefire.extensions.StatelessReportEventListener;
import org.apache.maven.surefire.extensions.StatelessTestSetSummaryListener;
import org.apache.maven.surefire.extensions.StatelessTestsetInfoConsoleReportEventListener;
import org.apache.maven.surefire.extensions.StatelessTestsetInfoFileReportEventListener;

/**
 *
 */
public final class ReportersAggregator
{
    private StatelessTestsetInfoConsoleReportEventListener<WrappedReportEntry, TestSetStats> consoleReporter;
    private StatelessTestsetInfoFileReportEventListener<WrappedReportEntry, TestSetStats> fileReporter;
    private StatelessReportEventListener<WrappedReportEntry, TestSetStats> simpleXMLReporter;
    private ConsoleOutputReportEventListener testOutputReceiver;
    private StatisticsReporter statisticsReporter;
    private StatelessTestSetSummaryListener testSetSummaryReporter;
    private boolean trimStackTrace;
    private boolean isPlainFormat;
    private boolean briefOrPlainFormat;

    public StatelessTestsetInfoConsoleReportEventListener<WrappedReportEntry, TestSetStats> getConsoleReporter()
    {
        return consoleReporter;
    }

    public void setConsoleReporter(
        StatelessTestsetInfoConsoleReportEventListener<WrappedReportEntry, TestSetStats> consoleReporter )
    {
        this.consoleReporter = consoleReporter;
    }

    public StatelessTestsetInfoFileReportEventListener<WrappedReportEntry, TestSetStats> getFileReporter()
    {
        return fileReporter;
    }

    public void setFileReporter(
        StatelessTestsetInfoFileReportEventListener<WrappedReportEntry, TestSetStats> fileReporter )
    {
        this.fileReporter = fileReporter;
    }

    public StatelessReportEventListener<WrappedReportEntry, TestSetStats> getSimpleXMLReporter()
    {
        return simpleXMLReporter;
    }

    public void setSimpleXMLReporter( StatelessReportEventListener<WrappedReportEntry, TestSetStats> simpleXMLReporter )
    {
        this.simpleXMLReporter = simpleXMLReporter;
    }

    public ConsoleOutputReportEventListener getTestOutputReceiver()
    {
        return testOutputReceiver;
    }

    public void setTestOutputReceiver( ConsoleOutputReportEventListener testOutputReceiver )
    {
        this.testOutputReceiver = testOutputReceiver;
    }

    public StatisticsReporter getStatisticsReporter()
    {
        return statisticsReporter;
    }

    public void setStatisticsReporter( StatisticsReporter statisticsReporter )
    {
        this.statisticsReporter = statisticsReporter;
    }

    public StatelessTestSetSummaryListener getTestSetSummaryReporter()
    {
        return testSetSummaryReporter;
    }

    public void setTestSetSummaryReporter( StatelessTestSetSummaryListener testSetSummaryReporter )
    {
        this.testSetSummaryReporter = testSetSummaryReporter;
    }

    public boolean isTrimStackTrace()
    {
        return trimStackTrace;
    }

    public void setTrimStackTrace( boolean trimStackTrace )
    {
        this.trimStackTrace = trimStackTrace;
    }

    public boolean isPlainFormat()
    {
        return isPlainFormat;
    }

    public void setPlainFormat( boolean plainFormat )
    {
        isPlainFormat = plainFormat;
    }

    public boolean isBriefOrPlainFormat()
    {
        return briefOrPlainFormat;
    }

    public void setBriefOrPlainFormat( boolean briefOrPlainFormat )
    {
        this.briefOrPlainFormat = briefOrPlainFormat;
    }
}
