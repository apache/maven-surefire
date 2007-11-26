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
import java.util.ResourceBundle;

import org.apache.maven.model.ReportPlugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.PathTool;
import org.codehaus.plexus.util.StringUtils;


/**
 * Creates a nicely formatted Surefire Test Report in html format.
 *
 * @author <a href="mailto:jruiz@exist.com">Johnny R. Ruiz III</a>
 * @version $Id$
 * @goal report
 * @execute phase="test" lifecycle="surefire"
 * @aggregator
 */
public class SurefireReportMojo
    extends AbstractMavenReport
{
    /**
     * Location where generated html will be created.
     *
     * @parameter expression="${project.reporting.outputDirectory}"
     */
    private String outputDirectory;

    /**
     * Doxia Site Renderer
     *
     * @parameter expression="${component.org.codehaus.doxia.site.renderer.SiteRenderer}"
     * @required @readonly
     */
    private SiteRenderer siteRenderer;

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
     * @deprecated
     * @parameter
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

    public void executeReport( Locale locale )
        throws MavenReportException
    {
        if ( reportsDirectory != null )
        {
            if ( reportsDirectories == null )
            {
                reportsDirectories = new File[] { reportsDirectory };
            }
            else
            {
                File[] oldReports = reportsDirectories;
                reportsDirectories = new File[oldReports.length+1];
                System.arraycopy( oldReports, 0, reportsDirectories, 0, oldReports.length );
                reportsDirectories[oldReports.length] = reportsDirectory;
            }
        }
        if ( aggregate )
        {
            if ( !project.isExecutionRoot() ) return;
            if ( reportsDirectories == null )
            {
                ArrayList reportsDirectoryList = new ArrayList(); 
                // TODO guess the real location
                for (Iterator i = reactorProjects.iterator(); i.hasNext();)
                {
                    MavenProject subProject = (MavenProject) i.next();
                    if ( project.equals( subProject ) ) continue;
                    String buildDir = subProject.getBuild().getDirectory();
                    File reportsDirectory = new File( buildDir + "/surefire-reports" );
                    reportsDirectoryList.add( reportsDirectory );
                }
                reportsDirectories = (File[]) reportsDirectoryList.toArray( new File[0] );
            }
        }
        else
        {
            if ( reportsDirectories == null )
            {
                reportsDirectories = new File[] { new File( project.getBuild().getDirectory() + "/surefire-reports" ) };
            }
        }
        
        
        SurefireReportGenerator report =
            new SurefireReportGenerator( reportsDirectories, locale, showSuccess, determineXrefLocation() );

        report.doGenerateReport( getBundle( locale ), getSink() );
    }

    private String determineXrefLocation()
    {
        String location = null;

        if ( linkXRef )
        {
            String relativePath = PathTool.getRelativePath( outputDirectory, xrefLocation.getAbsolutePath() );
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

    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.surefire.name" );
    }

    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.surefire.description" );
    }

    protected SiteRenderer getSiteRenderer()
    {
        return siteRenderer;
    }

    protected MavenProject getProject()
    {
        return project;
    }

    public String getOutputName()
    {
        return outputName;
    }

    protected String getOutputDirectory()
    {
        return outputDirectory;
    }

    private ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "surefire-report", locale, this.getClass().getClassLoader() );
    }

}
