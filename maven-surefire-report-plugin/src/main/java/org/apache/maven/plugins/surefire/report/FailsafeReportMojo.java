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

import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;


/**
 * Creates a nicely formatted Failsafe Test Report in html format.
 * This goal does not run the tests, it only builds the reports.
 * See <a href="http://jira.codehaus.org/browse/SUREFIRE-257">http://jira.codehaus.org/browse/SUREFIRE-257</a>
 *
 * @author Stephen Connolly
 * @since 2.10
 */
@Mojo(name = "failsafe-report-only")
public class FailsafeReportMojo
    extends AbstractSurefireReportMojo
{

    /**
     * The filename to use for the report.
     *
     * @noinspection UnusedDeclaration
     */
    @Parameter(defaultValue = "failsafe-report", property = "outputName", required = true)
    private String outputName;

    /**
     * If set to true the failsafe report will be generated even when there are no failsafe result files.
     * Defaults to <code>false</code> to preserve legacy behaviour pre 2.10
     *
     * @noinspection UnusedDeclaration
     * @since 2.11
     */
    @Parameter(defaultValue = "false", property = "alwaysGenerateFailsafeReport")
    private boolean alwaysGenerateFailsafeReport;

    /**
     * If set to true the failsafe report generation will be skipped.
     *
     * @noinspection UnusedDeclaration
     * @since 2.11
     */
    @Parameter(defaultValue = "false", property = "skipFailsafeReport")
    private boolean skipFailsafeReport;

    protected File getSurefireReportsDirectory( MavenProject subProject )
    {
        String buildDir = subProject.getBuild().getDirectory();
        return new File( buildDir + "/failsafe-reports" );
    }

    public String getOutputName()
    {
        return outputName;
    }

    protected boolean isSkipped()
    {
        return skipFailsafeReport;
    }

    protected boolean isGeneratedWhenNoResults()
    {
        return alwaysGenerateFailsafeReport;
    }

    /**
     * {@inheritDoc}
     */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.failsafe.name" );
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.failsafe.description" );
    }


    /*
    * This is currently a copy of the getBundle() method of the AbstractSurefireReportMojo class,
    * cause the failsafe report only different in two names for the bundles.
    */
    private ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "surefire-report", locale, this.getClass().getClassLoader() );
    }
}
