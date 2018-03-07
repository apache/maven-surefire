package org.apache.maven.plugins.surefire.report;

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

import java.util.ResourceBundle;

/**
 * Surefire Resource Bundle.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 */
public abstract class LocalizedProperties
{
    private final ResourceBundle bundle;

    protected LocalizedProperties( ResourceBundle bundle )
    {
        this.bundle = bundle;
    }

    public abstract String getReportName();
    public abstract String getReportDescription();
    public abstract String getReportHeader();

    protected final String toLocalizedValue( String key )
    {
        return bundle.getString( key );
    }

    public String getReportLabelSummary()
    {
        return toLocalizedValue( "report.surefire.label.summary" );
    }

    public String getReportLabelTests()
    {
        return toLocalizedValue( "report.surefire.label.tests" );
    }

    public String getReportLabelErrors()
    {
        return toLocalizedValue( "report.surefire.label.errors" );
    }

    public String getReportLabelFailures()
    {
        return toLocalizedValue( "report.surefire.label.failures" );
    }

    public String getReportLabelSkipped()
    {
        return toLocalizedValue( "report.surefire.label.skipped" );
    }

    public String getReportLabelSuccessRate()
    {
        return toLocalizedValue( "report.surefire.label.successrate" );
    }

    public String getReportLabelTime()
    {
        return toLocalizedValue( "report.surefire.label.time" );
    }

    public String getReportLabelPackageList()
    {
        return toLocalizedValue( "report.surefire.label.packagelist" );
    }

    public String getReportLabelPackage()
    {
        return toLocalizedValue( "report.surefire.label.package" );
    }

    public String getReportLabelClass()
    {
        return toLocalizedValue( "report.surefire.label.class" );
    }

    public String getReportLabelTestCases()
    {
        return toLocalizedValue( "report.surefire.label.testcases" );
    }

    public String getReportLabelFailureDetails()
    {
        return toLocalizedValue( "report.surefire.label.failuredetails" );
    }

    public String getReportTextNode1()
    {
        return toLocalizedValue( "report.surefire.text.note1" );
    }

    public String getReportTextNode2()
    {
        return toLocalizedValue( "report.surefire.text.note2" );
    }
}
