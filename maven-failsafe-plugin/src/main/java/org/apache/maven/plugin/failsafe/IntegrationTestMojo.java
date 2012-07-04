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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.surefire.AbstractSurefireMojo;
import org.apache.maven.plugin.surefire.Summary;
import org.apache.maven.plugin.surefire.booterclient.ChecksumCalculator;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.surefire.booter.ProviderConfiguration;
import org.apache.maven.surefire.failsafe.model.FailsafeSummary;
import org.apache.maven.surefire.failsafe.model.io.xpp3.FailsafeSummaryXpp3Reader;
import org.apache.maven.surefire.failsafe.model.io.xpp3.FailsafeSummaryXpp3Writer;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.codehaus.plexus.util.IOUtil.close;

/**
 * Run integration tests using Surefire.
 *
 * @author Jason van Zyl
 * @author Stephen Connolly
 * @noinspection JavaDoc, UnusedDeclaration
 */
@Mojo( name = "integration-test", requiresProject = true, requiresDependencyResolution = ResolutionScope.TEST,
       defaultPhase = LifecyclePhase.INTEGRATION_TEST, threadSafe = true )
public class IntegrationTestMojo
    extends AbstractSurefireMojo
{

    private static final String FAILSAFE_IN_PROGRESS_CONTEXT_KEY = "failsafe-in-progress";

    /**
     * Information about this plugin, mainly used to lookup this plugin's configuration from the currently executing
     * project.
     *
     * @since 2.12
     */
    @Parameter( defaultValue = "${plugin}", readonly = true )
    private PluginDescriptor pluginDescriptor;

    /**
     * Set this to "true" to skip running tests, but still compile them. Its use is NOT RECOMMENDED, but quite
     * convenient on occasion.
     *
     * @since 2.4
     */
    @Parameter( property = "skipTests", defaultValue = "false" )
    private boolean skipTests;

    /**
     * Set this to "true" to skip running integration tests, but still compile them. Its use is NOT RECOMMENDED, but
     * quite convenient on occasion.
     *
     * @since 2.4.3-alpha-2
     */
    @Parameter( property = "skipITs" )
    private boolean skipITs;

    /**
     * This old parameter is just like <code>skipTests</code>, but bound to the old property "maven.test.skip.exec".
     *
     * @since 2.3
     * @deprecated Use skipTests instead.
     */
    @Parameter( property = "maven.test.skip.exec" )
    private boolean skipExec;

    /**
     * Set this to "true" to bypass unit tests entirely. Its use is NOT RECOMMENDED, especially if you enable it using
     * the "maven.test.skip" property, because maven.test.skip disables both running the tests and compiling the tests.
     * Consider using the <code>skipTests parameter</code> instead.
     */
    @Parameter( property = "maven.test.skip", defaultValue = "false" )
    private boolean skip;

    /**
     * The base directory of the project being tested. This can be obtained in your integration test via
     * System.getProperty("basedir").
     */
    @Parameter( defaultValue = "${basedir}" )
    private File basedir;

    /**
     * The directory containing generated test classes of the project being tested. This will be included at the
     * beginning of the test classpath.
     */
    @Parameter( defaultValue = "${project.build.testOutputDirectory}" )
    private File testClassesDirectory;

    /**
     * The directory containing generated classes of the project being tested. This will be included after the test
     * classes in the test classpath.
     */
    @Parameter( defaultValue = "${project.build.outputDirectory}" )
    private File classesDirectory;

    /**
     * The Maven Project Object.
     */
    @Component
    private MavenProject project;

    /**
     * List of dependencies to exclude from the test classpath. Each dependency string must follow the format
     * <i>groupId:artifactId</i>. For example: <i>org.acme:project-a</i>
     *
     * @since 2.6
     */
    @Parameter
    private List<String> classpathDependencyExcludes;

    /**
     * A dependency scope to exclude from the test classpath. The scope should be one of the scopes defined by
     * org.apache.maven.artifact.Artifact. This includes the following:
     * <p/>
     * <ul>
     * <li><i>compile</i> - system, provided, compile
     * <li><i>runtime</i> - compile, runtime
     * <li><i>compile+runtime</i> - system, provided, compile, runtime
     * <li><i>runtime+system</i> - system, compile, runtime
     * <li><i>test</i> - system, provided, compile, runtime, test
     * </ul>
     *
     * @since 2.6
     */
    @Parameter( defaultValue = "" )
    private String classpathDependencyScopeExclude;

    /**
     * Additional elements to be appended to the classpath.
     *
     * @since 2.4
     */
    @Parameter
    private List<String> additionalClasspathElements;

    /**
     * Base directory where all reports are written to.
     */
    @Parameter( defaultValue = "${project.build.directory}/failsafe-reports" )
    private File reportsDirectory;

    /**
     * The test source directory containing test class sources.
     *
     * @since 2.2
     */
    @Parameter( defaultValue = "${project.build.testSourceDirectory}", required = true )
    private File testSourceDirectory;

    /**
     * Specify this parameter to run individual tests by file name, overriding the <code>includes/excludes</code>
     * parameters. Each pattern you specify here will be used to create an include pattern formatted like
     * <code>**&#47;${test}.java</code>, so you can just type "-Dit.test=MyTest" to run a single test called
     * "foo/MyTest.java".<br/>
     * This parameter overrides the <code>includes/excludes</code> parameters, and the TestNG <code>suiteXmlFiles</code>
     * parameter.
     * <p/>
     * since 2.7.3 You can execute a limited number of method in the test with adding #myMethod or #my*ethod. Si type
     * "-Dtest=MyTest#myMethod" <b>supported for junit 4.x and testNg</b>
     */
    @Parameter( property = "it.test" )
    private String test;

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
     * <p/>
     * This parameter is ignored if the TestNG <code>suiteXmlFiles</code> parameter is specified.
     */
    @Parameter
    private List<String> includes;

    /**
     * A list of &lt;exclude> elements specifying the tests (by pattern) that should be excluded in testing. When not
     * specified and when the <code>test</code> parameter is not specified, the default excludes will be <code><br/>
     * &lt;excludes><br/>
     * &nbsp;&lt;exclude>**&#47;*$*&lt;/exclude><br/>
     * &lt;/excludes><br/>
     * </code> (which excludes all inner classes).<br>
     * This parameter is ignored if the TestNG <code>suiteXmlFiles</code> parameter is specified.
     * <p/>
     * Each exclude item may also contain a comma-separated sublist of items, which will be treated as multiple
     * &nbsp;&lt;exclude> entries.<br/>
     */
    @Parameter
    private List<String> excludes;

    /**
     * ArtifactRepository of the localRepository. To obtain the directory of localRepository in unit tests use
     * System.getProperty("localRepository").
     */
    @Parameter( defaultValue = "${localRepository}", required = true, readonly = true )
    private ArtifactRepository localRepository;

    /**
     * List of System properties to pass to the JUnit tests.
     *
     * @deprecated Use systemPropertyVariables instead.
     */
    @Parameter
    private Properties systemProperties;

    /**
     * List of System properties to pass to the JUnit tests.
     *
     * @since 2.5
     */
    @Parameter
    private Map<String, String> systemPropertyVariables;

    /**
     * List of System properties, loaded from a file, to pass to the JUnit tests.
     *
     * @since 2.8.2
     */
    @Parameter
    private File systemPropertiesFile;

    /**
     * List of properties for configuring all TestNG related configurations. This is the new preferred method of
     * configuring TestNG.
     *
     * @since 2.4
     */
    @Parameter
    private Properties properties;

    /**
     * Map of plugin artifacts.
     */
    @Parameter( defaultValue = "${plugin.artifactMap}", required = true, readonly = true )
    private Map<String, Artifact> pluginArtifactMap;

    /**
     * Map of project artifacts.
     */
    @Parameter( defaultValue = "${project.artifactMap}", readonly = true, required = true )
    private Map<String, Artifact> projectArtifactMap;

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
     */
    @Parameter( property = "failsafe.reportFormat", defaultValue = "brief" )
    private String reportFormat;

    /**
     * Add custom text into report filename: TEST-testClassName-reportNameSuffix.xml,
     * testClassName-reportNameSuffix.txt and testClassName-reportNameSuffix-output.txt.
     * File TEST-testClassName-reportNameSuffix.xml has changed attributes 'testsuite'--'name'
     * and 'testcase'--'classname' - reportNameSuffix is added to the attribute value.
     */
    @Parameter( property = "surefire.reportNameSuffix", defaultValue = "" )
    private String reportNameSuffix;

    /**
     * Option to generate a file test report or just output the test report to the console.
     */
    @Parameter( property = "failsafe.useFile", defaultValue = "true" )
    private boolean useFile;

    /**
     * Set this to "true" to redirect the unit test standard output to a file (found in
     * reportsDirectory/testName-output.txt).
     *
     * @since 2.3
     */
    @Parameter( property = "maven.test.redirectTestOutputToFile", defaultValue = "false" )
    private boolean redirectTestOutputToFile;

    /**
     * Set this to "true" to cause a failure if there are no tests to run. Defaults to "false".
     *
     * @since 2.4
     */
    @Parameter( property = "failIfNoTests" )
    private Boolean failIfNoTests;

    /**
     * Set this to "true" to cause a failure if the none of the tests specified in -Dtest=... are run. Defaults to
     * "true".
     *
     * @since 2.12
     */
    @Parameter( property = "it.failIfNoSpecifiedTests" )
    private Boolean failIfNoSpecifiedTests;

    /**
     * Option to specify the forking mode. Can be "never", "once", "always" or "perthread". "none" and "pertest" are also accepted
     * for backwards compatibility. "always" forks for each test-class. "perthread" will create "threadCount" parallel forks.
     *
     * @since 2.1
     */
    @Parameter( property = "forkMode", defaultValue = "once" )
    private String forkMode;

    /**
     * Option to specify the jvm (or path to the java executable) to use with the forking options. For the default, the
     * jvm will be a new instance of the same VM as the one used to run Maven. JVM settings are not inherited from
     * MAVEN_OPTS.
     *
     * @since 2.1
     */
    @Parameter( property = "jvm" )
    private String jvm;

    /**
     * Arbitrary JVM options to set on the command line.
     *
     * @since 2.1
     */
    @Parameter( property = "argLine" )
    private String argLine;

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
     * Additional environment variables to set on the command line.
     *
     * @since 2.1.3
     */
    @Parameter
    private Map<String, String> environmentVariables = new HashMap<String, String>();

    /**
     * Command line working directory.
     *
     * @since 2.1.3
     */
    @Parameter( property = "basedir" )
    private File workingDirectory;

    /**
     * When false it makes tests run using the standard classloader delegation instead of the default Maven isolated
     * classloader. Only used when forking (forkMode is not "none").<br/>
     * Setting it to false helps with some problems caused by conflicts between xml parsers in the classpath and the
     * Java 5 provider parser.
     *
     * @since 2.1
     */
    @Parameter( property = "childDelegation", defaultValue = "false" )
    private boolean childDelegation;

    /**
     * (TestNG/JUnit47 provider with JUnit4.8+ only) Groups for this test. Only classes/methods/etc decorated with one of the groups specified here will
     * be included in test run, if specified.<br/>For JUnit, this parameter forces the use of the 4.7 provider<br/>
     * This parameter is ignored if the <code>suiteXmlFiles</code> parameter is specified.
     *
     * @since 2.2
     */
    @Parameter( property = "groups" )
    private String groups;

    /**
     * (TestNG/JUnit47 provider with JUnit4.8+ only) Excluded groups. Any methods/classes/etc with one of the groups specified in this list will
     * specifically not be run.<br/>For JUnit, this parameter forces the use of the 4.7 provider<br/>
     * This parameter is ignored if the <code>suiteXmlFiles</code> parameter is specified.
     *
     * @since 2.2
     */
    @Parameter( property = "excludedGroups" )
    private String excludedGroups;

    /**
     * (TestNG only) List of &lt;suiteXmlFile> elements specifying TestNG suite xml file locations. Note that
     * <code>suiteXmlFiles</code> is incompatible with several other parameters of this plugin, like
     * <code>includes/excludes</code>.<br/>
     * This parameter is ignored if the <code>test</code> parameter is specified (allowing you to run a single test
     * instead of an entire suite).
     *
     * @since 2.2
     */
    @Parameter
    private File[] suiteXmlFiles;

    /**
     * Allows you to specify the name of the JUnit artifact. If not set, <code>junit:junit</code> will be used.
     *
     * @since 2.3.1
     */
    @Parameter( property = "junitArtifactName", defaultValue = "junit:junit" )
    private String junitArtifactName;

    /**
     * Allows you to specify the name of the TestNG artifact. If not set, <code>org.testng:testng</code> will be used.
     *
     * @since 2.3.1
     */
    @Parameter( property = "testNGArtifactName", defaultValue = "org.testng:testng" )
    private String testNGArtifactName;

    /**
     * (forkMode=perthread or TestNG/JUnit 4.7 provider) The attribute thread-count allows you to specify how many threads should be
     * allocated for this execution. Only makes sense to use in conjunction with the <code>parallel</code> parameter. (forkMode=perthread
     * does not support/require the <code>parallel</code> parameter)
     *
     * @since 2.2
     */
    @Parameter( property = "threadCount" )
    private int threadCount;

    /**
     * (JUnit 4.7 provider) Indicates that threadCount is per cpu core.
     *
     * @since 2.5
     */
    @Parameter( property = "perCoreThreadCount", defaultValue = "true" )
    private boolean perCoreThreadCount;

    /**
     * (JUnit 4.7 provider) Indicates that the thread pool will be unlimited. The <code>parallel</code> parameter and
     * the actual number of classes/methods will decide. Setting this to "true" effectively disables
     * <code>perCoreThreadCount</code> and <code>threadCount</code>. Defaults to "false".
     *
     * @since 2.5
     */
    @Parameter( property = "useUnlimitedThreads", defaultValue = "false" )
    private boolean useUnlimitedThreads;

    /**
     * (TestNG only) When you use the <code>parallel</code> attribute, TestNG will try to run all your test methods in
     * separate threads, except for methods that depend on each other, which will be run in the same thread in order to
     * respect their order of execution.
     * <p/>
     * (JUnit 4.7 provider) Supports values "classes"/"methods"/"both" to run in separate threads, as controlled by
     * <code>threadCount</code>.
     *
     * @todo test how this works with forking, and console/file output parallelism
     * @since 2.2
     */
    @Parameter( property = "parallel" )
    private String parallel;

    /**
     * Whether to trim the stack trace in the reports to just the lines within the test, or show the full trace.
     *
     * @since 2.2
     */
    @Parameter( property = "trimStackTrace", defaultValue = "true" )
    private boolean trimStackTrace;

    /**
     * Resolves the artifacts needed.
     */
    @Component
    private ArtifactResolver artifactResolver;

    /**
     * Creates the artifact.
     */
    @Component
    private ArtifactFactory artifactFactory;

    /**
     * The remote plugin repositories declared in the POM.
     *
     * @since 2.2
     */
    @Parameter( defaultValue = "${project.pluginArtifactRepositories}" )
    private List remoteRepositories;

    /**
     * For retrieval of artifact's metadata.
     */
    @Component
    private ArtifactMetadataSource metadataSource;

    private Properties originalSystemProperties;

    /**
     * systemPropertyVariables + systemProperties
     */
    private Properties internalSystemProperties = new Properties();

    /**
     * Flag to disable the generation of report files in xml format.
     *
     * @since 2.2
     */
    @Parameter( property = "disableXmlReport", defaultValue = "false" )
    private boolean disableXmlReport;

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
     * launch your tests with a plain old Java classpath. (See
     * http://maven.apache.org/plugins/maven-surefire-plugin/examples/class-loading.html for a more detailed explanation
     * of manifest-only JARs and their benefits.)
     * <p/>
     * Beware, setting this to "false" may cause your tests to fail on Windows if your classpath is too long.
     *
     * @since 2.4.3
     */
    @Parameter( property = "failsafe.useManifestOnlyJar", defaultValue = "true" )
    private boolean useManifestOnlyJar;

    /**
     * By default, Surefire enables JVM assertions for the execution of your test cases. To disable the assertions, set
     * this flag to "false".
     *
     * @since 2.3.1
     */
    @Parameter( property = "enableAssertions", defaultValue = "true" )
    private boolean enableAssertions;

    /**
     * The current build session instance.
     */
    @Component
    private MavenSession session;

    /**
     * (TestNG only) Define the factory class used to create all test instances.
     *
     * @since 2.5
     */
    @Parameter( property = "objectFactory" )
    private String objectFactory;

    /**
     * The character encoding scheme to be applied.
     */
    @Parameter( property = "encoding", defaultValue = "${project.reporting.outputEncoding}" )
    private String encoding;

    /**
     *
     */
    @Parameter( defaultValue = "${session.parallel}", readonly = true )
    private Boolean parallelMavenExecution;

    /**
     * Defines the order the tests will be run in. Supported values are "alphabetical", "reversealphabetical", "random",
     * "hourly" (alphabetical on even hours, reverse alphabetical on odd hours), "failedfirst", "balanced" and "filesystem".
     * <p/>
     * <p/>
     * Odd/Even for hourly is determined at the time the of scanning the classpath, meaning it could change during a
     * multi-module build.
     * <p/>
     * Failed first will run tests that failed on previous run first, as well as new tests for this run.
     * <p/>
     * Balanced is only relevant with parallel=classes, and will try to optimize the run-order of the tests to
     * make all tests complete at the same time, reducing the overall execution time.
     * <p/>
     * Note that the statistics are stored in a file named .surefire-XXXXXXXXX beside pom.xml, and should not
     * be checked into version control. The "XXXXX" is the SHA1 checksum of the entire surefire configuration,
     * so different configurations will have different statistics files, meaning if you change any config
     * settings you will re-run once before new statistics data can be established.
     *
     * @since 2.7
     */
    @Parameter( defaultValue = "filesystem" )
    private String runOrder;

    /**
     *
     */
    @Component
    private ToolchainManager toolchainManager;

    protected void handleSummary( Summary summary )
        throws MojoExecutionException, MojoFailureException
    {
        FailsafeSummary failsafeSummary = createFailsafeSummaryFromSummary( summary );
        writeSummary( failsafeSummary );
    }

    private FailsafeSummary createFailsafeSummaryFromSummary( Summary summary )
    {
        FailsafeSummary failsafeSummary = new FailsafeSummary();
        if ( summary.isErrorFree() )
        {
            RunResult result = summary.getResultOfLastSuccessfulRun();
            if ( result != null )
            {
                failsafeSummary.setResult( result.getForkedProcessCode() );
            }
        }
        else
        {
            failsafeSummary.setResult( ProviderConfiguration.TESTS_FAILED_EXIT_CODE );
            //noinspection ThrowableResultOfMethodCallIgnored
            failsafeSummary.setException( summary.getFirstException().getMessage() );
        }
        return failsafeSummary;
    }

    @SuppressWarnings( "unchecked" )
    private void writeSummary( FailsafeSummary summary )
        throws MojoExecutionException
    {
        File summaryFile = getSummaryFile();
        if ( !summaryFile.getParentFile().isDirectory() )
        {
            summaryFile.getParentFile().mkdirs();
        }

        FileOutputStream fout = null;
        FileInputStream fin = null;
        try
        {
            FailsafeSummary mergedSummary = summary;
            Object token = getPluginContext().get( FAILSAFE_IN_PROGRESS_CONTEXT_KEY );
            if ( summaryFile.exists() && token != null )
            {
                fin = new FileInputStream( summaryFile );

                mergedSummary = new FailsafeSummaryXpp3Reader().read(
                    new InputStreamReader( new BufferedInputStream( fin ), getEncodingOrDefault() ) );

                mergedSummary.merge( summary );
            }

            fout = new FileOutputStream( summaryFile );
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream( fout );
            Writer writer = new OutputStreamWriter( bufferedOutputStream, getEncodingOrDefault() );
            FailsafeSummaryXpp3Writer xpp3Writer = new FailsafeSummaryXpp3Writer();
            xpp3Writer.write( writer, mergedSummary );
            writer.close();
            bufferedOutputStream.close();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( XmlPullParserException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        finally
        {
            close( fin );
            close( fout );
        }

        getPluginContext().put( FAILSAFE_IN_PROGRESS_CONTEXT_KEY, FAILSAFE_IN_PROGRESS_CONTEXT_KEY );
    }

    private String getEncodingOrDefault()
    {
        if ( StringUtils.isEmpty( encoding ) )
        {
            getLog().warn( "File encoding has not been set, using platform encoding " + ReaderFactory.FILE_ENCODING +
                               ", i.e. build is platform dependent!" );
            return ReaderFactory.FILE_ENCODING;
        }
        else
        {
            return encoding;
        }
    }

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

    public File getClassesDirectory()
    {
        return classesDirectory;
    }

    public void setClassesDirectory( File classesDirectory )
    {
        this.classesDirectory = classesDirectory;
    }

    public MavenProject getProject()
    {
        return project;
    }

    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    public List getClasspathDependencyExcludes()
    {
        return classpathDependencyExcludes;
    }

    public void setClasspathDependencyExcludes( List classpathDependencyExcludes )
    {
        this.classpathDependencyExcludes = classpathDependencyExcludes;
    }

    public String getClasspathDependencyScopeExclude()
    {
        return classpathDependencyScopeExclude;
    }

    public void setClasspathDependencyScopeExclude( String classpathDependencyScopeExclude )
    {
        this.classpathDependencyScopeExclude = classpathDependencyScopeExclude;
    }

    public List getAdditionalClasspathElements()
    {
        return additionalClasspathElements;
    }

    public void setAdditionalClasspathElements( List additionalClasspathElements )
    {
        this.additionalClasspathElements = additionalClasspathElements;
    }

    public File getReportsDirectory()
    {
        return reportsDirectory;
    }

    public void setReportsDirectory( File reportsDirectory )
    {
        this.reportsDirectory = reportsDirectory;
    }

    public File getTestSourceDirectory()
    {
        return testSourceDirectory;
    }

    public void setTestSourceDirectory( File testSourceDirectory )
    {
        this.testSourceDirectory = testSourceDirectory;
    }

    public String getTest()
    {
        if ( StringUtils.isBlank( test ) )
        {
            return null;
        }
        int index = test.indexOf( '#' );
        if ( index >= 0 )
        {
            return test.substring( 0, index );
        }
        return test;
    }

    public void setTest( String test )
    {
        this.test = test;
    }

    /**
     * @since 2.7.3
     */
    public String getTestMethod()
    {
        if ( StringUtils.isBlank( test ) )
        {
            return null;
        }
        int index = this.test.indexOf( '#' );
        if ( index >= 0 )
        {
            return this.test.substring( index + 1, this.test.length() );
        }
        return null;
    }

    public List<String> getIncludes()
    {
        return includes;
    }

    public void setIncludes( List<String> includes )
    {
        this.includes = includes;
    }

    public List<String> getExcludes()
    {
        return excludes;
    }

    public void setExcludes( List<String> excludes )
    {
        this.excludes = excludes;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    public Properties getSystemProperties()
    {
        return systemProperties;
    }

    public void setSystemProperties( Properties systemProperties )
    {
        this.systemProperties = systemProperties;
    }

    public Map<String, String> getSystemPropertyVariables()
    {
        return systemPropertyVariables;
    }

    public void setSystemPropertyVariables( Map<String, String> systemPropertyVariables )
    {
        this.systemPropertyVariables = systemPropertyVariables;
    }

    public File getSystemPropertiesFile()
    {
        return systemPropertiesFile;
    }

    public void setSystemPropertiesFile( File systemPropertiesFile )
    {
        this.systemPropertiesFile = systemPropertiesFile;
    }

    public Properties getProperties()
    {
        return properties;
    }

    public void setProperties( Properties properties )
    {
        this.properties = properties;
    }

    public Map<String, Artifact> getPluginArtifactMap()
    {
        return pluginArtifactMap;
    }

    public void setPluginArtifactMap( Map<String, Artifact> pluginArtifactMap )
    {
        this.pluginArtifactMap = pluginArtifactMap;
    }

    public Map getProjectArtifactMap()
    {
        return projectArtifactMap;
    }

    public void setProjectArtifactMap( Map projectArtifactMap )
    {
        this.projectArtifactMap = projectArtifactMap;
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

    public String getReportNameSuffix()
    {
        return reportNameSuffix;
    }

    public void setReportNameSuffix( String reportNameSuffix )
    {
        this.reportNameSuffix = reportNameSuffix;
    }

    public boolean isUseFile()
    {
        return useFile;
    }

    public void setUseFile( boolean useFile )
    {
        this.useFile = useFile;
    }

    public boolean isRedirectTestOutputToFile()
    {
        return redirectTestOutputToFile;
    }

    public void setRedirectTestOutputToFile( boolean redirectTestOutputToFile )
    {
        this.redirectTestOutputToFile = redirectTestOutputToFile;
    }

    public Boolean getFailIfNoTests()
    {
        return failIfNoTests;
    }

    public void setFailIfNoTests( Boolean failIfNoTests )
    {
        this.failIfNoTests = failIfNoTests;
    }

    public String getForkMode()
    {
        return forkMode;
    }

    public void setForkMode( String forkMode )
    {
        this.forkMode = forkMode;
    }

    public String getJvm()
    {
        return jvm;
    }

    public void setJvm( String jvm )
    {
        this.jvm = jvm;
    }

    public String getArgLine()
    {
        return argLine;
    }

    public void setArgLine( String argLine )
    {
        this.argLine = argLine;
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

    public Map<String, String> getEnvironmentVariables()
    {
        return environmentVariables;
    }

    public void setEnvironmentVariables( Map<String, String> environmentVariables )
    {
        this.environmentVariables = environmentVariables;
    }

    public File getWorkingDirectory()
    {
        return workingDirectory;
    }

    public void setWorkingDirectory( File workingDirectory )
    {
        this.workingDirectory = workingDirectory;
    }

    public boolean isChildDelegation()
    {
        return childDelegation;
    }

    public void setChildDelegation( boolean childDelegation )
    {
        this.childDelegation = childDelegation;
    }

    public String getGroups()
    {
        return groups;
    }

    public void setGroups( String groups )
    {
        this.groups = groups;
    }

    public String getExcludedGroups()
    {
        return excludedGroups;
    }

    public void setExcludedGroups( String excludedGroups )
    {
        this.excludedGroups = excludedGroups;
    }

    public File[] getSuiteXmlFiles()
    {
        return suiteXmlFiles;
    }

    public void setSuiteXmlFiles( File[] suiteXmlFiles )
    {
        this.suiteXmlFiles = suiteXmlFiles;
    }

    public String getJunitArtifactName()
    {
        return junitArtifactName;
    }

    public void setJunitArtifactName( String junitArtifactName )
    {
        this.junitArtifactName = junitArtifactName;
    }

    public String getTestNGArtifactName()
    {
        return testNGArtifactName;
    }

    public void setTestNGArtifactName( String testNGArtifactName )
    {
        this.testNGArtifactName = testNGArtifactName;
    }

    public int getThreadCount()
    {
        return threadCount;
    }

    public void setThreadCount( int threadCount )
    {
        this.threadCount = threadCount;
    }

    public boolean getPerCoreThreadCount()
    {
        return perCoreThreadCount;
    }

    public void setPerCoreThreadCount( boolean perCoreThreadCount )
    {
        this.perCoreThreadCount = perCoreThreadCount;
    }

    public boolean getUseUnlimitedThreads()
    {
        return useUnlimitedThreads;
    }

    public void setUseUnlimitedThreads( boolean useUnlimitedThreads )
    {
        this.useUnlimitedThreads = useUnlimitedThreads;
    }

    public String getParallel()
    {
        return parallel;
    }

    public void setParallel( String parallel )
    {
        this.parallel = parallel;
    }

    public boolean isTrimStackTrace()
    {
        return trimStackTrace;
    }

    public void setTrimStackTrace( boolean trimStackTrace )
    {
        this.trimStackTrace = trimStackTrace;
    }

    public ArtifactResolver getArtifactResolver()
    {
        return artifactResolver;
    }

    public void setArtifactResolver( ArtifactResolver artifactResolver )
    {
        this.artifactResolver = artifactResolver;
    }

    public ArtifactFactory getArtifactFactory()
    {
        return artifactFactory;
    }

    public void setArtifactFactory( ArtifactFactory artifactFactory )
    {
        this.artifactFactory = artifactFactory;
    }

    public List getRemoteRepositories()
    {
        return remoteRepositories;
    }

    public void setRemoteRepositories( List remoteRepositories )
    {
        this.remoteRepositories = remoteRepositories;
    }

    public ArtifactMetadataSource getMetadataSource()
    {
        return metadataSource;
    }

    public void setMetadataSource( ArtifactMetadataSource metadataSource )
    {
        this.metadataSource = metadataSource;
    }

    public Properties getOriginalSystemProperties()
    {
        return originalSystemProperties;
    }

    public void setOriginalSystemProperties( Properties originalSystemProperties )
    {
        this.originalSystemProperties = originalSystemProperties;
    }

    public Properties getInternalSystemProperties()
    {
        return internalSystemProperties;
    }

    public void setInternalSystemProperties( Properties internalSystemProperties )
    {
        this.internalSystemProperties = internalSystemProperties;
    }

    public boolean isDisableXmlReport()
    {
        return disableXmlReport;
    }

    public void setDisableXmlReport( boolean disableXmlReport )
    {
        this.disableXmlReport = disableXmlReport;
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

    public boolean isEnableAssertions()
    {
        return enableAssertions;
    }

    public void setEnableAssertions( boolean enableAssertions )
    {
        this.enableAssertions = enableAssertions;
    }

    public MavenSession getSession()
    {
        return session;
    }

    public void setSession( MavenSession session )
    {
        this.session = session;
    }

    public String getObjectFactory()
    {
        return objectFactory;
    }

    public void setObjectFactory( String objectFactory )
    {
        this.objectFactory = objectFactory;
    }

    public ToolchainManager getToolchainManager()
    {
        return toolchainManager;
    }

    public void setToolchainManager( ToolchainManager toolchainManager )
    {
        this.toolchainManager = toolchainManager;
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

    public boolean isMavenParallel()
    {
        return parallelMavenExecution != null && parallelMavenExecution;
    }

    public String getRunOrder()
    {
        return runOrder;
    }

    public void setRunOrder( String runOrder )
    {
        this.runOrder = runOrder;
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

    public void setFailIfNoSpecifiedTests( Boolean failIfNoSpecifiedTests )
    {
        this.failIfNoSpecifiedTests = failIfNoSpecifiedTests;
    }

    public PluginDescriptor getPluginDescriptor()
    {
        return pluginDescriptor;
    }

}
