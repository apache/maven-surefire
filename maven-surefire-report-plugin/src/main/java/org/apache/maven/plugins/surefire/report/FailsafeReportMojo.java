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

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.apache.maven.shared.utils.StringUtils.isEmpty;

/**
 * Creates a nicely formatted Failsafe Test Report in html format.
 * This goal does not run the tests, it only builds the reports.
 * See <a href="https://issues.apache.org/jira/browse/SUREFIRE-257">
 *     https://issues.apache.org/jira/browse/SUREFIRE-257</a>
 *
 * @author Stephen Connolly
 * @since 2.10
 */
@Mojo( name = "failsafe-report-only" )
@SuppressWarnings( "unused" )
public class FailsafeReportMojo
    extends AbstractSurefireReportMojo
{

    /**
     * The filename to use for the report.
     */
    @Parameter( defaultValue = "failsafe-report", property = "outputName", required = true )
    private String outputName;

    /**
     * If set to true the failsafe report will be generated even when there are no failsafe result files.
     * Defaults to {@code false} to preserve legacy behaviour pre 2.10.
     * @since 2.11
     */
    @Parameter( defaultValue = "false", property = "alwaysGenerateFailsafeReport" )
    private boolean alwaysGenerateFailsafeReport;

    /**
     * If set to true the failsafe report generation will be skipped.
     * @since 2.11
     */
    @Parameter( defaultValue = "false", property = "skipFailsafeReport" )
    private boolean skipFailsafeReport;

    /**
     * A custom title of the report for the menu and the project reports page.
     * @since 2.21.0
     */
    @Parameter( defaultValue = "", property = "failsafe.report.title" )
    private String title;

    /**
     * A custom description for the project reports page.
     * @since 2.21.0
     */
    @Parameter( defaultValue = "", property = "failsafe.report.description" )
    private String description;

    @Override
    protected File getSurefireReportsDirectory( MavenProject subProject )
    {
        String buildDir = subProject.getBuild().getDirectory();
        return new File( buildDir + "/failsafe-reports" );
    }

    @Override
    public String getOutputName()
    {
        return outputName;
    }

    @Override
    protected LocalizedProperties getBundle( Locale locale, ClassLoader resourceBundleClassLoader )
    {
        ResourceBundle bundle = ResourceBundle.getBundle( "surefire-report", locale, resourceBundleClassLoader );
        return new LocalizedProperties( bundle )
        {
            @Override
            public String getReportName()
            {
                return isEmpty( FailsafeReportMojo.this.getTitle() )
                        ? toLocalizedValue( "report.failsafe.name" ) : FailsafeReportMojo.this.getTitle();
            }

            @Override
            public String getReportDescription()
            {
                return isEmpty( FailsafeReportMojo.this.getDescription() )
                        ? toLocalizedValue( "report.failsafe.description" ) : FailsafeReportMojo.this.getDescription();
            }

            @Override
            public String getReportHeader()
            {
                return isEmpty( FailsafeReportMojo.this.getTitle() )
                        ? toLocalizedValue( "report.failsafe.header" ) : FailsafeReportMojo.this.getTitle();
            }
        };
    }

    @Override
    protected boolean isSkipped()
    {
        return skipFailsafeReport;
    }

    @Override
    protected boolean isGeneratedWhenNoResults()
    {
        return alwaysGenerateFailsafeReport;
    }

    @Override
    public void setTitle( String title )
    {
        this.title = title;
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    @Override
    public void setDescription( String description )
    {
        this.description = description;
    }

    @Override
    public String getDescription()
    {
        return description;
    }
}
