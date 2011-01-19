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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.PathTool;
import org.codehaus.plexus.util.StringUtils;


/**
 * Creates a nicely formatted Surefire Test Report in html format.
 *
 * @author <a href="mailto:jruiz@exist.com">Johnny R. Ruiz III</a>
 * @version $Id$
 * @goal report
 * @execute phase="test" lifecycle="surefire"
 */
public class SurefireReportMojo
    extends AbstractMavenReport
{
    /**
     * Location where generated html will be created.
     *
     * @parameter expression="${project.reporting.outputDirectory}"
     */
    private File outputDirectory;

    /**
     * Doxia Site Renderer
     *
     * @component
     */
    private Renderer siteRenderer;

    /**
     * Maven Project
     *
     * @parameter expression="${project}"
     * @required @readonly
     */
    private MavenProject project;

    /**
     * If set to false, only failures are shown.
     *
     * @parameter expression="${showSuccess}" default-value="true"
     * @required
     */
    private boolean showSuccess;

    /**
     * Directories containing the XML Report files that will be parsed and rendered to HTML format.
     *
     * @parameter
     */
    private File[] reportsDirectories;

    /**
     * (Deprecated, use reportsDirectories) This directory contains the XML Report files that will be parsed and rendered to HTML format.
     *
     * @parameter
     * @deprecated
     */
    private File reportsDirectory;


    /**
     * The projects in the reactor for aggregation report.
     *
     * @parameter expression="${reactorProjects}"
     * @readonly
     */
    private List reactorProjects;

    /**
     * The filename to use for the report.
     *
     * @parameter expression="${outputName}" default-value="surefire-report"
     * @required
     */
    private String outputName;

    /**
     * Location of the Xrefs to link.
     *
     * @parameter default-value="${project.reporting.outputDirectory}/xref-test"
     */
    private File xrefLocation;

    /**
     * Whether to link the XRef if found.
     *
     * @parameter expression="${linkXRef}" default-value="true"
     */
    private boolean linkXRef;

    /**
     * Whether to build an aggregated report at the root, or build individual reports.
     *
     * @parameter expression="${aggregate}" default-value="false"
     */
    private boolean aggregate;

    /**
     * {@inheritDoc}
     */
    public void executeReport( Locale locale )
        throws MavenReportException
    {
        final List reportsDirectoryList = new ArrayList();

        if ( reportsDirectory != null )
        {
            if ( reportsDirectories != null )
            {
                reportsDirectoryList.addAll( Arrays.asList( reportsDirectories ) );
            }
            reportsDirectoryList.add( reportsDirectory );
        }
        if ( aggregate )
        {
            if ( !project.isExecutionRoot() )
            {
                return;
            }
            if ( reportsDirectories == null )
            {
                for ( Iterator i = getProjectsWithoutRoot().iterator(); i.hasNext(); )
                {
                    reportsDirectoryList.add( getSurefireReportsDirectory( (MavenProject) i.next() ) );
                }
            }
            else
            {
                // Multiple report directories are configured.
                // Let's see if those directories exist in each sub-module to fix SUREFIRE-570
                String parentBaseDir = getProject().getBasedir().getAbsolutePath();
                for ( Iterator i = getProjectsWithoutRoot().iterator(); i.hasNext(); )
                {
                    MavenProject subProject = (MavenProject) i.next();
                    String moduleBaseDir = subProject.getBasedir().getAbsolutePath();
                    for ( int d = 0; d < reportsDirectories.length; d++ )
                    {
                        String reportDir = reportsDirectories[d].getPath();
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

        SurefireReportGenerator report =
            new SurefireReportGenerator( reportsDirectoryList, locale, showSuccess, determineXrefLocation() );

        report.doGenerateReport( getBundle( locale ), getSink() );
    }

    private File getSurefireReportsDirectory( MavenProject subProject )
    {
        String buildDir = subProject.getBuild().getDirectory();
        return new File( buildDir + "/surefire-reports" );
    }

    private List getProjectsWithoutRoot()
    {
        List result = new ArrayList();
        for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
        {
            MavenProject subProject = (MavenProject) i.next();
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
                for ( Iterator reports = project.getReportPlugins().iterator(); reports.hasNext(); )
                {
                    ReportPlugin report = (ReportPlugin) reports.next();

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
    public String getOutputName()
    {
        return outputName;
    }

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
