package org.apache.maven.plugin.surefire;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.surefire.booterclient.ChecksumCalculator;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.surefire.extensions.ForkNodeFactory;
import org.apache.maven.surefire.api.suite.RunResult;

import static org.apache.maven.plugin.surefire.SurefireHelper.reportExecution;

/**
 * Run tests using Surefire.
 *
 * @author Jason van Zyl
 */
@Mojo( name = "test", defaultPhase = LifecyclePhase.TEST, threadSafe = true,
       requiresDependencyResolution = ResolutionScope.TEST )
public class SurefirePlugin
    extends AbstractSurefireMojo
    implements SurefireReportParameters
{

    /**
     * The directory containing generated classes of the project being tested. This will be included after the test
     * classes in the test classpath.
     */
    @Parameter( defaultValue = "${project.build.outputDirectory}" )
    private File classesDirectory;

    /**
     * Set this to "true" to ignore a failure during testing. Its use is NOT RECOMMENDED, but quite convenient on
     * occasion.
     */
    @Parameter( property = "maven.test.failure.ignore", defaultValue = "false" )
    private boolean testFailureIgnore;

    /**
     * Base directory where all reports are written to.
     */
    @Parameter( defaultValue = "${project.build.directory}/surefire-reports" )
    private File reportsDirectory;

    @SuppressWarnings( "checkstyle:linelength" )
    /**
     * Specify this parameter to run individual tests by file name, overriding the parameter {@code includes} and
     * {@code excludes}. Each pattern you specify here will be used to create an include pattern formatted like
     * <code>**{@literal /}${test}.java</code>, so you can just type {@code -Dtest=MyTest} to run a single test called
     * "foo/MyTest.java". The test patterns prefixed with a <em>!</em> will be excluded.
     * <br>
     * This parameter overrides the parameter {@code includes}, {@code excludes}, and the TestNG parameter
     * {@code suiteXmlFiles}.
     * <br>
     * Since 2.7.3, you can execute a limited number of methods in the test by adding <i>#myMethod</i> or
     * <i>#my*ethod</i>. For example, {@code -Dtest=MyTest#myMethod}. This is supported for junit 4.x and TestNg.<br>
     * <br>
     * Since 2.19 a complex syntax is supported in one parameter (JUnit 4, JUnit 4.7+, TestNG):
     * <pre><code>"-Dtest=???Test, !Unstable*, pkg{@literal /}**{@literal /}Ci*leTest.java, *Test#test*One+testTwo?????, #fast*+slowTest"</code></pre>
     * or e.g.
     * <pre><code>"-Dtest=Basic*, !%regex[.*.Unstable.*], !%regex[.*.MyTest.class#one.*|two.*], %regex[#fast.*|slow.*]"</code></pre>
     * <br>
     * The Parameterized JUnit runner {@code describes} test methods using an index in brackets, so the non-regex
     * method pattern would become: {@code #testMethod[*]}.
     * If using <code>@Parameters(name="{index}: fib({0})={1}")</code> and selecting the index e.g. 5 in pattern, the
     * non-regex method pattern would become {@code #testMethod[5:*]}.
     */
    @Parameter( property = "test" )
    private String test;

    /**
     * Option to print summary of test suites or just print the test cases that have errors.
     */
    @Parameter( property = "surefire.printSummary", defaultValue = "true" )
    private boolean printSummary;

    /**
     * Selects the formatting for the test report to be generated. Can be set as "brief" or "plain".
     * Only applies to the output format of the output files  (target/surefire-reports/testName.txt)
     */
    @Parameter( property = "surefire.reportFormat", defaultValue = "brief" )
    private String reportFormat;

    /**
     * Option to generate a file test report or just output the test report to the console.
     */
    @Parameter( property = "surefire.useFile", defaultValue = "true" )
    private boolean useFile;

    /**
     * Set this to "true" to cause a failure if none of the tests specified in -Dtest=... are run. Defaults to
     * "true".
     *
     * @since 2.12
     */
    @Parameter( property = "surefire.failIfNoSpecifiedTests", defaultValue = "true" )
    private boolean failIfNoSpecifiedTests;

    /**
     * Attach a debugger to the forked JVM. If set to "true", the process will suspend and wait for a debugger to attach
     * on port 5005. If set to some other string, that string will be appended to the argLine, allowing you to configure
     * arbitrary debuggability options (without overwriting the other options specified through the {@code argLine}
     * parameter).
     *
     * @since 2.4
     */
    @Parameter( property = "maven.surefire.debug" )
    private String debugForkedProcess;

    /**
     * Kill the forked test process after a certain number of seconds. If set to 0, wait forever for the process, never
     * timing out.
     *
     * @since 2.4
     */
    @Parameter( property = "surefire.timeout" )
    private int forkedProcessTimeoutInSeconds;

    /**
     * Forked process is normally terminated without any significant delay after given tests have completed.
     * If the particular tests started non-daemon Thread(s), the process hangs instead of been properly terminated
     * by {@code System.exit()}. Use this parameter in order to determine the timeout of terminating the process.
     * <a href="http://maven.apache.org/surefire/maven-surefire-plugin/examples/shutdown.html">see the documentation:
     * http://maven.apache.org/surefire/maven-surefire-plugin/examples/shutdown.html</a>
     * Turns to default fallback value of 30 seconds if negative integer.
     *
     * @since 2.20
     */
    @Parameter( property = "surefire.exitTimeout", defaultValue = "30" )
    private int forkedProcessExitTimeoutInSeconds;

    /**
     * Stop executing queued parallel JUnit tests after a certain number of seconds.
     * <br>
     * Example values: "3.5", "4"<br>
     * <br>
     * If set to 0, wait forever, never timing out.
     * Makes sense with specified {@code parallel} different from "none".
     *
     * @since 2.16
     */
    @Parameter( property = "surefire.parallel.timeout" )
    private double parallelTestsTimeoutInSeconds;

    /**
     * Stop executing queued parallel JUnit tests
     * and {@code interrupt} currently running tests after a certain number of seconds.
     * <br>
     * Example values: "3.5", "4"<br>
     * <br>
     * If set to 0, wait forever, never timing out.
     * Makes sense with specified {@code parallel} different from "none".
     *
     * @since 2.16
     */
    @Parameter( property = "surefire.parallel.forcedTimeout" )
    private double parallelTestsTimeoutForcedInSeconds;

    @SuppressWarnings( "checkstyle:linelength" )
    /**
     * A list of {@literal <include>} elements specifying the tests (by pattern) that should be included in testing.
     * When not specified and when the {@code test} parameter is not specified, the default includes will be
     * <pre><code>
     * {@literal <includes>}
     *     {@literal <include>}**{@literal /}Test*.java{@literal </include>}
     *     {@literal <include>}**{@literal /}*Test.java{@literal </include>}
     *     {@literal <include>}**{@literal /}*Tests.java{@literal </include>}
     *     {@literal <include>}**{@literal /}*TestCase.java{@literal </include>}
     * {@literal </includes>}
     * </code></pre>
     * Each include item may also contain a comma-separated sub-list of items, which will be treated as multiple
     * {@literal <include>} entries.<br>
     * Since 2.19 a complex syntax is supported in one parameter (JUnit 4, JUnit 4.7+, TestNG):
     * <pre><code>
     * {@literal <include>}%regex[.*[Cat|Dog].*], Basic????, !Unstable*{@literal </include>}
     * {@literal <include>}%regex[.*[Cat|Dog].*], !%regex[pkg.*Slow.*.class], pkg{@literal /}**{@literal /}*Fast*.java{@literal </include>}
     * </code></pre>
     * <br>
     * This parameter is ignored if the TestNG {@code suiteXmlFiles} parameter is specified.<br>
     * <br>
     * <b>Notice that</b> these values are relative to the directory containing generated test classes of the project
     * being tested. This directory is declared by the parameter {@code testClassesDirectory} which defaults
     * to the POM property {@code ${project.build.testOutputDirectory}}, typically
     * <code>{@literal src/test/java}</code> unless overridden.
     */
    @Parameter( property = "surefire.includes" )
    // TODO use regex for fully qualified class names in 3.0 and change the filtering abilities
    private List<String> includes;

    /**
     * A list of {@literal <exclude>} elements specifying the tests (by pattern) that should be excluded in testing.
     * When not specified and when the {@code test} parameter is not specified, the default excludes will be <br>
     * <pre><code>
     * {@literal <excludes>}
     *     {@literal <exclude>}**{@literal /}*$*{@literal </exclude>}
     * {@literal </excludes>}
     * </code></pre>
     * (which excludes all inner classes).
     * <br>
     * This parameter is ignored if the TestNG {@code suiteXmlFiles} parameter is specified.
     * <br>
     * Each exclude item may also contain a comma-separated sub-list of items, which will be treated as multiple
     * {@literal <exclude>} entries.<br>
     * Since 2.19 a complex syntax is supported in one parameter (JUnit 4, JUnit 4.7+, TestNG):
     * <pre><code>
     * {@literal <exclude>}%regex[pkg.*Slow.*.class], Unstable*{@literal </exclude>}
     * </code></pre>
     * <br>
     * <b>Notice that</b> these values are relative to the directory containing generated test classes of the project
     * being tested. This directory is declared by the parameter {@code testClassesDirectory} which defaults
     * to the POM property <code>${project.build.testOutputDirectory}</code>, typically
     * <code>{@literal src/test/java}</code> unless overridden.
     */
    @Parameter( property = "surefire.excludes" )
    // TODO use regex for fully qualified class names in 3.0 and change the filtering abilities
    private List<String> excludes;

    /**
     * Option to pass dependencies to the system's classloader instead of using an isolated class loader when forking.
     * Prevents problems with JDKs which implement the service provider lookup mechanism by using the system's
     * ClassLoader.
     *
     * @since 2.3
     */
    @Parameter( property = "surefire.useSystemClassLoader", defaultValue = "true" )
    private boolean useSystemClassLoader;

    /**
     * By default, Surefire forks your tests using a manifest-only JAR; set this parameter to "false" to force it to
     * launch your tests with a plain old Java classpath. (See the
     * <a href="http://maven.apache.org/plugins/maven-surefire-plugin/examples/class-loading.html">
     *     http://maven.apache.org/plugins/maven-surefire-plugin/examples/class-loading.html</a>
     * for a more detailed explanation of manifest-only JARs and their benefits.)
     * <br>
     * Beware, setting this to "false" may cause your tests to fail on Windows if your classpath is too long.
     *
     * @since 2.4.3
     */
    @Parameter( property = "surefire.useManifestOnlyJar", defaultValue = "true" )
    private boolean useManifestOnlyJar;

    /**
     * The character encoding scheme to be applied while generating test report
     * files (see target/surefire-reports/yourTestName.txt).
     * The report output files (*-out.txt) are encoded in UTF-8 if not set otherwise.
     *
     * @since 3.0.0-M1
     */
    @Parameter( property = "surefire.encoding", defaultValue = "${project.reporting.outputEncoding}" )
    private String encoding;

    /**
     * (JUnit 4+ providers and JUnit 5+ providers since 3.0.0-M4)
     * The number of times each failing test will be rerun. If set larger than 0, rerun failing tests immediately after
     * they fail. If a failing test passes in any of those reruns, it will be marked as pass and reported as a "flake".
     * However, all the failing attempts will be recorded.
     */
    @Parameter( property = "surefire.rerunFailingTestsCount", defaultValue = "0" )
    private int rerunFailingTestsCount;

    /**
     * Set this to a value greater than 0 to fail the whole test set if the cumulative number of flakes reaches
     * this threshold. Set to 0 to allow an unlimited number of flakes.
     *
     * @since 3.0.0-M6
     */
    @Parameter( property = "surefire.failOnFlakeCount", defaultValue = "0" )
    private int failOnFlakeCount;

    /**
     * (TestNG) List of &lt;suiteXmlFile&gt; elements specifying TestNG suite xml file locations. Note that
     * {@code suiteXmlFiles} is incompatible with several other parameters of this plugin, like
     * {@code includes} and {@code excludes}.<br>
     * This parameter is ignored if the {@code test} parameter is specified (allowing you to run a single test
     * instead of an entire suite).
     *
     * @since 2.2
     */
    @Parameter( property = "surefire.suiteXmlFiles" )
    private File[] suiteXmlFiles;

    /**
     * Defines the order the tests will be run in. Supported values are {@code alphabetical},
     * {@code reversealphabetical}, {@code random}, {@code hourly} (alphabetical on even hours, reverse alphabetical
     * on odd hours), {@code failedfirst}, {@code balanced} and {@code filesystem}.
     * <br>
     * <br>
     * Odd/Even for hourly is determined at the time the of scanning the classpath, meaning it could change during a
     * multi-module build.
     * <br>
     * <br>
     * Failed first will run tests that failed on previous run first, as well as new tests for this run.
     * <br>
     * <br>
     * Balanced is only relevant with parallel=classes, and will try to optimize the run-order of the tests reducing the
     * overall execution time. Initially a statistics file is created and every next test run will reorder classes.
     * <br>
     * <br>
     * Note that the statistics are stored in a file named <b>.surefire-XXXXXXXXX</b> beside <i>pom.xml</i> and
     * should not be checked into version control. The "XXXXX" is the SHA1 checksum of the entire surefire
     * configuration, so different configurations will have different statistics files, meaning if you change any
     * configuration settings you will re-run once before new statistics data can be established.
     *
     * @since 2.7
     */
    @Parameter( property = "surefire.runOrder", defaultValue = "filesystem" )
    private String runOrder;

    /**
     * Sets the random seed that will be used to order the tests if {@code surefire.runOrder} is set to {@code random}.
     * <br>
     * <br>
     * If no seeds are set and {@code surefire.runOrder} is set to {@code random}, then the seed used will be
     * outputted (search for "To reproduce ordering use flag -Dsurefire.runOrder.random.seed").
     * <br>
     * <br>
     * To deterministically reproduce any random test order that was run before, simply set the seed to
     * be the same value.
     *
     * @since 3.0.0-M6
     */
    @Parameter( property = "surefire.runOrder.random.seed" )
    private Long runOrderRandomSeed;

    /**
     * A file containing include patterns. Blank lines, or lines starting with # are ignored. If {@code includes} are
     * also specified, these patterns are appended. Example with path, simple and regex includes:
     * <pre><code>
     * *{@literal /}test{@literal /}*
     * **{@literal /}NotIncludedByDefault.java
     * %regex[.*Test.*|.*Not.*]
     * </code></pre>
     * <br>
     * Since 3.0.0-M6, method filtering support is provided in the inclusions file as well, example:
     * <pre><code>
     * pkg.SomeTest#testMethod
     * </code></pre>
     *
     * @since 2.13
     */
    @Parameter( property = "surefire.includesFile" )
    private File includesFile;

    /**
     * A file containing exclude patterns. Blank lines, or lines starting with # are ignored. If {@code excludes} are
     * also specified, these patterns are appended. Example with path, simple and regex excludes:<br>
     * <pre><code>
     * *{@literal /}test{@literal /}*
     * **{@literal /}DontRunTest.*
     * %regex[.*Test.*|.*Not.*]
     * </code></pre>
     *
     * Since 3.0.0-M6, method filtering support is provided in the exclusions file as well, example:
     * <pre><code>
     * pkg.SomeTest#testMethod
     * </code></pre>
     *
     * @since 2.13
     */
    @Parameter( property = "surefire.excludesFile" )
    private File excludesFile;

    /**
     * Set to error/failure count in order to skip remaining tests.
     * Due to race conditions in parallel/forked execution this may not be fully guaranteed.<br>
     * Enable with system property {@code -Dsurefire.skipAfterFailureCount=1} or any number greater than zero.
     * Defaults to "0".<br>
     * See the prerequisites and limitations in documentation:<br>
     * <a href="http://maven.apache.org/plugins/maven-surefire-plugin/examples/skip-after-failure.html">
     *     http://maven.apache.org/plugins/maven-surefire-plugin/examples/skip-after-failure.html</a>
     *
     * @since 2.19
     */
    @Parameter( property = "surefire.skipAfterFailureCount", defaultValue = "0" )
    private int skipAfterFailureCount;

    /**
     * After the plugin process is shutdown by sending <i>SIGTERM signal (CTRL+C)</i>, <i>SHUTDOWN command</i> is
     * received by every forked JVM.
     * <br>
     * The value is set to ({@code shutdown=exit}) by default (changed in version 3.0.0-M4).
     * <br>
     * The parameter can be configured with other two values {@code testset} and {@code kill}.
     * <br>
     * With({@code shutdown=testset}) the test set may still continue to run in forked JVM.
     * <br>
     * Using {@code exit} forked JVM executes {@code System.exit(1)} after the plugin process has received
     * <i>SIGTERM signal</i>.
     * <br>
     * Using {@code kill} the JVM executes {@code Runtime.halt(1)} and kills itself.
     *
     * @since 2.19
     */
    @Parameter( property = "surefire.shutdown", defaultValue = "exit" )
    private String shutdown;

    /**
     * When {@code true}, uses the modulepath when executing with JDK 9+ and <i>module-info.java</i> is
     * present. When {@code false}, always uses the classpath.
     * <br>
     * Defaults to {@code true}.
     *
     * @since 3.0.0-M2
     */
    @Parameter( property = "surefire.useModulePath", defaultValue = "true" )
    private boolean useModulePath;

    /**
     * This parameter configures the forked node. Currently, you can select the communication protocol, i.e. process
     * pipes or TCP/IP sockets.
     * The plugin uses process pipes by default which will be turned to TCP/IP in the version 3.0.0.
     * Alternatively, you can implement your own factory and SPI.
     * <br>
     * See the documentation for more details:<br>
     * <a href="https://maven.apache.org/plugins/maven-surefire-plugin/examples/process-communication.html">
     *     https://maven.apache.org/plugins/maven-surefire-plugin/examples/process-communication.html</a>
     *
     * @since 3.0.0-M5
     */
    @Parameter( property = "surefire.forkNode" )
    private ForkNodeFactory forkNode;

    /**
     * You can selectively exclude individual environment variables by enumerating their keys.
     * <br>
     * The environment is a system-dependent mapping from keys to values which is inherited from the Maven process
     * to the forked Surefire processes. The keys must literally (case sensitive) match in order to exclude
     * their environment variable.
     * <br>
     * Example to exclude three environment variables:
     * <br>
     * <i>mvn test -Dsurefire.excludedEnvironmentVariables=ACME1,ACME2,ACME3</i>
     *
     * @since 3.0.0-M4
     */
    @Parameter( property = "surefire.excludedEnvironmentVariables" )
    private String[] excludedEnvironmentVariables;

    /**
     * Since 3.0.0-M4 the process checkers are disabled.
     * You can enable them namely by setting {@code ping} and {@code native} or {@code all} in this parameter.
     * <br>
     * The checker is useful in situations when you kill the build on a CI system and you want the Surefire forked JVM
     * to kill the tests asap and free all handlers on the file system been previously used by the JVM and by the tests.
     *
     * <br>
     *
     * The {@code ping} should be safely used together with ZGC or Shenandoah Garbage Collector.
     * Due to the {@code ping} relies on timing of the PING (triggered every 30 seconds), slow GCs may pause
     * the timers and pretend that the parent process of the forked JVM does not exist.
     *
     * <br>
     *
     * The {@code native} is very fast checker.
     * It is useful mechanism on Unix based systems, Linux distributions and Alpine/BusyBox Linux.
     * See the JIRA <a href="https://issues.apache.org/jira/browse/SUREFIRE-1631">SUREFIRE-1631</a> for Windows issues.
     *
     * <br>
     *
     * Another useful configuration parameter is {@code forkedProcessTimeoutInSeconds}.
     * <br>
     * See the Frequently Asked Questions page with more details:<br>
     * <a href="http://maven.apache.org/surefire/maven-surefire-plugin/faq.html#kill-jvm">
     *     http://maven.apache.org/surefire/maven-surefire-plugin/faq.html#kill-jvm</a>
     * <br>
     * <a href="http://maven.apache.org/surefire/maven-failsafe-plugin/faq.html#kill-jvm">
     *     http://maven.apache.org/surefire/maven-failsafe-plugin/faq.html#kill-jvm</a>
     *
     * <br>
     *
     * Example of use:
     * <br>
     * <i>mvn test -Dsurefire.enableProcessChecker=all</i>
     *
     * @since 3.0.0-M4
     */
    @Parameter( property = "surefire.enableProcessChecker" )
    private String enableProcessChecker;

    @Parameter( property = "surefire.systemPropertiesFile" )
    private File systemPropertiesFile;

    /**
     * Provide the ID/s of an JUnit engine to be included in the test run.
     *
     * @since 3.0.0-M6
     */
    @Parameter( property = "surefire.includeJUnit5Engines" )
    private String[] includeJUnit5Engines;

    /**
     * Provide the ID/s of an JUnit engine to be excluded in the test run.
     *
     * @since 3.0.0-M6
     */
    @Parameter( property = "surefire.excludeJUnit5Engines" )
    private String[] excludeJUnit5Engines;

    @Override
    protected int getRerunFailingTestsCount()
    {
        return rerunFailingTestsCount;
    }

    @Override
    public int getFailOnFlakeCount()
    {
        return failOnFlakeCount;
    }

    @Override
    public void setFailOnFlakeCount( int failOnFlakeCount )
    {
        this.failOnFlakeCount = failOnFlakeCount;
    }

    @Override
    protected void handleSummary( RunResult summary, Exception firstForkException )
        throws MojoExecutionException, MojoFailureException
    {
        reportExecution( this, summary, getConsoleLogger(), firstForkException );
    }

    @Override
    protected boolean isSkipExecution()
    {
        return isSkip() || isSkipTests() || isSkipExec();
    }

    @Override
    protected String getPluginName()
    {
        return "surefire";
    }

    @Override
    protected String[] getDefaultIncludes()
    {
        return new String[]{ "**/Test*.java", "**/*Test.java", "**/*Tests.java", "**/*TestCase.java" };
    }

    @Override
    protected String getReportSchemaLocation()
    {
        return "https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report-3.0.xsd";
    }


    public File getSystemPropertiesFile()
    {
        return systemPropertiesFile;
    }


    public void setSystemPropertiesFile( File systemPropertiesFile )
    {
        this.systemPropertiesFile = systemPropertiesFile;
    }


    // now for the implementation of the field accessors

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
    public File getMainBuildPath()
    {
        return classesDirectory;
    }

    @Override
    public void setMainBuildPath( File mainBuildPath )
    {
        classesDirectory = mainBuildPath;
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
    public String getTest()
    {
        return test;
    }

    @Override
    public boolean isUseSystemClassLoader()
    {
        return useSystemClassLoader;
    }

    @Override
    public void setUseSystemClassLoader( boolean useSystemClassLoader )
    {
        this.useSystemClassLoader = useSystemClassLoader;
    }

    @Override
    public boolean isUseManifestOnlyJar()
    {
        return useManifestOnlyJar;
    }

    @Override
    public void setUseManifestOnlyJar( boolean useManifestOnlyJar )
    {
        this.useManifestOnlyJar = useManifestOnlyJar;
    }

    @Override
    public String getEncoding()
    {
        return encoding;
    }

    @Override
    public void setEncoding( String encoding )
    {
        this.encoding = encoding;
    }

    @Override
    public boolean getFailIfNoSpecifiedTests()
    {
        return failIfNoSpecifiedTests;
    }

    @Override
    public void setFailIfNoSpecifiedTests( boolean failIfNoSpecifiedTests )
    {
        this.failIfNoSpecifiedTests = failIfNoSpecifiedTests;
    }

    @Override
    public int getSkipAfterFailureCount()
    {
        return skipAfterFailureCount;
    }

    @Override
    public String getShutdown()
    {
        return shutdown;
    }

    @Override
    public boolean isPrintSummary()
    {
        return printSummary;
    }

    @Override
    public void setPrintSummary( boolean printSummary )
    {
        this.printSummary = printSummary;
    }

    @Override
    public String getReportFormat()
    {
        return reportFormat;
    }

    @Override
    public void setReportFormat( String reportFormat )
    {
        this.reportFormat = reportFormat;
    }

    @Override
    public boolean isUseFile()
    {
        return useFile;
    }

    @Override
    public void setUseFile( boolean useFile )
    {
        this.useFile = useFile;
    }

    @Override
    public String getDebugForkedProcess()
    {
        return debugForkedProcess;
    }

    @Override
    public void setDebugForkedProcess( String debugForkedProcess )
    {
        this.debugForkedProcess = debugForkedProcess;
    }

    @Override
    public int getForkedProcessTimeoutInSeconds()
    {
        return forkedProcessTimeoutInSeconds;
    }

    @Override
    public void setForkedProcessTimeoutInSeconds( int forkedProcessTimeoutInSeconds )
    {
        this.forkedProcessTimeoutInSeconds = forkedProcessTimeoutInSeconds;
    }

    @Override
    public int getForkedProcessExitTimeoutInSeconds()
    {
        return forkedProcessExitTimeoutInSeconds;
    }

    @Override
    public void setForkedProcessExitTimeoutInSeconds( int forkedProcessExitTimeoutInSeconds )
    {
        this.forkedProcessExitTimeoutInSeconds = forkedProcessExitTimeoutInSeconds;
    }

    @Override
    public double getParallelTestsTimeoutInSeconds()
    {
        return parallelTestsTimeoutInSeconds;
    }

    @Override
    public void setParallelTestsTimeoutInSeconds( double parallelTestsTimeoutInSeconds )
    {
        this.parallelTestsTimeoutInSeconds = parallelTestsTimeoutInSeconds;
    }

    @Override
    public double getParallelTestsTimeoutForcedInSeconds()
    {
        return parallelTestsTimeoutForcedInSeconds;
    }

    @Override
    public void setParallelTestsTimeoutForcedInSeconds( double parallelTestsTimeoutForcedInSeconds )
    {
        this.parallelTestsTimeoutForcedInSeconds = parallelTestsTimeoutForcedInSeconds;
    }

    @Override
    public void setTest( String test )
    {
        this.test = test;
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

    @Override
    public List<String> getExcludes()
    {
        return excludes;
    }

    @Override
    public void setExcludes( List<String> excludes )
    {
        this.excludes = excludes;
    }

    @Override
    public File[] getSuiteXmlFiles()
    {
        return suiteXmlFiles.clone();
    }

    @Override
    @SuppressWarnings( "UnusedDeclaration" )
    public void setSuiteXmlFiles( File[] suiteXmlFiles )
    {
        this.suiteXmlFiles = suiteXmlFiles.clone();
    }

    @Override
    public String getRunOrder()
    {
        return runOrder;
    }

    @Override
    @SuppressWarnings( "UnusedDeclaration" )
    public void setRunOrder( String runOrder )
    {
        this.runOrder = runOrder;
    }

    @Override
    public Long getRunOrderRandomSeed()
    {
        return runOrderRandomSeed;
    }

    @Override
    public void setRunOrderRandomSeed( Long runOrderRandomSeed )
    {
        this.runOrderRandomSeed = runOrderRandomSeed;
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
    protected boolean useModulePath()
    {
        return useModulePath;
    }

    @Override
    protected void setUseModulePath( boolean useModulePath )
    {
        this.useModulePath = useModulePath;
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

    @Override
    protected final String[] getExcludedEnvironmentVariables()
    {
        return excludedEnvironmentVariables == null ? new String[0] : excludedEnvironmentVariables;
    }

    void setExcludedEnvironmentVariables( String[] excludedEnvironmentVariables )
    {
        this.excludedEnvironmentVariables = excludedEnvironmentVariables;
    }

    @Override
    protected final String getEnableProcessChecker()
    {
        return enableProcessChecker;
    }

    @Override
    protected final ForkNodeFactory getForkNode()
    {
        return forkNode;
    }

    @Override
    protected void warnIfIllegalFailOnFlakeCount() throws MojoFailureException
    {
        if ( failOnFlakeCount < 0 )
        {
            throw new MojoFailureException( "Parameter \"failOnFlakeCount\" should not be negative." );
        }
        if ( failOnFlakeCount > 0 && rerunFailingTestsCount < 1 )
        {
            throw new MojoFailureException( "\"failOnFlakeCount\" requires rerunFailingTestsCount to be at least 1." );
        }
    }

    @Override
    protected void addPluginSpecificChecksumItems( ChecksumCalculator checksum )
    {
        checksum.add( skipAfterFailureCount );
    }

    public String[] getIncludeJUnit5Engines()
    {
        return includeJUnit5Engines;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setIncludeJUnit5Engines( String[] includeJUnit5Engines )
    {
        this.includeJUnit5Engines = includeJUnit5Engines;
    }

    public String[] getExcludeJUnit5Engines()
    {
        return excludeJUnit5Engines;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setExcludeJUnit5Engines( String[] excludeJUnit5Engines )
    {
        this.excludeJUnit5Engines = excludeJUnit5Engines;
    }
}
