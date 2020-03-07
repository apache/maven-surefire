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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.shared.utils.PathTool;

import static java.util.Collections.addAll;
import static org.apache.maven.plugins.surefire.report.SurefireReportParser.hasReportFiles;
import static org.apache.maven.shared.utils.StringUtils.isEmpty;

/**
 * Abstract base class for reporting test results using Surefire.
 *
 * @author Stephen Connolly
 */
public abstract class AbstractSurefireReportMojo
    extends AbstractMavenReport
{

    /**
     * If set to false, only failures are shown.
     */
    @Parameter( defaultValue = "true", required = true, property = "showSuccess" )
    private boolean showSuccess;

    /**
     * Directories containing the XML Report files that will be parsed and rendered to HTML format.
     */
    @Parameter
    private File[] reportsDirectories;

    /**
     * (Deprecated, use reportsDirectories) This directory contains the XML Report files that will be parsed and
     * rendered to HTML format.
     */
    @Deprecated
    @Parameter
    private File reportsDirectory;

    /**
     * The projects in the reactor for aggregation report.
     */
    @Parameter( defaultValue = "${reactorProjects}", readonly = true )
    private List<MavenProject> reactorProjects;

    /**
     * Location of the Xrefs to link.
     */
    @Parameter( defaultValue = "${project.reporting.outputDirectory}/xref-test" )
    private File xrefLocation;

    /**
     * Whether to link the XRef if found.
     */
    @Parameter( defaultValue = "true", property = "linkXRef" )
    private boolean linkXRef;

    /**
     * Whether to build an aggregated report at the root, or build individual reports.
     */
    @Parameter( defaultValue = "false", property = "aggregate" )
    private boolean aggregate;

    private List<File> resolvedReportsDirectories;

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

    public abstract void setTitle( String title );

    public abstract String getTitle();

    public abstract void setDescription( String description );

    public abstract String getDescription();

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeReport( Locale locale )
        throws MavenReportException
    {
        if ( !hasReportDirectories() )
        {
            return;
        }

        new SurefireReportGenerator( getReportsDirectories(), locale, showSuccess, determineXrefLocation(),
                                           getConsoleLogger() )
                .doGenerateReport( getBundle( locale ), getSink() );
    }

    @Override
    public boolean canGenerateReport()
    {
        return hasReportDirectories() && super.canGenerateReport();
    }

    private boolean hasReportDirectories()
    {
        if ( isSkipped() )
        {
            return false;
        }

        final List<File> reportsDirectories = getReportsDirectories();

        if ( reportsDirectories == null )
        {
            return false;
        }

        if ( !isGeneratedWhenNoResults() )
        {
            boolean atLeastOneDirectoryExists = false;
            for ( Iterator<File> i = reportsDirectories.iterator(); i.hasNext() && !atLeastOneDirectoryExists; )
            {
                atLeastOneDirectoryExists = hasReportFiles( i.next() );
            }
            if ( !atLeastOneDirectoryExists )
            {
                return false;
            }
        }
        return true;
    }

    private List<File> getReportsDirectories()
    {
        if ( resolvedReportsDirectories != null )
        {
            return resolvedReportsDirectories;
        }

        resolvedReportsDirectories = new ArrayList<>();

        if ( this.reportsDirectories != null )
        {
            addAll( resolvedReportsDirectories, this.reportsDirectories );
        }
        //noinspection deprecation
        if ( reportsDirectory != null )
        {
            //noinspection deprecation
            resolvedReportsDirectories.add( reportsDirectory );
        }
        if ( aggregate )
        {
            if ( !project.isExecutionRoot() )
            {
                return null;
            }
            if ( this.reportsDirectories == null )
            {
                for ( MavenProject mavenProject : getProjectsWithoutRoot() )
                {
                    resolvedReportsDirectories.add( getSurefireReportsDirectory( mavenProject ) );
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
                    for ( File reportsDirectory1 : this.reportsDirectories )
                    {
                        String reportDir = reportsDirectory1.getPath();
                        if ( reportDir.startsWith( parentBaseDir ) )
                        {
                            reportDir = reportDir.substring( parentBaseDir.length() );
                        }
                        File reportsDirectory = new File( moduleBaseDir, reportDir );
                        if ( reportsDirectory.exists() && reportsDirectory.isDirectory() )
                        {
                            getConsoleLogger().debug( "Adding report dir : " + moduleBaseDir + reportDir );
                            resolvedReportsDirectories.add( reportsDirectory );
                        }
                    }
                }
            }
        }
        else
        {
            if ( resolvedReportsDirectories.isEmpty() )
            {

                resolvedReportsDirectories.add( getSurefireReportsDirectory( project ) );
            }
        }
        return resolvedReportsDirectories;
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
        List<MavenProject> result = new ArrayList<>();
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
            if ( isEmpty( relativePath ) )
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
                getConsoleLogger().warning( "Unable to locate Test Source XRef to link to - DISABLED" );
            }
        }
        return location;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName( Locale locale )
    {
        return getBundle( locale ).getReportName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getReportDescription();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract String getOutputName();

    protected abstract LocalizedProperties getBundle( Locale locale, ClassLoader resourceBundleClassLoader );

    protected final ConsoleLogger getConsoleLogger()
    {
        return new PluginConsoleLogger( getLog() );
    }

    final LocalizedProperties getBundle( Locale locale )
    {
        return getBundle( locale, getClass().getClassLoader() );
    }
}
