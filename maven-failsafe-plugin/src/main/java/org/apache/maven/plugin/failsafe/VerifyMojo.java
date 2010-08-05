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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.surefire.SurefireHelper;
import org.apache.maven.plugin.surefire.SurefireReportParameters;
import org.apache.maven.surefire.failsafe.model.FailsafeSummary;
import org.apache.maven.surefire.failsafe.model.io.xpp3.FailsafeSummaryXpp3Reader;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Verify integration tests ran using Surefire.
 *
 * @author Stephen Connolly
 * @author Jason van Zyl
 * @requiresProject true
 * @goal verify
 * @phase verify
 * @threadSafe
 */
public class VerifyMojo
    extends AbstractMojo
    implements SurefireReportParameters
{

    /**
     * Set this to 'true' to skip running tests, but still compile them. Its use is NOT RECOMMENDED, but quite
     * convenient on occasion.
     *
     * @parameter expression="${skipTests}"
     * @since 2.4
     */
    private boolean skipTests;

    /**
     * Set this to 'true' to skip running integration tests, but still compile them. Its use is NOT RECOMMENDED, but quite
     * convenient on occasion.
     *
     * @parameter expression="${skipITs}"
     * @since 2.4.3-alpha-2
     */
    private boolean skipITs;

    /**
     * This old parameter is just like skipTests, but bound to the old property maven.test.skip.exec.
     *
     * @parameter expression="${maven.test.skip.exec}"
     * @since 2.3
     * @deprecated Use -DskipTests instead.
     */
    private boolean skipExec;

    /**
     * Set this to 'true' to bypass unit tests entirely. Its use is NOT RECOMMENDED, especially if you
     * enable it using the "maven.test.skip" property, because maven.test.skip disables both running the
     * tests and compiling the tests.  Consider using the skipTests parameter instead.
     *
     * @parameter default-value="false" expression="${maven.test.skip}"
     */
    private boolean skip;

    /**
     * Set this to true to ignore a failure during testing. Its use is NOT RECOMMENDED, but quite convenient on
     * occasion.
     *
     * @parameter default-value="false" expression="${maven.test.failure.ignore}"
     */
    private boolean testFailureIgnore;

    /**
     * The base directory of the project being tested. This can be obtained in your unit test by
     * System.getProperty("basedir").
     *
     * @parameter default-value="${basedir}"
     */
    private File basedir;

    /**
     * The directory containing generated test classes of the project being tested.
     * This will be included at the beginning the test classpath.
     *
     * @parameter default-value="${project.build.testOutputDirectory}"
     */
    private File testClassesDirectory;

    /**
     * Base directory where all reports are written to.
     *
     * @parameter default-value="${project.build.directory}/failsafe-reports"
     */
    private File reportsDirectory;

    /**
     * The summary file to read integration test results from.
     *
     * @parameter expression="${project.build.directory}/failsafe-reports/failsafe-summary.xml"
     * @required
     */
    private File summaryFile;

    /**
     * Additional summary files to read integration test results from.
     *
     * @parameter
     * @since 2.6
     */
    private File[] summaryFiles;

    /**
     * Set this to "true" to cause a failure if there are no tests to run.
     *
     * @parameter expression="${failIfNoTests}"
     * @since 2.4
     */
    private Boolean failIfNoTests;

    /**
     * The character encoding scheme to be applied.
     *
     * @parameter expression="${encoding}" default-value="${project.reporting.outputEncoding}"
     */
    private String encoding;


    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( verifyParameters() )
        {
            getLog().info(
                StringUtils.capitalizeFirstLetter( getPluginName() ) + " report directory: " + getReportsDirectory() );

            int result;
            try
            {
                String encoding;
                if ( StringUtils.isEmpty( this.encoding ) )
                {
                    getLog().warn(
                        "File encoding has not been set, using platform encoding " + ReaderFactory.FILE_ENCODING
                            + ", i.e. build is platform dependent!" );
                    encoding = ReaderFactory.FILE_ENCODING;
                }
                else
                {
                    encoding = this.encoding;
                }

                final FailsafeSummary summary;
                if ( !summaryFile.isFile() && summaryFiles != null )
                {
                    summary = new FailsafeSummary();
                }
                else
                {                    
                    summary = readSummary( encoding, summaryFile );
                }
                if ( summaryFiles != null )
                {
                    for ( int i = 0; i < summaryFiles.length; i++ )
                    {
                        summary.merge( readSummary( encoding, summaryFiles[i] ) );
                    }
                }
                result = summary.getResult();
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( e.getMessage(), e );
            }
            catch ( XmlPullParserException e )
            {
                throw new MojoExecutionException( e.getMessage(), e );
            }

            SurefireHelper.reportExecution( this, result, getLog() );
        }
    }

    private FailsafeSummary readSummary( String encoding, File summaryFile )
            throws IOException, XmlPullParserException
    {
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        Reader reader = null;
        try
        {
            fileInputStream = new FileInputStream( summaryFile );
            bufferedInputStream = new BufferedInputStream( fileInputStream );
            reader = new InputStreamReader( bufferedInputStream, encoding );
            FailsafeSummaryXpp3Reader xpp3Reader = new FailsafeSummaryXpp3Reader();
            return xpp3Reader.read( reader );
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
            if ( getFailIfNoTests() != null && getFailIfNoTests().booleanValue() )
            {
                throw new MojoFailureException( "No tests to run!" );
            }
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

    public boolean isSkipExec()
    {
        return skipExec;
    }

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

    public void setFailIfNoTests( Boolean failIfNoTests )
    {
        this.failIfNoTests = failIfNoTests;
    }

}
