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

import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.apache.maven.shared.utils.StringUtils.isEmpty;

/**
 * Creates a nicely formatted Surefire Test Report in html format.
 *
 * @author <a href="mailto:jruiz@exist.com">Johnny R. Ruiz III</a>
 */
@Mojo( name = "report", inheritByDefault = false )
@Execute( lifecycle = "surefire", phase = LifecyclePhase.TEST )
@SuppressWarnings( "unused" )
public class SurefireReportMojo
    extends AbstractSurefireReportMojo
{

    /**
     * The filename to use for the report.
     */
    @Parameter( defaultValue = "surefire-report", property = "outputName", required = true )
    private String outputName;

    /**
     * If set to true the surefire report will be generated even when there are no surefire result files.
     * Defaults to {@code true} to preserve legacy behaviour pre 2.10.
     * @since 2.11
     */
    @Parameter( defaultValue = "true", property = "alwaysGenerateSurefireReport" )
    private boolean alwaysGenerateSurefireReport;

    /**
     * If set to true the surefire report generation will be skipped.
     * @since 2.11
     */
    @Parameter( defaultValue = "false", property = "skipSurefireReport" )
    private boolean skipSurefireReport;

    /**
     * A custom title of the report for the menu and the project reports page.
     * @since 2.21.0
     */
    @Parameter( defaultValue = "", property = "surefire.report.title" )
    private String title;

    /**
     * A custom description for the project reports page.
     * @since 2.21.0
     */
    @Parameter( defaultValue = "", property = "surefire.report.description" )
    private String description;

    @Override
    protected File getSurefireReportsDirectory( MavenProject subProject )
    {
        String buildDir = subProject.getBuild().getDirectory();
        return new File( buildDir + "/surefire-reports" );
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
                return isEmpty( SurefireReportMojo.this.getTitle() )
                        ? toLocalizedValue( "report.surefire.name" ) : SurefireReportMojo.this.getTitle();
            }

            @Override
            public String getReportDescription()
            {
                return isEmpty( SurefireReportMojo.this.getDescription() )
                        ? toLocalizedValue( "report.surefire.description" ) : SurefireReportMojo.this.getDescription();
            }

            @Override
            public String getReportHeader()
            {
                return isEmpty( SurefireReportMojo.this.getTitle() )
                        ? toLocalizedValue( "report.surefire.header" ) : SurefireReportMojo.this.getTitle();
            }
        };
    }

    @Override
    protected boolean isSkipped()
    {
        return skipSurefireReport;
    }

    @Override
    protected boolean isGeneratedWhenNoResults()
    {
        return alwaysGenerateSurefireReport;
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
