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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.shared.utils.PathTool;
import org.apache.maven.shared.utils.StringUtils;

/**
 * Abstract base class for reporting test results using Surefire.
 *
 * @author Stephen Connolly
 */
public abstract class AbstractSurefireReportMojo
    extends AbstractMavenReport
{
    /**
     * Location where generated html will be created.
     *
     * @noinspection UnusedDeclaration
     */
    @Parameter(property = "project.reporting.outputDirectory")
    private File outputDirectory;

    /**
     * Doxia Site Renderer
     *
     * @noinspection UnusedDeclaration
     */
    @Component
    private Renderer siteRenderer;

    /**
     * Maven Project
     *
     * @noinspection UnusedDeclaration
     */
    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    /**
     * If set to false, only failures are shown.
     *
     * @noinspection UnusedDeclaration
     */
    @Parameter(defaultValue = "true", required = true, property = "showSuccess")
    private boolean showSuccess;

    /**
     * Directories containing the XML Report files that will be parsed and rendered to HTML format.
     *
     * @noinspection UnusedDeclaration
     */
    @Parameter
    private File[] reportsDirectories;

    /**
     * (Deprecated, use reportsDirectories) This directory contains the XML Report files that will be parsed and rendered to HTML format.
     *
     * @noinspection UnusedDeclaration
     */
    @Deprecated
    @Parameter
    private File reportsDirectory;

    /**
     * The projects in the reactor for aggregation report.
     *
     * @noinspection MismatchedQueryAndUpdateOfCollection, UnusedDeclaration
     */
    @Parameter(property = "reactorProjects", readonly = true)
    private List<MavenProject> reactorProjects;

    /**
     * Location of the Xrefs to link.
     *
     * @noinspection UnusedDeclaration
     */
    @Parameter(defaultValue = "${project.reporting.outputDirectory}/xref-test")
    private File xrefLocation;

    /**
     * Whether to link the XRef if found.
     *
     * @noinspection UnusedDeclaration
     */
    @Parameter(defaultValue = "true", property = "linkXRef")
    private boolean linkXRef;

    /**
     * Whether to build an aggregated report at the root, or build individual reports.
     *
     * @noinspection UnusedDeclaration
     */
    @Parameter(defaultValue = "false", property = "aggregate")
    private boolean aggregate;

    /**
     * Whether the report should be generated or not.
     *
     * @return {@code true} if and only if the report should be generated.
     * @since 2.11
     */
    protected boolean isSkipped()
    {
        return false;
    }

    /**
     * Whether the report should be generated when there are no test results.
     *
     * @return {@code true} if and only if the report should be generated when there are no result files at all.
     * @since 2.11
     */
    protected boolean isGeneratedWhenNoResults()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void executeReport( Locale locale )
        throws MavenReportException
    {
        if ( isSkipped() )
        {
            return;
        }

        final List<File> reportsDirectoryList = getReportsDirectories();

        if ( reportsDirectoryList == null )
        {
            return;
        }

        if ( !isGeneratedWhenNoResults() )
        {
            boolean atLeastOneDirectoryExists = false;
            for ( Iterator<File> i = reportsDirectoryList.iterator(); i.hasNext() && !atLeastOneDirectoryExists; )
            {
                atLeastOneDirectoryExists = SurefireReportParser.hasReportFiles( i.next() );
            }
            if ( !atLeastOneDirectoryExists )
            {
                return;
            }
        }

        SurefireReportGenerator report =
            new SurefireReportGenerator( reportsDirectoryList, locale, showSuccess, determineXrefLocation() );

        report.doGenerateReport( getBundle( locale ), getSink() );
    }

    public boolean canGenerateReport()
    {
        if ( isSkipped() )
        {
            return false;
        }

        final List<File> reportsDirectoryList = getReportsDirectories();

        if ( reportsDirectoryList == null )
        {
            return false;
        }

        if ( !isGeneratedWhenNoResults() )
        {
            boolean atLeastOneDirectoryExists = false;
            for ( Iterator<File> i = reportsDirectoryList.iterator(); i.hasNext() && !atLeastOneDirectoryExists; )
            {
                atLeastOneDirectoryExists = SurefireReportParser.hasReportFiles( i.next() );
            }
            if ( !atLeastOneDirectoryExists )
            {
                return false;
            }
        }

        return super.canGenerateReport();
    }

    private List<File> getReportsDirectories()
    {
        final List<File> reportsDirectoryList = new ArrayList<File>();

        if ( reportsDirectories != null )
        {
            reportsDirectoryList.addAll( Arrays.asList( reportsDirectories ) );
        }
        //noinspection deprecation
        if ( reportsDirectory != null )
        {
            //noinspection deprecation
            reportsDirectoryList.add( reportsDirectory );
        }
        if ( aggregate )
        {
            if ( !project.isExecutionRoot() )
            {
                return null;
            }
            if ( reportsDirectories == null )
            {
                for ( MavenProject mavenProject : getProjectsWithoutRoot() )
                {
                    reportsDirectoryList.add( getSurefireReportsDirectory( mavenProject ) );
                }
            }
            else
            {
                // Multiple report directories are configured.
                // Let's see if those directories exist in each sub-module to fix SUREFIRE-570
                String parentBaseDir = getProject().getBasedir().getAbsolutePath();
                for ( MavenProject subProject : getProjectsWithoutRoot() )
                {
                    String moduleBaseDir = subProject.getBasedir().getAbsolutePath();
                    for ( File reportsDirectory1 : reportsDirectories )
                    {
                        String reportDir = reportsDirectory1.getPath();
                        if ( reportDir.startsWith( parentBaseDir ) )
                        {
                            reportDir = reportDir.substring( parentBaseDir.length() );
                        }
                        File reportsDirectory = new File( moduleBaseDir, reportDir );
                        if ( reportsDirectory.exists() && reportsDirectory.isDirectory() )
                        {
                            getLog().debug( "Adding report dir : " + moduleBaseDir + reportDir );
                            reportsDirectoryList.add( reportsDirectory );
                        }
                    }
                }
            }
        }
        else
        {
            if ( reportsDirectoryList.size() == 0 )
            {

                reportsDirectoryList.add( getSurefireReportsDirectory( project ) );
            }
        }
        return reportsDirectoryList;
    }

    /**
     * Gets the default surefire reports directory for the specified project.
     *
     * @param subProject the project to query.
     * @return the default surefire reports directory for the specified project.
     */
    protected abstract File getSurefireReportsDirectory( MavenProject subProject );

    private List<MavenProject> getProjectsWithoutRoot()
    {
        List<MavenProject> result = new ArrayList<MavenProject>();
        for ( MavenProject subProject : reactorProjects )
        {
            if ( !project.equals( subProject ) )
            {
                result.add( subProject );
            }
        }
        return result;

    }

    private String determineXrefLocation()
    {
        String location = null;

        if ( linkXRef )
        {
            String relativePath = PathTool.getRelativePath( getOutputDirectory(), xrefLocation.getAbsolutePath() );
            if ( StringUtils.isEmpty( relativePath ) )
            {
                relativePath = ".";
            }
            relativePath = relativePath + "/" + xrefLocation.getName();
            if ( xrefLocation.exists() )
            {
                // XRef was already generated by manual execution of a lifecycle binding
                location = relativePath;
            }
            else
            {
                // Not yet generated - check if the report is on its way
                for ( Object o : project.getReportPlugins() )
                {
                    ReportPlugin report = (ReportPlugin) o;

                    String artifactId = report.getArtifactId();
                    if ( "maven-jxr-plugin".equals( artifactId ) || "jxr-maven-plugin".equals( artifactId ) )
                    {
                        location = relativePath;
                    }
                }
            }

            if ( location == null )
            {
                getLog().warn( "Unable to locate Test Source XRef to link to - DISABLED" );
            }
        }
        return location;
    }

    /**
     * {@inheritDoc}
     */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.surefire.name" );
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.surefire.description" );
    }

    /**
     * {@inheritDoc}
     */
    protected Renderer getSiteRenderer()
    {
        return siteRenderer;
    }

    /**
     * {@inheritDoc}
     */
    protected MavenProject getProject()
    {
        return project;
    }

    /**
     * {@inheritDoc}
     */
    public abstract String getOutputName();

    /**
     * {@inheritDoc}
     */
    protected String getOutputDirectory()
    {
        return outputDirectory.getAbsolutePath();
    }

    private ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "surefire-report", locale, this.getClass().getClassLoader() );
    }
}
