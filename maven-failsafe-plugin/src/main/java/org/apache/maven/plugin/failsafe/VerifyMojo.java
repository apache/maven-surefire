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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.surefire.SurefireHelper;
import org.apache.maven.plugin.surefire.SurefireReportParameters;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.utils.ReaderFactory;
import org.apache.maven.shared.utils.StringUtils;
import org.apache.maven.shared.utils.io.IOUtil;
import org.apache.maven.surefire.cli.CommandLineOption;
import org.apache.maven.surefire.suite.RunResult;

/**
 * Verify integration tests ran using Surefire.
 *
 * @author Stephen Connolly
 * @author Jason van Zyl
 */
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
     *
     * @noinspection UnusedDeclaration
     */
    @Parameter( defaultValue = "${project.build.directory}/failsafe-reports/failsafe-summary.xml", required = true )
    private File summaryFile;

    /**
     * Additional summary files to read integration test results from.
     *
     * @noinspection UnusedDeclaration, MismatchedReadAndWriteOfArray
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
     *
     * @noinspection UnusedDeclaration
     */
    @Parameter( property = "encoding", defaultValue = "${project.reporting.outputEncoding}" )
    private String encoding;

    /**
     * The current build session instance.
     */
    @Component
    protected MavenSession session;

    private Collection<CommandLineOption> cli;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        cli = commandLineOptions();
        if ( verifyParameters() )
        {
            logDebugOrCliShowErrors(
                StringUtils.capitalizeFirstLetter( getPluginName() ) + " report directory: " + getReportsDirectory() );

            RunResult summary;
            try
            {
                final String encoding;
                if ( StringUtils.isEmpty( this.encoding ) )
                {
                    getLog().warn( "File encoding has not been set, using platform encoding "
                            + ReaderFactory.FILE_ENCODING
                            + ", i.e. build is platform dependent! The file encoding for reports output files "
                            + "should be provided by the POM property ${project.reporting.outputEncoding}." );
                    encoding = ReaderFactory.FILE_ENCODING;
                }
                else
                {
                    encoding = this.encoding;
                }

                summary = existsSummaryFile() ? readSummary( encoding, summaryFile ) : RunResult.noTestsRun();

                if ( existsSummaryFiles() )
                {
                    for ( final File summaryFile : summaryFiles )
                    {
                        summary = summary.aggregate( readSummary( encoding, summaryFile ) );
                    }
                }
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( e.getMessage(), e );
            }

            SurefireHelper.reportExecution( this, summary, getLog() );
        }
    }

    private RunResult readSummary( String encoding, File summaryFile )
        throws IOException
    {
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        Reader reader = null;
        try
        {
            fileInputStream = new FileInputStream( summaryFile );
            bufferedInputStream = new BufferedInputStream( fileInputStream );
            reader = new InputStreamReader( bufferedInputStream, encoding );
            return RunResult.fromInputStream( bufferedInputStream, encoding );
        }
        finally
        {
            IOUtil.close( reader );
            IOUtil.close( bufferedInputStream );
            IOUtil.close( fileInputStream );
        }
    }

    protected boolean verifyParameters()
        throws MojoFailureException
    {
        if ( isSkip() || isSkipTests() || isSkipITs() || isSkipExec() )
        {
            getLog().info( "Tests are skipped." );
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
            getLog().info( "No tests to run." );
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

    public boolean isSkipTests()
    {
        return skipTests;
    }

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

    @Deprecated
    public boolean isSkipExec()
    {
        return skipExec;
    }

    @Deprecated
    public void setSkipExec( boolean skipExec )
    {
        this.skipExec = skipExec;
    }

    public boolean isSkip()
    {
        return skip;
    }

    public void setSkip( boolean skip )
    {
        this.skip = skip;
    }

    public boolean isTestFailureIgnore()
    {
        return testFailureIgnore;
    }

    public void setTestFailureIgnore( boolean testFailureIgnore )
    {
        this.testFailureIgnore = testFailureIgnore;
    }

    public File getBasedir()
    {
        return basedir;
    }

    public void setBasedir( File basedir )
    {
        this.basedir = basedir;
    }

    public File getTestClassesDirectory()
    {
        return testClassesDirectory;
    }

    public void setTestClassesDirectory( File testClassesDirectory )
    {
        this.testClassesDirectory = testClassesDirectory;
    }

    public File getReportsDirectory()
    {
        return reportsDirectory;
    }

    public void setReportsDirectory( File reportsDirectory )
    {
        this.reportsDirectory = reportsDirectory;
    }

    public Boolean getFailIfNoTests()
    {
        return failIfNoTests;
    }

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
        return SurefireHelper.commandLineOptions( session, getLog() );
    }

    private void logDebugOrCliShowErrors( CharSequence s )
    {
        SurefireHelper.logDebugOrCliShowErrors( s, getLog(), cli );
    }

}
