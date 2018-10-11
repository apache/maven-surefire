package org.apache.maven.plugin.failsafe;

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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.failsafe.util.FailsafeSummaryXmlUtils;
import org.apache.maven.plugin.surefire.SurefireHelper;
import org.apache.maven.plugin.surefire.SurefireReportParameters;
import org.apache.maven.plugin.surefire.log.PluginConsoleLogger;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.surefire.cli.CommandLineOption;
import org.apache.maven.surefire.suite.RunResult;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.util.Collection;

import static org.apache.maven.plugin.surefire.SurefireHelper.reportExecution;
import static org.apache.maven.shared.utils.StringUtils.capitalizeFirstLetter;
import static org.apache.maven.surefire.suite.RunResult.noTestsRun;

/**
 * Verify integration tests ran using Surefire.
 *
 * @author Stephen Connolly
 * @author Jason van Zyl
 */
@SuppressWarnings( "unused" )
@Mojo( name = "verify", defaultPhase = LifecyclePhase.VERIFY, requiresProject = true, threadSafe = true )
public class VerifyMojo
        extends AbstractMojo
        implements SurefireReportParameters
{

    /**
     * Set this to 'true' to skip running tests, but still compile them. Its use is NOT RECOMMENDED, but quite
     * convenient on occasion.
     *
     * @since 2.4
     */
    @Parameter( property = "skipTests" )
    private boolean skipTests;

    /**
     * Set this to 'true' to skip running integration tests, but still compile them. Its use is NOT RECOMMENDED, but
     * quite convenient on occasion.
     *
     * @since 2.4.3-alpha-2
     */
    @Parameter( property = "skipITs" )
    private boolean skipITs;

    /**
     * This old parameter is just like skipTests, but bound to the old property maven.test.skip.exec.
     *
     * @since 2.3
     * @deprecated Use -DskipTests instead.
     */
    @Deprecated
    @Parameter( property = "maven.test.skip.exec" )
    private boolean skipExec;

    /**
     * Set this to 'true' to bypass unit tests entirely. Its use is NOT RECOMMENDED, especially if you
     * enable it using the "maven.test.skip" property, because maven.test.skip disables both running the
     * tests and compiling the tests.  Consider using the skipTests parameter instead.
     */
    @Parameter( property = "maven.test.skip", defaultValue = "false" )
    private boolean skip;

    /**
     * Set this to true to ignore a failure during testing. Its use is NOT RECOMMENDED, but quite convenient on
     * occasion.
     */
    @Parameter( property = "maven.test.failure.ignore", defaultValue = "false" )
    private boolean testFailureIgnore;

    /**
     * The base directory of the project being tested. This can be obtained in your unit test by
     * System.getProperty("basedir").
     */
    @Parameter( defaultValue = "${basedir}" )
    private File basedir;

    /**
     * The directory containing generated test classes of the project being tested.
     * This will be included at the beginning the test classpath.
     */
    @Parameter( defaultValue = "${project.build.testOutputDirectory}" )
    private File testClassesDirectory;

    /**
     * Base directory where all reports are written to.
     */
    @Parameter( defaultValue = "${project.build.directory}/failsafe-reports" )
    private File reportsDirectory;

    /**
     * The summary file to read integration test results from.
     */
    @Parameter( defaultValue = "${project.build.directory}/failsafe-reports/failsafe-summary.xml", required = true )
    private File summaryFile;

    /**
     * Additional summary files to read integration test results from.
     * @since 2.6
     */
    @Parameter
    private File[] summaryFiles;

    /**
     * Set this to "true" to cause a failure if there are no tests to run.
     *
     * @since 2.4
     */
    @Parameter( property = "failIfNoTests" )
    private Boolean failIfNoTests;

    /**
     * The character encoding scheme to be applied.
     * Deprecated since 2.20.1 and used encoding UTF-8 in <tt>failsafe-summary.xml</tt>.
     *
     * @deprecated since of 2.20.1
     */
    @Parameter( property = "encoding", defaultValue = "${project.reporting.outputEncoding}" )
    private String encoding;

    /**
     * The current build session instance.
     */
    @Parameter( defaultValue = "${session}", readonly = true )
    private MavenSession session;

    @Component
    private Logger logger;

    private Collection<CommandLineOption> cli;

    private volatile PluginConsoleLogger consoleLogger;

    @Override
    public void execute()
            throws MojoExecutionException, MojoFailureException
    {
        cli = commandLineOptions();
        if ( verifyParameters() )
        {
            logDebugOrCliShowErrors( capitalizeFirstLetter( getPluginName() )
                                             + " report directory: " + getReportsDirectory() );

            RunResult summary;
            try
            {
                summary = existsSummaryFile() ? readSummary( summaryFile ) : noTestsRun();

                if ( existsSummaryFiles() )
                {
                    for ( final File summaryFile : summaryFiles )
                    {
                        summary = summary.aggregate( readSummary( summaryFile ) );
                    }
                }
            }
            catch ( Exception e )
            {
                throw new MojoExecutionException( e.getMessage(), e );
            }

            reportExecution( this, summary, getConsoleLogger(), null );
        }
    }

    private PluginConsoleLogger getConsoleLogger()
    {
        if ( consoleLogger == null )
        {
            synchronized ( this )
            {
                if ( consoleLogger == null )
                {
                    consoleLogger = new PluginConsoleLogger( logger );
                }
            }
        }
        return consoleLogger;
    }

    private RunResult readSummary( File summaryFile ) throws Exception
    {
        return FailsafeSummaryXmlUtils.toRunResult( summaryFile );
    }

    protected boolean verifyParameters()
            throws MojoFailureException
    {
        if ( isSkip() || isSkipTests() || isSkipITs() || isSkipExec() )
        {
            getConsoleLogger().info( "Tests are skipped." );
            return false;
        }

        if ( !getTestClassesDirectory().exists() )
        {
            if ( getFailIfNoTests() != null && getFailIfNoTests() )
            {
                throw new MojoFailureException( "No tests to run!" );
            }
        }

        if ( !existsSummary() )
        {
            getConsoleLogger().info( "No tests to run." );
            return false;
        }

        return true;
    }

    protected String getPluginName()
    {
        return "failsafe";
    }

    protected String[] getDefaultIncludes()
    {
        return null;
    }

    @Override
    public boolean isSkipTests()
    {
        return skipTests;
    }

    @Override
    public void setSkipTests( boolean skipTests )
    {
        this.skipTests = skipTests;
    }

    public boolean isSkipITs()
    {
        return skipITs;
    }

    public void setSkipITs( boolean skipITs )
    {
        this.skipITs = skipITs;
    }

    @Override
    @Deprecated
    public boolean isSkipExec()
    {
        return skipExec;
    }

    @Override
    @Deprecated
    public void setSkipExec( boolean skipExec )
    {
        this.skipExec = skipExec;
    }

    @Override
    public boolean isSkip()
    {
        return skip;
    }

    @Override
    public void setSkip( boolean skip )
    {
        this.skip = skip;
    }

    @Override
    public boolean isTestFailureIgnore()
    {
        return testFailureIgnore;
    }

    @Override
    public void setTestFailureIgnore( boolean testFailureIgnore )
    {
        this.testFailureIgnore = testFailureIgnore;
    }

    @Override
    public File getBasedir()
    {
        return basedir;
    }

    @Override
    public void setBasedir( File basedir )
    {
        this.basedir = basedir;
    }

    @Override
    public File getTestClassesDirectory()
    {
        return testClassesDirectory;
    }

    @Override
    public void setTestClassesDirectory( File testClassesDirectory )
    {
        this.testClassesDirectory = testClassesDirectory;
    }

    @Override
    public File getReportsDirectory()
    {
        return reportsDirectory;
    }

    @Override
    public void setReportsDirectory( File reportsDirectory )
    {
        this.reportsDirectory = reportsDirectory;
    }

    @Override
    public Boolean getFailIfNoTests()
    {
        return failIfNoTests;
    }

    @Override
    public void setFailIfNoTests( boolean failIfNoTests )
    {
        this.failIfNoTests = failIfNoTests;
    }

    private boolean existsSummaryFile()
    {
        return summaryFile != null && summaryFile.isFile();
    }

    private boolean existsSummaryFiles()
    {
        return summaryFiles != null && summaryFiles.length != 0;
    }

    private boolean existsSummary()
    {
        return existsSummaryFile() || existsSummaryFiles();
    }

    private Collection<CommandLineOption> commandLineOptions()
    {
        return SurefireHelper.commandLineOptions( session, getConsoleLogger() );
    }

    private void logDebugOrCliShowErrors( String s )
    {
        SurefireHelper.logDebugOrCliShowErrors( s, getConsoleLogger(), cli );
    }

}
