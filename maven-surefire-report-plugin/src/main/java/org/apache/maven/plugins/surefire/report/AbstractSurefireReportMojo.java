package org.apache.maven.plugins.surefire.report;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
 * Abstract base class for reporting test results using Surefire.
 *
 * @author Stephen Connolly
 * @version $Id$
 */
public abstract class AbstractSurefireReportMojo
    extends AbstractMavenReport
{
    /**
     * Location where generated html will be created.
     *
     * @parameter expression="${project.reporting.outputDirectory}"
     * @noinspection UnusedDeclaration
     */
    private File outputDirectory;

    /**
     * Doxia Site Renderer
     *
     * @component
     * @noinspection UnusedDeclaration
     */
    private Renderer siteRenderer;

    /**
     * Maven Project
     *
     * @parameter expression="${project}"
     * @required @readonly
     * @noinspection UnusedDeclaration
     */
    private MavenProject project;

    /**
     * If set to false, only failures are shown.
     *
     * @parameter expression="${showSuccess}" default-value="true"
     * @required
     * @noinspection UnusedDeclaration
     */
    private boolean showSuccess;

    /**
     * Directories containing the XML Report files that will be parsed and rendered to HTML format.
     *
     * @parameter
     * @noinspection UnusedDeclaration
     */
    private File[] reportsDirectories;

    /**
     * (Deprecated, use reportsDirectories) This directory contains the XML Report files that will be parsed and rendered to HTML format.
     *
     * @parameter
     * @noinspection UnusedDeclaration
     * @deprecated
     */
    private File reportsDirectory;

    /**
     * The projects in the reactor for aggregation report.
     *
     * @parameter expression="${reactorProjects}"
     * @readonly
     * @noinspection MismatchedQueryAndUpdateOfCollection, UnusedDeclaration
     */
    private List<MavenProject> reactorProjects;

    /**
     * Location of the Xrefs to link.
     *
     * @parameter default-value="${project.reporting.outputDirectory}/xref-test"
     * @noinspection UnusedDeclaration
     */
    private File xrefLocation;

    /**
     * Whether to link the XRef if found.
     *
     * @parameter expression="${linkXRef}" default-value="true"
     * @noinspection UnusedDeclaration
     */
    private boolean linkXRef;

    /**
     * Whether to build an aggregated report at the root, or build individual reports.
     *
     * @parameter expression="${aggregate}" default-value="false"
     * @noinspection UnusedDeclaration
     */
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
            for ( Iterator i = reportsDirectoryList.iterator(); i.hasNext() && !atLeastOneDirectoryExists; )
            {
                atLeastOneDirectoryExists = SurefireReportParser.hasReportFiles( (File) i.next() );
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

        final List reportsDirectoryList = getReportsDirectories();

        if ( reportsDirectoryList == null )
        {
            return false;
        }

        if ( !isGeneratedWhenNoResults() )
        {
            boolean atLeastOneDirectoryExists = false;
            for ( Iterator i = reportsDirectoryList.iterator(); i.hasNext() && !atLeastOneDirectoryExists; )
            {
                atLeastOneDirectoryExists = SurefireReportParser.hasReportFiles( (File) i.next() );
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
