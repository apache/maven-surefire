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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.surefire.booter.ForkConfiguration;
import org.apache.maven.surefire.booter.SurefireBooter;
import org.apache.maven.surefire.booter.SurefireBooterForkException;
import org.apache.maven.surefire.booter.SurefireExecutionException;
import org.apache.maven.surefire.report.BriefConsoleReporter;
import org.apache.maven.surefire.report.BriefFileReporter;
import org.apache.maven.surefire.report.ConsoleReporter;
import org.apache.maven.surefire.report.DetailedConsoleReporter;
import org.apache.maven.surefire.report.FileReporter;
import org.apache.maven.surefire.report.ForkingConsoleReporter;
import org.apache.maven.surefire.report.XMLReporter;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.StringUtils;

/**
 * Run tests using Surefire.
 *
 * @author Jason van Zyl
 * @version $Id$
 * @requiresDependencyResolution test
 * @goal test
 * @phase test
 */
public class SurefirePlugin
    extends AbstractMojo
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
     * @parameter expression="${maven.test.skip}"
     */
    private boolean skip;

    /**
     * Set this to true to ignore a failure during testing. Its use is NOT RECOMMENDED, but quite convenient on
     * occasion.
     *
     * @parameter expression="${maven.test.failure.ignore}"
     */
    private boolean testFailureIgnore;

    /**
     * The base directory of the project being tested. This can be obtained in your unit test by
     * System.getProperty("basedir").
     *
     * @parameter expression="${basedir}"
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
     * The directory containing generated classes of the project being tested.
     * This will be included after the test classes in the test classpath.
     *
     * @parameter default-value="${project.build.outputDirectory}"
     */
    private File classesDirectory;

    /**
     * The Maven Project Object
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The classpath elements of the project being tested.
     *
     * @parameter expression="${project.testClasspathElements}"
     * @required
     * @readonly
     */
    private List classpathElements;

    /**
     * Additional elements to be appended to the classpath.
     *
     * @parameter
     * @since 2.4
     */
    private List additionalClasspathElements;

    /**
     * Base directory where all reports are written to.
     *
     * @parameter expression="${project.build.directory}/surefire-reports"
     */
    private File reportsDirectory;

    /**
     * The test source directory containing test class sources.
     *
     * @parameter expression="${project.build.testSourceDirectory}"
     * @required
     * @since 2.2
     */
    private File testSourceDirectory;

    /**
     * Specify this parameter to run individual tests by file name, overriding the <code>includes/excludes</code>
     * parameters.  Each pattern you specify here will be used to create an
     * include pattern formatted like <code>**&#47;${test}.java</code>, so you can just type "-Dtest=MyTest"
     * to run a single test called "foo/MyTest.java".  This parameter will override the TestNG suiteXmlFiles
     * parameter.
     *
     * @parameter expression="${test}"
     */
    private String test;

    /**
     * List of patterns (separated by commas) used to specify the tests that should be included in testing. When not
     * specified and when the <code>test</code> parameter is not specified, the default includes will be
     * <code>**&#47;Test*.java   **&#47;*Test.java   **&#47;*TestCase.java</code>.  This parameter is ignored if
     * TestNG suiteXmlFiles are specified.
     *
     * @parameter
     */
    private List includes;

    /**
     * List of patterns (separated by commas) used to specify the tests that should be excluded in testing. When not
     * specified and when the <code>test</code> parameter is not specified, the default excludes will be
     * <code>**&#47;*$*</code> (which excludes all inner classes).  This parameter is ignored if
     * TestNG suiteXmlFiles are specified.
     *
     * @parameter
     */
    private List excludes;

    /**
     * ArtifactRepository of the localRepository. To obtain the directory of localRepository in unit tests use
     * System.setProperty( "localRepository").
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * List of System properties to pass to the JUnit tests.
     *
     * @parameter
     * @deprecated Use systemPropertyVariables instead.
     */
    private Properties systemProperties;

    /**
     * List of System properties to pass to the JUnit tests.
     *
     * @parameter
     * @since 2.5
     */
    private Map systemPropertyVariables;

    /**
     * List of properties for configuring all TestNG related configurations. This is the new
     * preferred method of configuring TestNG.
     *
     * @parameter
     * @since 2.4
     */
    private Properties properties;

    /**
     * Map of of plugin artifacts.
     *
     * @parameter expression="${plugin.artifactMap}"
     * @required
     * @readonly
     */
    private Map pluginArtifactMap;

    /**
     * Map of of project artifacts.
     *
     * @parameter expression="${project.artifactMap}"
     * @required
     * @readonly
     */
    private Map projectArtifactMap;

    /**
     * Option to print summary of test suites or just print the test cases that has errors.
     *
     * @parameter expression="${surefire.printSummary}" default-value="true"
     */
    private boolean printSummary;

    /**
     * Selects the formatting for the test report to be generated. Can be set as brief or plain.
     *
     * @parameter expression="${surefire.reportFormat}" default-value="brief"
     */
    private String reportFormat;

    /**
     * Option to generate a file test report or just output the test report to the console.
     *
     * @parameter expression="${surefire.useFile}" default-value="true"
     */
    private boolean useFile;

    /**
     * When forking, set this to true to redirect the unit test standard output to a file (found in
     * reportsDirectory/testName-output.txt).
     *
     * @parameter expression="${maven.test.redirectTestOutputToFile}" default-value="false"
     * @since 2.3
     */
    private boolean redirectTestOutputToFile;

    /**
     * Set this to "true" to cause a failure if there are no tests to run.
     *
     * @parameter expression="${failIfNoTests}"
     * @since 2.4
     */
    private Boolean failIfNoTests;

    /**
     * Option to specify the forking mode. Can be "never", "once" or "always". "none" and "pertest" are also accepted
     * for backwards compatibility.
     *
     * @parameter expression="${forkMode}" default-value="once"
     * @since 2.1
     */
    private String forkMode;

    /**
     * Option to specify the jvm (or path to the java executable) to use with the forking options. For the default, the
     * jvm will be the same as the one used to run Maven.
     *
     * @parameter expression="${jvm}"
     * @since 2.1
     */
    private String jvm;

    /**
     * Arbitrary JVM options to set on the command line.
     *
     * @parameter expression="${argLine}"
     * @since 2.1
     */
    private String argLine;

    /**
     * Attach a debugger to the forked JVM.  If set to "true", the process will suspend and
     * wait for a debugger to attach on port 5005.  If set to some other string, that
     * string will be appended to the argLine, allowing you to configure arbitrary
     * debuggability options (without overwriting the other options specified in the argLine).
     *
     * @parameter expression="${maven.surefire.debug}"
     * @since 2.4
     */
    private String debugForkedProcess;

    /**
     * Kill the forked test process after a certain number of seconds.  If set to 0,
     * wait forever for the process, never timing out.
     *
     * @parameter expression="${surefire.timeout}"
     * @since 2.4
     */
    private int forkedProcessTimeoutInSeconds;

    /**
     * Additional environments to set on the command line.
     *
     * @parameter
     * @since 2.1.3
     */
    private Map environmentVariables = new HashMap();

    /**
     * Command line working directory.
     *
     * @parameter expression="${basedir}"
     * @since 2.1.3
     */
    private File workingDirectory;

    /**
     * When false it makes tests run using the standard classloader delegation instead of the default Maven isolated
     * classloader. Only used when forking (forkMode is not "none").<br/> Setting it to false helps with some problems
     * caused by conflicts between xml parsers in the classpath and the Java 5 provider parser.
     *
     * @parameter expression="${childDelegation}" default-value="false"
     * @since 2.1
     */
    private boolean childDelegation;

    /**
     * (TestNG only) Groups for this test. Only classes/methods/etc decorated with one of the groups specified here will be included
     * in test run, if specified.  This parameter is overridden if suiteXmlFiles are specified.
     *
     * @parameter expression="${groups}"
     * @since 2.2
     */
    private String groups;

    /**
     * (TestNG only) Excluded groups. Any methods/classes/etc with one of the groups specified in this list will specifically not be
     * run.  This parameter is overridden if suiteXmlFiles are specified.
     *
     * @parameter expression="${excludedGroups}"
     * @since 2.2
     */
    private String excludedGroups;

    /**
     * (TestNG only) List of TestNG suite xml file locations, seperated by commas. Note that suiteXmlFiles is incompatible
     * with several other parameters on this plugin, like includes/excludes.  This parameter is ignored if
     * the "test" parameter is specified (allowing you to run a single test instead of an entire suite).
     *
     * @parameter
     * @since 2.2
     */
    private File[] suiteXmlFiles;

    /**
     * Allows you to specify the name of the JUnit artifact. If not set, <code>junit:junit</code> will be used.
     *
     * @parameter expression="${junitArtifactName}" default-value="junit:junit"
     * @since 2.3.1
     */
    private String junitArtifactName;

    /**
     * Allows you to specify the name of the TestNG artifact. If not set, <code>org.testng:testng</code> will be used.
     *
     * @parameter expression="${testNGArtifactName}" default-value="org.testng:testng"
     * @since 2.3.1
     */
    private String testNGArtifactName;

    /**
     * (TestNG only) The attribute thread-count allows you to specify how many threads should be allocated for this execution. Only
     * makes sense to use in conjunction with parallel.
     *
     * @parameter expression="${threadCount}"
     * @since 2.2
     */
    private int threadCount;

    /**
     * (junitcore only) Indicates that threadCount is per cpu core. Defaults to true
     *
     * @parameter expression="${perCoreThreadCount}"
     * @since 2.5
     */
    private String perCoreThreadCount;

    /**
     * (junitcore only) Indicates that the thread pool will be unlimited. The parallel parameter and the actual number of classes/methods
     * will decide. Setting this to true effectively disables perCoreThreadCount and threadCount.
     *
     * @parameter expression="${useUnlimitedThreads}"
     * @since 2.5
     */
    private String useUnlimitedThreads;

    /**
     * (TestNG only) When you use the parallel attribute, TestNG will try to run all your test methods in separate threads, except for
     * methods that depend on each other, which will be run in the same thread in order to respect their order of
     * execution.
     * <p/>
     * In JUnit 4.7 the values are classes/methods/both to run in separate threads, as controlled by threadCount.
     *
     * @parameter expression="${parallel}"
     * @todo test how this works with forking, and console/file output parallelism
     * @since 2.2
     */
    private String parallel;

    /**
     * Whether to trim the stack trace in the reports to just the lines within the test, or show the full trace.
     *
     * @parameter expression="${trimStackTrace}" default-value="true"
     * @since 2.2
     */
    private boolean trimStackTrace;

    /**
     * Resolves the artifacts needed.
     *
     * @component
     */
    private ArtifactResolver artifactResolver;

    /**
     * Creates the artifact
     *
     * @component
     */
    private ArtifactFactory artifactFactory;

    /**
     * The plugin remote repositories declared in the POM.
     *
     * @parameter expression="${project.pluginArtifactRepositories}"
     * @since 2.2
     */
    private List remoteRepositories;

    /**
     * For retrieval of artifact's metadata.
     *
     * @component
     */
    private ArtifactMetadataSource metadataSource;

    private static final String BRIEF_REPORT_FORMAT = "brief";

    private static final String PLAIN_REPORT_FORMAT = "plain";

    private Properties originalSystemProperties;

    /**
     * systemPropertyVariables + systemProperties
     */
    private Properties internalSystemProperties = new Properties();

    /**
     * Flag to disable the generation of report files in xml format.
     *
     * @parameter expression="${disableXmlReport}" default-value="false"
     * @since 2.2
     */
    private boolean disableXmlReport;

    /**
     * Option to pass dependencies to the system's classloader instead of using an isolated class loader when forking.
     * Prevents problems with JDKs which implement the service provider lookup mechanism by using the system's
     * classloader.  Default value is "true".
     *
     * @parameter expression="${surefire.useSystemClassLoader}"
     * @since 2.3
     */
    private Boolean useSystemClassLoader;

    /**
     * By default, Surefire forks your tests using a manifest-only JAR; set this parameter
     * to "false" to force it to launch your tests with a plain old Java classpath.
     * (See http://maven.apache.org/plugins/maven-surefire-plugin/examples/class-loading.html
     * for a more detailed explanation of manifest-only JARs and their benefits.)
     * <p/>
     * Beware, setting this to "false" may cause your tests to
     * fail on Windows if your classpath is too long.
     *
     * @parameter expression="${surefire.useManifestOnlyJar}" default-value="true"
     * @since 2.4.3
     */
    private boolean useManifestOnlyJar;

    /**
     * By default, Surefire enables JVM assertions for the execution of your test cases. To disable the assertions, set
     * this flag to <code>false</code>.
     *
     * @parameter expression="${enableAssertions}" default-value="true"
     * @since 2.3.1
     */
    private boolean enableAssertions;

    /**
     * The current build session instance.
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    private MavenSession session;

    /**
     * (TestNG only) Define the factory class used to create all test instances
     *
     * @parameter expression="${objectFactory}"
     * @since 2.5
     */
    private String objectFactory;

    /**
     * @component
     */
    private ToolchainManager toolchainManager;


    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( verifyParameters() )
        {
            SurefireBooter surefireBooter = constructSurefireBooter();

            getLog().info( "Surefire report directory: " + reportsDirectory );

            int result;
            try
            {
                result = surefireBooter.run();
            }
            catch ( SurefireBooterForkException e )
            {
                throw new MojoExecutionException( e.getMessage(), e );
            }
            catch ( SurefireExecutionException e )
            {
                throw new MojoExecutionException( e.getMessage(), e );
            }

            if ( originalSystemProperties != null && !surefireBooter.isForking() )
            {
                // restore system properties, only makes sense when not forking..
                System.setProperties( originalSystemProperties );
            }

            if ( result == 0 )
            {
                return;
            }

            String msg;

            if ( result == SurefireBooter.NO_TESTS_EXIT_CODE )
            {
                if ( ( failIfNoTests == null ) || !failIfNoTests.booleanValue() )
                {
                    return;
                }
                // TODO: i18n
                throw new MojoFailureException(
                    "No tests were executed!  (Set -DfailIfNoTests=false to ignore this error.)" );
            }
            else
            {
                // TODO: i18n
                msg = "There are test failures.\n\nPlease refer to " + reportsDirectory
                    + " for the individual test results.";

            }

            if ( testFailureIgnore )
            {
                getLog().error( msg );
            }
            else
            {
                throw new MojoFailureException( msg );
            }
        }
    }

    private boolean verifyParameters()
        throws MojoFailureException
    {
        if ( skip || skipTests || skipExec )
        {
            getLog().info( "Tests are skipped." );
            return false;
        }

        if ( !testClassesDirectory.exists() )
        {
            if ( failIfNoTests != null && failIfNoTests.booleanValue() )
            {
                throw new MojoFailureException( "No tests to run!" );
            }
            getLog().info( "No tests to run." );
            return false;
        }

        if ( useSystemClassLoader != null && ForkConfiguration.FORK_NEVER.equals( forkMode ) )
        {
            getLog().warn( "useSystemClassloader setting has no effect when not forking" );
        }

        return true;
    }

    /**
     * Converts old TestNG configuration parameters over to new properties based configuration
     * method. (if any are defined the old way)
     */
    private void convertTestNGParameters()
    {
        if ( properties == null )
        {
            properties = new Properties();
        }

        if ( this.parallel != null )
        {
            properties.setProperty( "parallel", this.parallel );
        }
        if ( this.excludedGroups != null )
        {
            properties.setProperty( "excludegroups", this.excludedGroups );
        }
        if ( this.groups != null )
        {
            properties.setProperty( "groups", this.groups );
        }

        if ( this.threadCount > 0 )
        {
            properties.setProperty( "threadcount", new Integer( this.threadCount ).toString() );
        }
        if ( this.objectFactory != null )
        {
            properties.setProperty( "objectfactory", this.objectFactory );
        }
    }

    private boolean isAnyConcurrencySelected()
    {
        return this.parallel != null && this.parallel.trim().length() > 0;
    }

    /**
     * Converts old TestNG configuration parameters over to new properties based configuration
     * method. (if any are defined the old way)
     */
    private void convertJunitCoreParameters()
    {
        if ( properties == null )
        {
            properties = new Properties();
        }

        if ( this.parallel != null )
        {
            properties.setProperty( "parallel", this.parallel );
        }
        if ( this.threadCount > 0 )
        {
            properties.setProperty( "threadCount", new Integer( this.threadCount ).toString() );
        }
        if ( this.perCoreThreadCount != null )
        {
            properties.setProperty( "perCoreThreadCount", perCoreThreadCount );
        }
        if ( this.useUnlimitedThreads != null )
        {
            properties.setProperty( "useUnlimitedThreads", useUnlimitedThreads );
        }
        Artifact configurableParallelComputer =
            (Artifact) projectArtifactMap.get( "org.jdogma.junit:configurable-parallel-computer" );
        properties.setProperty( "configurableParallelComputerPresent",
                                Boolean.toString( configurableParallelComputer != null ) );

    }

    private boolean isJunit47Compatible( Artifact artifact )
        throws MojoExecutionException
    {
        return isWithinVersionSpec( artifact, "[4.7,)" );
    }

    private boolean isAnyJunit4( Artifact artifact )
        throws MojoExecutionException
    {
        return isWithinVersionSpec( artifact, "[4.0,)" );
    }

    private boolean isWithinVersionSpec( Artifact artifact, String versionSpec )
        throws MojoExecutionException
    {
        if ( artifact == null )
        {
            return false;
        }
        try
        {
            VersionRange range = VersionRange.createFromVersionSpec( versionSpec );
            try
            {
                return range.containsVersion( artifact.getSelectedVersion() );
            }
            catch ( NullPointerException e )
            {
                return range.containsVersion( new DefaultArtifactVersion( artifact.getBaseVersion() ) );
            }
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new MojoExecutionException( "Bug in junit 4.7 plugin. Please report with stacktrace" );
        }
        catch ( OverConstrainedVersionException e )
        {
            throw new MojoExecutionException( "Bug in junit 4.7 plugin. Please report with stacktrace" );
        }
    }


    private SurefireBooter constructSurefireBooter()
        throws MojoExecutionException, MojoFailureException
    {
        SurefireBooter surefireBooter = new SurefireBooter();

        Artifact surefireArtifact = (Artifact) pluginArtifactMap.get( "org.apache.maven.surefire:surefire-booter" );
        if ( surefireArtifact == null )
        {
            throw new MojoExecutionException( "Unable to locate surefire-booter in the list of plugin artifacts" );
        }

        surefireArtifact.isSnapshot(); // MNG-2961: before Maven 2.0.8, fixes getBaseVersion to be -SNAPSHOT if needed

        Artifact junitArtifact;
        Artifact testNgArtifact;
        try
        {
            addArtifact( surefireBooter, surefireArtifact );

            junitArtifact = (Artifact) projectArtifactMap.get( junitArtifactName );
            // SUREFIRE-378, junit can have an alternate artifact name
            if ( junitArtifact == null && "junit:junit".equals( junitArtifactName ) )
            {
                junitArtifact = (Artifact) projectArtifactMap.get( "junit:junit-dep" );
            }

            // TODO: this is pretty manual, but I'd rather not require the plugin > dependencies section right now
            testNgArtifact = (Artifact) projectArtifactMap.get( testNGArtifactName );

            if ( testNgArtifact != null )
            {
                VersionRange range = VersionRange.createFromVersionSpec( "[4.7,)" );
                if ( !range.containsVersion( new DefaultArtifactVersion( testNgArtifact.getVersion() ) ) )
                {
                    throw new MojoFailureException(
                        "TestNG support requires version 4.7 or above. You have declared version "
                            + testNgArtifact.getVersion() );
                }

                convertTestNGParameters();

                if ( this.testClassesDirectory != null )
                {
                    properties.setProperty( "testng.test.classpath", testClassesDirectory.getAbsolutePath() );
                }

                addArtifact( surefireBooter, testNgArtifact );

                // The plugin uses a JDK based profile to select the right testng. We might be explicity using a
                // different one since its based on the source level, not the JVM. Prune using the filter.
                addProvider( surefireBooter, "surefire-testng", surefireArtifact.getBaseVersion(), testNgArtifact );
            }
            else if ( junitArtifact != null && isAnyJunit4( junitArtifact ) )
            {
                if ( isAnyConcurrencySelected() && isJunit47Compatible( junitArtifact ) )
                {
                    convertJunitCoreParameters();
                    addProvider( surefireBooter, "surefire-junit47", surefireArtifact.getBaseVersion(), null );
                }
                else
                {
                    addProvider( surefireBooter, "surefire-junit4", surefireArtifact.getBaseVersion(), null );
                }
            }
            else
            {
                // add the JUnit provider as default - it doesn't require JUnit to be present,
                // since it supports POJO tests.
                addProvider( surefireBooter, "surefire-junit", surefireArtifact.getBaseVersion(), null );
            }
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new MojoExecutionException(
                "Unable to locate required surefire provider dependency: " + e.getMessage(), e );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new MojoExecutionException( "Error determining the TestNG version requested: " + e.getMessage(), e );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException( "Error to resolving surefire provider dependency: " + e.getMessage(), e );
        }

        if ( suiteXmlFiles != null && suiteXmlFiles.length > 0 && test == null )
        {
            if ( testNgArtifact == null )
            {
                throw new MojoExecutionException( "suiteXmlFiles is configured, but there is no TestNG dependency" );
            }

            // TODO: properties should be passed in here too
            surefireBooter.addTestSuite( "org.apache.maven.surefire.testng.TestNGXmlTestSuite",
                                         new Object[]{suiteXmlFiles, testSourceDirectory.getAbsolutePath(),
                                             testNgArtifact.getVersion(), testNgArtifact.getClassifier(), properties,
                                             reportsDirectory} );
        }
        else
        {
            List includes;
            List excludes;

            if ( test != null )
            {
                // Check to see if we are running a single test. The raw parameter will
                // come through if it has not been set.

                // FooTest -> **/FooTest.java

                includes = new ArrayList();

                excludes = new ArrayList();

                if ( failIfNoTests == null )
                {
                    failIfNoTests = Boolean.TRUE;
                }

                String[] testRegexes = StringUtils.split( test, "," );

                for ( int i = 0; i < testRegexes.length; i++ )
                {
                    String testRegex = testRegexes[i];
                    if ( testRegex.endsWith( ".java" ) )
                    {
                        testRegex = testRegex.substring( 0, testRegex.length() - 5 );
                    }
                    // Allow paths delimited by '.' or '/'
                    testRegex = testRegex.replace( '.', '/' );
                    includes.add( "**/" + testRegex + ".java" );
                }
            }
            else
            {
                includes = this.includes;

                excludes = this.excludes;

                // defaults here, qdox doesn't like the end javadoc value
                // Have to wrap in an ArrayList as surefire expects an ArrayList instead of a List for some reason
                if ( includes == null || includes.size() == 0 )
                {
                    includes = new ArrayList(
                        Arrays.asList( new String[]{"**/Test*.java", "**/*Test.java", "**/*TestCase.java"} ) );
                }
                if ( excludes == null || excludes.size() == 0 )
                {
                    excludes = new ArrayList( Arrays.asList( new String[]{"**/*$*"} ) );
                }
            }

            if ( testNgArtifact != null )
            {
                surefireBooter.addTestSuite( "org.apache.maven.surefire.testng.TestNGDirectoryTestSuite",
                                             new Object[]{testClassesDirectory, includes, excludes,
                                                 testSourceDirectory.getAbsolutePath(), testNgArtifact.getVersion(),
                                                 testNgArtifact.getClassifier(), properties, reportsDirectory} );
            }
            else
            {
                String junitDirectoryTestSuite;
                if ( isAnyConcurrencySelected() && isJunit47Compatible( junitArtifact ) )
                {
                    junitDirectoryTestSuite = "org.apache.maven.surefire.junitcore.JUnitCoreDirectoryTestSuite";
                    getLog().info( "Concurrency config is " + properties.toString() );
                    surefireBooter.addTestSuite( junitDirectoryTestSuite,
                                                 new Object[]{testClassesDirectory, includes, excludes, properties} );
                }
                else
                {
                    if ( isAnyJunit4( junitArtifact ) )
                    {
                        junitDirectoryTestSuite = "org.apache.maven.surefire.junit4.JUnit4DirectoryTestSuite";
                    }
                    else
                    {
                        // fall back to JUnit, which also contains POJO support. Also it can run
                        // classes compiled against JUnit since it has a dependency on JUnit itself.
                        junitDirectoryTestSuite = "org.apache.maven.surefire.junit.JUnitDirectoryTestSuite";
                    }
                    surefireBooter.addTestSuite( junitDirectoryTestSuite,
                                                 new Object[]{testClassesDirectory, includes, excludes} );
                }
            }
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        // Check if we need to add configured classes/test classes directories here.
        // If they are configured, we should remove the default to avoid conflicts.
        File projectClassesDirectory = new File( project.getBuild().getOutputDirectory() );
        if ( !projectClassesDirectory.equals( classesDirectory ) )
        {
            int indexToReplace = classpathElements.indexOf( project.getBuild().getOutputDirectory() );
            if ( indexToReplace != -1 )
            {
                classpathElements.remove( indexToReplace );
                classpathElements.add( indexToReplace, classesDirectory.getAbsolutePath() );
            }
            else
            {
                classpathElements.add( 1, classesDirectory.getAbsolutePath() );
            }
        }

        File projectTestClassesDirectory = new File( project.getBuild().getTestOutputDirectory() );
        if ( !projectTestClassesDirectory.equals( testClassesDirectory ) )
        {
            int indexToReplace = classpathElements.indexOf( project.getBuild().getTestOutputDirectory() );
            if ( indexToReplace != -1 )
            {
                classpathElements.remove( indexToReplace );
                classpathElements.add( indexToReplace, testClassesDirectory.getAbsolutePath() );
            }
            else
            {
                classpathElements.add( 0, testClassesDirectory.getAbsolutePath() );
            }
        }

        getLog().debug( "Test Classpath :" );

        for ( Iterator i = classpathElements.iterator(); i.hasNext(); )
        {
            String classpathElement = (String) i.next();

            getLog().debug( "  " + classpathElement );

            surefireBooter.addClassPathUrl( classpathElement );
        }

        Toolchain tc = getToolchain();

        if ( tc != null )
        {
            getLog().info( "Toolchain in surefire-plugin: " + tc );
            if ( ForkConfiguration.FORK_NEVER.equals( forkMode ) )
            {
                forkMode = ForkConfiguration.FORK_ONCE;
            }
            if ( jvm != null )
            {
                getLog().warn( "Toolchains are ignored, 'executable' parameter is set to " + jvm );
            }
            else
            {
                jvm = tc.findTool( "java" ); //NOI18N
            }
        }

        if ( additionalClasspathElements != null )
        {
            for ( Iterator i = additionalClasspathElements.iterator(); i.hasNext(); )
            {
                String classpathElement = (String) i.next();

                getLog().debug( "  " + classpathElement );

                surefireBooter.addClassPathUrl( classpathElement );
            }
        }

        // ----------------------------------------------------------------------
        // Forking
        // ----------------------------------------------------------------------

        ForkConfiguration fork = new ForkConfiguration();

        fork.setForkMode( forkMode );

        processSystemProperties( !fork.isForking() );

        if ( getLog().isDebugEnabled() )
        {
            showMap( internalSystemProperties, "system property" );
        }

        if ( fork.isForking() )
        {
            useSystemClassLoader = useSystemClassLoader == null ? Boolean.TRUE : useSystemClassLoader;
            fork.setUseSystemClassLoader( useSystemClassLoader.booleanValue() );
            fork.setUseManifestOnlyJar( useManifestOnlyJar );

            fork.setSystemProperties( internalSystemProperties );

            if ( "true".equals( debugForkedProcess ) )
            {
                debugForkedProcess =
                    "-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005";
            }

            fork.setDebugLine( debugForkedProcess );

            if ( jvm == null || "".equals( jvm ) )
            {
                // use the same JVM as the one used to run Maven (the "java.home" one)
                jvm = System.getProperty( "java.home" ) + File.separator + "bin" + File.separator + "java";
                getLog().debug( "Using JVM: " + jvm );
            }

            fork.setJvmExecutable( jvm );

            if ( workingDirectory != null )
            {
                fork.setWorkingDirectory( workingDirectory );
            }
            else
            {
                fork.setWorkingDirectory( basedir );
            }

            fork.setArgLine( argLine );

            fork.setEnvironmentVariables( environmentVariables );

            if ( getLog().isDebugEnabled() )
            {
                showMap( environmentVariables, "environment variable" );

                fork.setDebug( true );
            }

            if ( argLine != null )
            {
                List args = Arrays.asList( argLine.split( " " ) );
                if ( args.contains( "-da" ) || args.contains( "-disableassertions" ) )
                {
                    enableAssertions = false;
                }
            }
        }

        surefireBooter.setFailIfNoTests( failIfNoTests == null ? false : failIfNoTests.booleanValue() );

        surefireBooter.setForkedProcessTimeoutInSeconds( forkedProcessTimeoutInSeconds );

        surefireBooter.setRedirectTestOutputToFile( redirectTestOutputToFile );

        surefireBooter.setForkConfiguration( fork );

        surefireBooter.setChildDelegation( childDelegation );

        surefireBooter.setEnableAssertions( enableAssertions );

        surefireBooter.setReportsDirectory( reportsDirectory );

        addReporters( surefireBooter, fork.isForking() );

        return surefireBooter;
    }

    private void showMap( Map map, String setting )
    {
        for ( Iterator i = map.keySet().iterator(); i.hasNext(); )
        {
            String key = (String) i.next();
            String value = (String) map.get( key );
            getLog().debug( "Setting " + setting + " [" + key + "]=[" + value + "]" );
        }
    }

    private void addProvider( SurefireBooter surefireBooter, String provider, String version,
                              Artifact filteredArtifact )
        throws ArtifactNotFoundException, ArtifactResolutionException
    {
        Artifact providerArtifact = artifactFactory.createDependencyArtifact( "org.apache.maven.surefire", provider,
                                                                              VersionRange.createFromVersion( version ),
                                                                              "jar", null, Artifact.SCOPE_TEST );
        ArtifactResolutionResult result = resolveArtifact( filteredArtifact, providerArtifact );

        for ( Iterator i = result.getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            getLog().debug( "Adding to surefire test classpath: " + artifact.getFile().getAbsolutePath() );

            surefireBooter.addSurefireClassPathUrl( artifact.getFile().getAbsolutePath() );
        }
    }

    private ArtifactResolutionResult resolveArtifact( Artifact filteredArtifact, Artifact providerArtifact )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        ArtifactFilter filter = null;
        if ( filteredArtifact != null )
        {
            filter = new ExcludesArtifactFilter(
                Collections.singletonList( filteredArtifact.getGroupId() + ":" + filteredArtifact.getArtifactId() ) );
        }

        Artifact originatingArtifact = artifactFactory.createBuildArtifact( "dummy", "dummy", "1.0", "jar" );

        return artifactResolver.resolveTransitively( Collections.singleton( providerArtifact ), originatingArtifact,
                                                     localRepository, remoteRepositories, metadataSource, filter );
    }

    private void addArtifact( SurefireBooter surefireBooter, Artifact surefireArtifact )
        throws ArtifactNotFoundException, ArtifactResolutionException
    {
        ArtifactResolutionResult result = resolveArtifact( null, surefireArtifact );

        for ( Iterator i = result.getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            getLog().debug( "Adding to surefire booter test classpath: " + artifact.getFile().getAbsolutePath() );

            surefireBooter.addSurefireBootClassPathUrl( artifact.getFile().getAbsolutePath() );
        }
    }

    protected void processSystemProperties( boolean setInSystem )
    {
        if ( this.systemProperties != null )
        {
            for ( Iterator i = systemProperties.keySet().iterator(); i.hasNext(); )
            {
                String key = (String) i.next();
                String value = (String) systemProperties.get( key );
                internalSystemProperties.setProperty( key, value );
            }
        }

        if ( this.systemPropertyVariables != null )
        {
            for ( Iterator i = systemPropertyVariables.keySet().iterator(); i.hasNext(); )
            {
                String key = (String) i.next();
                String value = (String) systemPropertyVariables.get( key );
                //java Properties does not accept null value
                if ( value != null )
                {
                    internalSystemProperties.setProperty( key, value );
                }
            }
        }

        originalSystemProperties = (Properties) System.getProperties().clone();

        // We used to take all of our system properties and dump them in with the
        // user specified properties for SUREFIRE-121, causing SUREFIRE-491.
        // Not gonna do THAT any more... but I'm leaving this code here in case
        // we need it later when we try to fix SUREFIRE-121 again.

        // Get the properties from the MavenSession instance to make embedded use work correctly
        Properties userSpecifiedProperties = (Properties) session.getExecutionProperties().clone();
        userSpecifiedProperties.putAll( internalSystemProperties );
        //systemProperties = userSpecifiedProperties;

        internalSystemProperties.setProperty( "basedir", basedir.getAbsolutePath() );
        internalSystemProperties.setProperty( "user.dir", workingDirectory.getAbsolutePath() );

        internalSystemProperties.setProperty( "localRepository", localRepository.getBasedir() );

        if ( setInSystem )
        {
            // Add all system properties configured by the user
            Iterator iter = internalSystemProperties.keySet().iterator();

            while ( iter.hasNext() )
            {
                String key = (String) iter.next();

                String value = internalSystemProperties.getProperty( key );

                System.setProperty( key, value );
            }
        }
    }

    /**
     * <p/>
     * Adds Reporters that will generate reports with different formatting.
     * <p/>
     * The Reporter that will be added will be based on the value of the parameter useFile, reportFormat, and
     * printSummary.
     *
     * @param surefireBooter The surefire booter that will run tests.
     * @param forking
     */
    private void addReporters( SurefireBooter surefireBooter, boolean forking )
    {
        Boolean trimStackTrace = Boolean.valueOf( this.trimStackTrace );
        if ( useFile )
        {
            if ( printSummary )
            {
                if ( forking )
                {
                    surefireBooter.addReport( ForkingConsoleReporter.class.getName(), new Object[]{trimStackTrace} );
                }
                else
                {
                    surefireBooter.addReport( ConsoleReporter.class.getName(), new Object[]{trimStackTrace} );
                }
            }

            if ( BRIEF_REPORT_FORMAT.equals( reportFormat ) )
            {
                surefireBooter.addReport( BriefFileReporter.class.getName(),
                                          new Object[]{reportsDirectory, trimStackTrace} );
            }
            else if ( PLAIN_REPORT_FORMAT.equals( reportFormat ) )
            {
                surefireBooter.addReport( FileReporter.class.getName(),
                                          new Object[]{reportsDirectory, trimStackTrace} );
            }
        }
        else
        {
            if ( BRIEF_REPORT_FORMAT.equals( reportFormat ) )
            {
                surefireBooter.addReport( BriefConsoleReporter.class.getName(), new Object[]{trimStackTrace} );
            }
            else if ( PLAIN_REPORT_FORMAT.equals( reportFormat ) )
            {
                surefireBooter.addReport( DetailedConsoleReporter.class.getName(), new Object[]{trimStackTrace} );
            }
        }

        if ( !disableXmlReport )
        {
            surefireBooter.addReport( XMLReporter.class.getName(), new Object[]{reportsDirectory, trimStackTrace} );
        }
    }

    /**
     * @return SurefirePlugin Returns the skipExec.
     */
    public boolean isSkipExec()
    {
        return this.skipTests;
    }

    /**
     * @param skipExec the skipExec to set
     */
    public void setSkipExec( boolean skipExec )
    {
        this.skipTests = skipExec;
    }

    private Toolchain getToolchain()
    {
        Toolchain tc = null;

        if ( toolchainManager != null )
        {
            tc = toolchainManager.getToolchainFromBuildContext( "jdk", session );
        }

        return tc;
    }
}
