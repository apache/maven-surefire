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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.surefire.AbstractSurefireMojo;
import org.apache.maven.plugin.surefire.booterclient.ChecksumCalculator;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.utils.ReaderFactory;
import org.apache.maven.shared.utils.StringUtils;
import org.apache.maven.surefire.suite.RunResult;

/**
 * Run integration tests using Surefire.
 *
 * @author Jason van Zyl
 * @author Stephen Connolly
 * @noinspection JavaDoc,
 */
@Mojo( name = "integration-test", requiresProject = true, requiresDependencyResolution = ResolutionScope.TEST,
       defaultPhase = LifecyclePhase.INTEGRATION_TEST, threadSafe = true )
public class IntegrationTestMojo
    extends AbstractSurefireMojo
{

    private static final String FAILSAFE_IN_PROGRESS_CONTEXT_KEY = "failsafe-in-progress";

    /**
     * The path representing project <em>JAR</em> file, if exists; Otherwise the directory containing generated
     * classes of the project being tested. This will be included after the test classes in the test classpath.
     */
    @Parameter( defaultValue = "${project.build.outputDirectory}" )
    private File classesDirectory;

    /**
     * Set this to "true" to skip running integration tests, but still compile them. Its use is NOT RECOMMENDED, but
     * quite convenient on occasion.
     *
     * @since 2.4.3-alpha-2
     */
    @Parameter( property = "skipITs" )
    private boolean skipITs;

    /**
     * Base directory where all reports are written to.
     */
    @Parameter( defaultValue = "${project.build.directory}/failsafe-reports" )
    private File reportsDirectory;

    /**
     * Specify this parameter to run individual tests by file name, overriding the <code>includes/excludes</code>
     * parameters. Each pattern you specify here will be used to create an include pattern formatted like
     * <code>**&#47;${test}.java</code>, so you can just type "-Dit.test=MyTest" to run a single test called
     * "foo/MyTest.java".<br/>
     * This parameter overrides the <code>includes/excludes</code> parameters, and the TestNG <code>suiteXmlFiles</code>
     * parameter.
     * <p/>
     * Since 2.7.3 You can execute a limited number of methods in the test with adding #myMethod or #my*ethod. E.g. type
     * "-Dit.test=MyTest#myMethod" <b>supported for junit 4.x and testNg</b>
     * <br/>
     * Since 2.19 a complex syntax is supported in one parameter (JUnit 4, JUnit 4.7+, TestNG):<br/>
     * "-Dit.test=???IT, !Unstable*, pkg&#47;**&#47;Ci*leIT.java, *IT#test*One+testTwo?????, #fast*+slowTest"<br/>
     * "-Dit.test=Basic*, !%regex[.*.Unstable.*], !%regex[.*.MyIT.class#one.*|two.*], %regex[#fast.*|slow.*]"<br/>
     * <br/>
     * The Parameterized JUnit runner <em>describes</em> test methods using an index in brackets, so the non-regex
     * method pattern would become: <em>#testMethod[*]</em>. If using <em>@Parameters(name="{index}: fib({0})={1}")</em>
     * and selecting the index e.g. 5 in pattern, the non-regex method pattern would become <em>#testMethod[5:*]</em>.
     * <br/>
     */
    @Parameter( property = "it.test" )
    private String test;

    /**
     * The summary file to write integration test results to.
     */
    @Parameter( defaultValue = "${project.build.directory}/failsafe-reports/failsafe-summary.xml", required = true )
    private File summaryFile;

    /**
     * Option to print summary of test suites or just print the test cases that have errors.
     */
    @Parameter( property = "failsafe.printSummary", defaultValue = "true" )
    private boolean printSummary;

    /**
     * Selects the formatting for the test report to be generated. Can be set as "brief" or "plain".
     * Only applies to the output format of the output files  (target/surefire-reports/testName.txt)
     */
    @Parameter( property = "failsafe.reportFormat", defaultValue = "brief" )
    private String reportFormat;

    /**
     * Option to generate a file test report or just output the test report to the console.
     */
    @Parameter( property = "failsafe.useFile", defaultValue = "true" )
    private boolean useFile;

    /**
     * Set this to "true" to cause a failure if the none of the tests specified in -Dtest=... are run. Defaults to
     * "true".
     *
     * @since 2.12
     */
    @Parameter( property = "it.failIfNoSpecifiedTests" )
    private Boolean failIfNoSpecifiedTests;

    /**
     * Attach a debugger to the forked JVM. If set to "true", the process will suspend and wait for a debugger to attach
     * on port 5005. If set to some other string, that string will be appended to the argLine, allowing you to configure
     * arbitrary debuggability options (without overwriting the other options specified through the <code>argLine</code>
     * parameter).
     *
     * @since 2.4
     */
    @Parameter( property = "maven.failsafe.debug" )
    private String debugForkedProcess;

    /**
     * Kill the forked test process after a certain number of seconds. If set to 0, wait forever for the process, never
     * timing out.
     *
     * @since 2.4
     */
    @Parameter( property = "failsafe.timeout" )
    private int forkedProcessTimeoutInSeconds;

    /**
     * Stop executing queued parallel JUnit tests after a certain number of seconds.
     * <br/>
     * Example values: "3.5", "4"<br/>
     * <br/>
     * If set to 0, wait forever, never timing out.
     * Makes sense with specified <code>parallel</code> different from "none".
     *
     * @since 2.16
     */
    @Parameter( property = "failsafe.parallel.timeout" )
    private double parallelTestsTimeoutInSeconds;

    /**
     * Stop executing queued parallel JUnit tests
     * and <em>interrupt</em> currently running tests after a certain number of seconds.
     * <br/>
     * Example values: "3.5", "4"<br/>
     * <br/>
     * If set to 0, wait forever, never timing out.
     * Makes sense with specified <code>parallel</code> different from "none".
     *
     * @since 2.16
     */
    @Parameter( property = "failsafe.parallel.forcedTimeout" )
    private double parallelTestsTimeoutForcedInSeconds;

    /**
     * A list of &lt;include> elements specifying the tests (by pattern) that should be included in testing. When not
     * specified and when the <code>test</code> parameter is not specified, the default includes will be <code><br/>
     * &lt;includes><br/>
     * &nbsp;&lt;include>**&#47;IT*.java&lt;/include><br/>
     * &nbsp;&lt;include>**&#47;*IT.java&lt;/include><br/>
     * &nbsp;&lt;include>**&#47;*ITCase.java&lt;/include><br/>
     * &lt;/includes><br/>
     * </code>
     * <p/>
     * Each include item may also contain a comma-separated sublist of items, which will be treated as multiple
     * &nbsp;&lt;include> entries.<br/>
     * Since 2.19 a complex syntax is supported in one parameter (JUnit 4, JUnit 4.7+, TestNG):<br/>
     * &nbsp;&lt;include>%regex[.*[Cat|Dog].*], Basic????, !Unstable*&lt;/include><br/>
     * &nbsp;&lt;include>%regex[.*[Cat|Dog].*], !%regex[pkg.*Slow.*.class], pkg&#47;**&#47;*Fast*.java&lt;/include><br/>
     * <p/>
     * This parameter is ignored if the TestNG <code>suiteXmlFiles</code> parameter is specified.<br/>
     * <br/>
     * <em>Notice that</em> these values are relative to the directory containing generated test classes of the project
     * being tested. This directory is declared by the parameter <code>testClassesDirectory</code> which defaults
     * to the POM property <code>${project.build.testOutputDirectory}</code>, typically <em>src/test/java</em>
     * unless overridden.
     */
    @Parameter
    private List<String> includes;

    /**
     * Option to pass dependencies to the system's classloader instead of using an isolated class loader when forking.
     * Prevents problems with JDKs which implement the service provider lookup mechanism by using the system's
     * classloader.
     *
     * @since 2.3
     */
    @Parameter( property = "failsafe.useSystemClassLoader", defaultValue = "true" )
    private boolean useSystemClassLoader;

    /**
     * By default, Surefire forks your tests using a manifest-only JAR; set this parameter to "false" to force it to
     * launch your tests with a plain old Java classpath. (See the
     * <a href="http://maven.apache.org/plugins/maven-failsafe-plugin/examples/class-loading.html">
     *     http://maven.apache.org/plugins/maven-failsafe-plugin/examples/class-loading.html</a>
     * for a more detailed explanation of manifest-only JARs and their benefits.)
     * <br/>
     * Beware, setting this to "false" may cause your tests to fail on Windows if your classpath is too long.
     *
     * @since 2.4.3
     */
    @Parameter( property = "failsafe.useManifestOnlyJar", defaultValue = "true" )
    private boolean useManifestOnlyJar;

    /**
     * The character encoding scheme to be applied.
     */
    @Parameter( property = "encoding", defaultValue = "${project.reporting.outputEncoding}" )
    private String encoding;

    /**
     * (JUnit 4+ providers)
     * The number of times each failing test will be rerun. If set larger than 0, rerun failing tests immediately after
     * they fail. If a failing test passes in any of those reruns, it will be marked as pass and reported as a "flake".
     * However, all the failing attempts will be recorded.
     */
    @Parameter( property = "failsafe.rerunFailingTestsCount", defaultValue = "0" )
    private int rerunFailingTestsCount;

    /**
     * (TestNG) List of &lt;suiteXmlFile> elements specifying TestNG suite xml file locations. Note that
     * <code>suiteXmlFiles</code> is incompatible with several other parameters of this plugin, like
     * <code>includes/excludes</code>.<br/>
     * This parameter is ignored if the <code>test</code> parameter is specified (allowing you to run a single test
     * instead of an entire suite).
     *
     * @since 2.2
     */
    @Parameter( property = "failsafe.suiteXmlFiles" )
    private File[] suiteXmlFiles;

    /**
     * Defines the order the tests will be run in. Supported values are "alphabetical", "reversealphabetical", "random",
     * "hourly" (alphabetical on even hours, reverse alphabetical on odd hours), "failedfirst", "balanced" and
     * "filesystem".
     * <br/>
     * <br/>
     * Odd/Even for hourly is determined at the time the of scanning the classpath, meaning it could change during a
     * multi-module build.
     * <br/>
     * <br/>
     * Failed first will run tests that failed on previous run first, as well as new tests for this run.
     * <br/>
     * <br/>
     * Balanced is only relevant with parallel=classes, and will try to optimize the run-order of the tests reducing the
     * overall execution time. Initially a statistics file is created and every next test run will reorder classes.
     * <br/>
     * <br/>
     * Note that the statistics are stored in a file named .surefire-XXXXXXXXX beside pom.xml, and should not be checked
     * into version control. The "XXXXX" is the SHA1 checksum of the entire surefire configuration, so different
     * configurations will have different statistics files, meaning if you change any config settings you will re-run
     * once before new statistics data can be established.
     *
     * @since 2.7
     */
    @Parameter( property = "failsafe.runOrder", defaultValue = "filesystem" )
    private String runOrder;

    /**
     * A file containing include patterns. Blank lines, or lines starting with # are ignored. If {@code includes} are
     * also specified, these patterns are appended. Example with path, simple and regex includes:<br/>
     * &#042;&#047;test/*<br/>
     * &#042;&#042;&#047;NotIncludedByDefault.java<br/>
     * %regex[.*Test.*|.*Not.*]<br/>
     */
    @Parameter( property = "failsafe.includesFile" )
    private File includesFile;

    /**
     * A file containing exclude patterns. Blank lines, or lines starting with # are ignored. If {@code excludes} are
     * also specified, these patterns are appended. Example with path, simple and regex excludes:<br/>
     * &#042;&#047;test/*<br/>
     * &#042;&#042;&#047;DontRunTest.*<br/>
     * %regex[.*Test.*|.*Not.*]<br/>
     */
    @Parameter( property = "failsafe.excludesFile" )
    private File excludesFile;

    /**
     * Set to error/failure count in order to skip remaining tests.
     * Due to race conditions in parallel/forked execution this may not be fully guaranteed.<br/>
     * Enable with system property -Dfailsafe.skipAfterFailureCount=1 or any number greater than zero.
     * Defaults to "0".<br/>
     * See the prerequisites and limitations in documentation:<br/>
     * <a href="http://maven.apache.org/plugins/maven-failsafe-plugin/examples/skip-after-failure.html">
     *     http://maven.apache.org/plugins/maven-failsafe-plugin/examples/skip-after-failure.html</a>
     *
     * @since 2.19
     */
    @Parameter( property = "failsafe.skipAfterFailureCount", defaultValue = "0" )
    private int skipAfterFailureCount;

    /**
     * After the plugin process is shutdown by sending SIGTERM signal (CTRL+C), SHUTDOWN command is received by every
     * forked JVM. By default (shutdown=testset) forked JVM would not continue with new test which means that
     * the current test may still continue to run.<br/>
     * The parameter can be configured with other two values "exit" and "kill".<br/>
     * Using "exit" forked JVM executes System.exit(1) after the plugin process has received SIGTERM signal.<br/>
     * Using "kill" the JVM executes Runtime.halt(1) and kills itself.
     *
     * @since 2.19
     */
    @Parameter( property = "failsafe.shutdown", defaultValue = "testset" )
    private String shutdown;

    protected int getRerunFailingTestsCount()
    {
        return rerunFailingTestsCount;
    }

    @SuppressWarnings( "unchecked" )
    protected void handleSummary( RunResult summary, Exception firstForkException )
        throws MojoExecutionException, MojoFailureException
    {
        File summaryFile = getSummaryFile();
        if ( !summaryFile.getParentFile().isDirectory() )
        {
            //noinspection ResultOfMethodCallIgnored
            summaryFile.getParentFile().mkdirs();
        }

        try
        {
            Object token = getPluginContext().get( FAILSAFE_IN_PROGRESS_CONTEXT_KEY );
            summary.writeSummary( summaryFile, token != null, getEncodingOrDefault() );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }

        getPluginContext().put( FAILSAFE_IN_PROGRESS_CONTEXT_KEY, FAILSAFE_IN_PROGRESS_CONTEXT_KEY );
    }

    private String getEncodingOrDefault()
    {
        if ( StringUtils.isEmpty( encoding ) )
        {
            getLog().warn( "File encoding has not been set, using platform encoding " + ReaderFactory.FILE_ENCODING
                           + ", i.e. build is platform dependent! The file encoding for reports output files "
                               + "should be provided by the POM property ${project.reporting.outputEncoding}." );
            return ReaderFactory.FILE_ENCODING;
        }
        else
        {
            return encoding;
        }
    }

    @SuppressWarnings( "deprecation" )
    protected boolean isSkipExecution()
    {
        return isSkip() || isSkipTests() || isSkipITs() || isSkipExec();
    }

    protected String getPluginName()
    {
        return "failsafe";
    }

    protected String[] getDefaultIncludes()
    {
        return new String[]{ "**/IT*.java", "**/*IT.java", "**/*ITCase.java" };
    }

    @Override
    protected String getReportSchemaLocation()
    {
        return "https://maven.apache.org/surefire/maven-failsafe-plugin/xsd/failsafe-test-report.xsd";
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

    @SuppressWarnings( "deprecation" )
    @Deprecated
    public boolean isSkipExec()
    {
        return skipExec;
    }

    @SuppressWarnings( "deprecation" )
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

    /**
     * @return Output directory, or artifact file if artifact type is "jar". If not forking the JVM, parameter
     * {@link #useSystemClassLoader} is ignored and the {@link org.apache.maven.surefire.booter.IsolatedClassLoader} is
     * used instead. See the resolution of {@link #getClassLoaderConfiguration() ClassLoaderConfiguration}.
     */
    public File getClassesDirectory()
    {
        Artifact artifact = getProject().getArtifact();
        File artifactFile = artifact.getFile();

        boolean useArtifactFile = artifactFile != null && artifactFile.isFile()
            && artifactFile.getName().toLowerCase().endsWith( ".jar" );

        return useArtifactFile ? artifactFile : classesDirectory;
    }

    public void setClassesDirectory( File classesDirectory )
    {
        this.classesDirectory = classesDirectory;
    }

    public File getReportsDirectory()
    {
        return reportsDirectory;
    }

    public void setReportsDirectory( File reportsDirectory )
    {
        this.reportsDirectory = reportsDirectory;
    }

    public String getTest()
    {
        return test;
    }

    public void setTest( String test )
    {
        this.test = test;
    }

    public File getSummaryFile()
    {
        return summaryFile;
    }

    public void setSummaryFile( File summaryFile )
    {
        this.summaryFile = summaryFile;
    }

    public boolean isPrintSummary()
    {
        return printSummary;
    }

    public void setPrintSummary( boolean printSummary )
    {
        this.printSummary = printSummary;
    }

    public String getReportFormat()
    {
        return reportFormat;
    }

    public void setReportFormat( String reportFormat )
    {
        this.reportFormat = reportFormat;
    }

    public boolean isUseFile()
    {
        return useFile;
    }

    public void setUseFile( boolean useFile )
    {
        this.useFile = useFile;
    }

    public String getDebugForkedProcess()
    {
        return debugForkedProcess;
    }

    public void setDebugForkedProcess( String debugForkedProcess )
    {
        this.debugForkedProcess = debugForkedProcess;
    }

    public int getForkedProcessTimeoutInSeconds()
    {
        return forkedProcessTimeoutInSeconds;
    }

    public void setForkedProcessTimeoutInSeconds( int forkedProcessTimeoutInSeconds )
    {
        this.forkedProcessTimeoutInSeconds = forkedProcessTimeoutInSeconds;
    }

    public double getParallelTestsTimeoutInSeconds()
    {
        return parallelTestsTimeoutInSeconds;
    }

    public void setParallelTestsTimeoutInSeconds( double parallelTestsTimeoutInSeconds )
    {
        this.parallelTestsTimeoutInSeconds = parallelTestsTimeoutInSeconds;
    }

    public double getParallelTestsTimeoutForcedInSeconds()
    {
        return parallelTestsTimeoutForcedInSeconds;
    }

    public void setParallelTestsTimeoutForcedInSeconds( double parallelTestsTimeoutForcedInSeconds )
    {
        this.parallelTestsTimeoutForcedInSeconds = parallelTestsTimeoutForcedInSeconds;
    }

    public boolean isUseSystemClassLoader()
    {
        return useSystemClassLoader;
    }

    public void setUseSystemClassLoader( boolean useSystemClassLoader )
    {
        this.useSystemClassLoader = useSystemClassLoader;
    }

    public boolean isUseManifestOnlyJar()
    {
        return useManifestOnlyJar;
    }

    public void setUseManifestOnlyJar( boolean useManifestOnlyJar )
    {
        this.useManifestOnlyJar = useManifestOnlyJar;
    }

    // the following will be refactored out once the common code is all in one place

    public boolean isTestFailureIgnore()
    {
        return true; // ignore
    }

    public void setTestFailureIgnore( boolean testFailureIgnore )
    {
        // ignore
    }

    protected void addPluginSpecificChecksumItems( ChecksumCalculator checksum )
    {
        checksum.add( skipITs );
        checksum.add( summaryFile );
    }

    public Boolean getFailIfNoSpecifiedTests()
    {
        return failIfNoSpecifiedTests;
    }

    public void setFailIfNoSpecifiedTests( boolean failIfNoSpecifiedTests )
    {
        this.failIfNoSpecifiedTests = failIfNoSpecifiedTests;
    }

    public int getSkipAfterFailureCount()
    {
        return skipAfterFailureCount;
    }

    public String getShutdown()
    {
        return shutdown;
    }

    @Override
    public List<String> getIncludes()
    {
        return includes;
    }

    @Override
    public void setIncludes( List<String> includes )
    {
        this.includes = includes;
    }

    public File[] getSuiteXmlFiles()
    {
        return suiteXmlFiles.clone();
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setSuiteXmlFiles( File[] suiteXmlFiles )
    {
        this.suiteXmlFiles = suiteXmlFiles.clone();
    }

    public String getRunOrder()
    {
        return runOrder;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setRunOrder( String runOrder )
    {
        this.runOrder = runOrder;
    }

    @Override
    public File getIncludesFile()
    {
        return includesFile;
    }

    @Override
    public File getExcludesFile()
    {
        return excludesFile;
    }

    @Override
    protected final List<File> suiteXmlFiles()
    {
        return hasSuiteXmlFiles() ? Arrays.asList( suiteXmlFiles ) : Collections.<File>emptyList();
    }

    @Override
    protected final boolean hasSuiteXmlFiles()
    {
        return suiteXmlFiles != null && suiteXmlFiles.length != 0;
    }
}
