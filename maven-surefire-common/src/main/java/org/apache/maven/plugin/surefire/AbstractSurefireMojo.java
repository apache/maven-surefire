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
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
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
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.surefire.booterclient.ChecksumCalculator;
import org.apache.maven.plugin.surefire.booterclient.ForkConfiguration;
import org.apache.maven.plugin.surefire.booterclient.ForkStarter;
import org.apache.maven.plugin.surefire.util.DirectoryScanner;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.PatternIncludesArtifactFilter;
import org.apache.maven.surefire.booter.ClassLoaderConfiguration;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.ClasspathConfiguration;
import org.apache.maven.surefire.booter.ProviderConfiguration;
import org.apache.maven.surefire.booter.ProviderParameterNames;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.SurefireBooterForkException;
import org.apache.maven.surefire.booter.SurefireExecutionException;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.RunOrderParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestRequest;
import org.apache.maven.surefire.util.DefaultScanResult;
import org.apache.maven.surefire.util.NestedRuntimeException;
import org.apache.maven.surefire.util.RunOrder;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * Abstract base class for running tests using Surefire.
 *
 * @author Stephen Connolly
 * @version $Id: SurefirePlugin.java 945065 2010-05-17 10:26:22Z stephenc $
 */
public abstract class AbstractSurefireMojo
    extends AbstractMojo
    implements SurefireExecutionParameters
{

    // common mojo parameters

    /**
     * Information about this plugin, mainly used to lookup this plugin's configuration from the currently executing
     * project.
     *
     * @since 2.12
     */
    @Parameter( defaultValue = "${plugin}", readonly = true )
    protected PluginDescriptor pluginDescriptor;

    /**
     * Set this to "true" to skip running tests, but still compile them. Its use is NOT RECOMMENDED, but quite
     * convenient on occasion.
     *
     * @since 2.4
     */
    @Parameter( property = "skipTests", defaultValue = "false" )
    protected boolean skipTests;

    /**
     * This old parameter is just like <code>skipTests</code>, but bound to the old property "maven.test.skip.exec".
     *
     * @since 2.3
     * @deprecated Use skipTests instead.
     */
    @Parameter( property = "maven.test.skip.exec" )
    protected boolean skipExec;

    /**
     * Set this to "true" to bypass unit tests entirely. Its use is NOT RECOMMENDED, especially if you enable it using
     * the "maven.test.skip" property, because maven.test.skip disables both running the tests and compiling the tests.
     * Consider using the <code>skipTests</code> parameter instead.
     */
    @Parameter( property = "maven.test.skip", defaultValue = "false" )
    protected boolean skip;

    /**
     * The Maven Project Object.
     */
    @Component
    protected MavenProject project;

    /**
     * The base directory of the project being tested. This can be obtained in your integration test via
     * System.getProperty("basedir").
     */
    @Parameter( defaultValue = "${basedir}" )
    protected File basedir;

    /**
     * The directory containing generated test classes of the project being tested. This will be included at the
     * beginning of the test classpath. *
     */
    @Parameter( defaultValue = "${project.build.testOutputDirectory}" )
    protected File testClassesDirectory;

    /**
     * The directory containing generated classes of the project being tested. This will be included after the test
     * classes in the test classpath.
     */
    @Parameter( defaultValue = "${project.build.outputDirectory}" )
    protected File classesDirectory;

    /**
     * List of dependencies to exclude from the test classpath. Each dependency string must follow the format
     * <i>groupId:artifactId</i>. For example: <i>org.acme:project-a</i>
     *
     * @since 2.6
     */
    @Parameter
    protected List<String> classpathDependencyExcludes;

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
    protected String classpathDependencyScopeExclude;

    /**
     * Additional elements to be appended to the classpath.
     *
     * @since 2.4
     */
    @Parameter
    protected List<String> additionalClasspathElements;

    /**
     * The test source directory containing test class sources.
     *
     * @since 2.2
     */
    @Parameter( defaultValue = "${project.build.testSourceDirectory}", required = true )
    protected File testSourceDirectory;

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
    protected List<String> includes;

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
    protected List<String> excludes;

    /**
     * ArtifactRepository of the localRepository. To obtain the directory of localRepository in unit tests use
     * System.getProperty("localRepository").
     */
    @Parameter( defaultValue = "${localRepository}", required = true, readonly = true )
    protected ArtifactRepository localRepository;

    /**
     * List of System properties to pass to the JUnit tests.
     *
     * @deprecated Use systemPropertyVariables instead.
     */
    @Parameter
    protected Properties systemProperties;

    /**
     * List of System properties to pass to the JUnit tests.
     *
     * @since 2.5
     */
    @Parameter
    protected Map<String, String> systemPropertyVariables;

    /**
     * List of System properties, loaded from a file, to pass to the JUnit tests.
     *
     * @since 2.8.2
     */
    @Parameter
    protected File systemPropertiesFile;

    /**
     * List of properties for configuring all TestNG related configurations. This is the new preferred method of
     * configuring TestNG.
     *
     * @since 2.4
     */
    @Parameter
    protected Properties properties;

    /**
     * Map of plugin artifacts.
     */
    // olamy: would make more sense using defaultValue but doesn't work with maven 2.x
    @Parameter( property = "plugin.artifactMap", required = true, readonly = true )
    protected Map<String, Artifact> pluginArtifactMap;

    /**
     * Map of project artifacts.
     */
    // olamy: would make more sense using defaultValue but doesn't work with maven 2.x
    @Parameter( property = "project.artifactMap", readonly = true, required = true )
    protected Map<String, Artifact> projectArtifactMap;

    /**
     * Add custom text into report filename: TEST-testClassName-reportNameSuffix.xml,
     * testClassName-reportNameSuffix.txt and testClassName-reportNameSuffix-output.txt.
     * File TEST-testClassName-reportNameSuffix.xml has changed attributes 'testsuite'--'name'
     * and 'testcase'--'classname' - reportNameSuffix is added to the attribute value.
     */
    @Parameter( property = "surefire.reportNameSuffix", defaultValue = "" )
    protected String reportNameSuffix;

    /**
     * Set this to "true" to redirect the unit test standard output to a file (found in
     * reportsDirectory/testName-output.txt).
     *
     * @since 2.3
     */
    @Parameter( property = "maven.test.redirectTestOutputToFile", defaultValue = "false" )
    protected boolean redirectTestOutputToFile;

    /**
     * Set this to "true" to cause a failure if there are no tests to run. Defaults to "false".
     *
     * @since 2.4
     */
    @Parameter( property = "failIfNoTests" )
    protected Boolean failIfNoTests;

    /**
     * Option to specify the forking mode. Can be "never", "once", "always" or "perthread". "none" and "pertest" are also accepted
     * for backwards compatibility. "always" forks for each test-class. "perthread" will create "threadCount" parallel forks.
     *
     * @since 2.1
     */
    @Parameter( property = "forkMode", defaultValue = "once" )
    protected String forkMode;

    /**
     * Option to specify the jvm (or path to the java executable) to use with the forking options. For the default, the
     * jvm will be a new instance of the same VM as the one used to run Maven. JVM settings are not inherited from
     * MAVEN_OPTS.
     *
     * @since 2.1
     */
    @Parameter( property = "jvm" )
    protected String jvm;

    /**
     * Arbitrary JVM options to set on the command line.
     *
     * @since 2.1
     */
    @Parameter( property = "argLine" )
    protected String argLine;

    /**
     * Additional environment variables to set on the command line.
     *
     * @since 2.1.3
     */
    @Parameter
    protected Map<String, String> environmentVariables = new HashMap<String, String>();

    /**
     * Command line working directory.
     *
     * @since 2.1.3
     */
    @Parameter( property = "basedir" )
    protected File workingDirectory;

    /**
     * When false it makes tests run using the standard classloader delegation instead of the default Maven isolated
     * classloader. Only used when forking (forkMode is not "none").<br/>
     * Setting it to false helps with some problems caused by conflicts between xml parsers in the classpath and the
     * Java 5 provider parser.
     *
     * @since 2.1
     */
    @Parameter( property = "childDelegation", defaultValue = "false" )
    protected boolean childDelegation;

    /**
     * (TestNG/JUnit47 provider with JUnit4.8+ only) Groups for this test. Only classes/methods/etc decorated with one of the groups specified here will
     * be included in test run, if specified.<br/>For JUnit, this parameter forces the use of the 4.7 provider<br/>
     * This parameter is ignored if the <code>suiteXmlFiles</code> parameter is specified.
     *
     * @since 2.2
     */
    @Parameter( property = "groups" )
    protected String groups;

    /**
     * (TestNG/JUnit47 provider with JUnit4.8+ only) Excluded groups. Any methods/classes/etc with one of the groups specified in this list will
     * specifically not be run.<br/>For JUnit, this parameter forces the use of the 4.7 provider<br/>
     * This parameter is ignored if the <code>suiteXmlFiles</code> parameter is specified.
     *
     * @since 2.2
     */
    @Parameter( property = "excludedGroups" )
    protected String excludedGroups;

    /**
     * (TestNG) List of &lt;suiteXmlFile> elements specifying TestNG suite xml file locations. Note that
     * <code>suiteXmlFiles</code> is incompatible with several other parameters of this plugin, like
     * <code>includes/excludes</code>.<br/>
     * This parameter is ignored if the <code>test</code> parameter is specified (allowing you to run a single test
     * instead of an entire suite).
     *
     * @since 2.2
     */
    @Parameter
    protected File[] suiteXmlFiles;

    /**
     * Allows you to specify the name of the JUnit artifact. If not set, <code>junit:junit</code> will be used.
     *
     * @since 2.3.1
     */
    @Parameter( property = "junitArtifactName", defaultValue = "junit:junit" )
    protected String junitArtifactName;

    /**
     * Allows you to specify the name of the TestNG artifact. If not set, <code>org.testng:testng</code> will be used.
     *
     * @since 2.3.1
     */
    @Parameter( property = "testNGArtifactName", defaultValue = "org.testng:testng" )
    protected String testNGArtifactName;

    /**
     * (forkMode=perthread or TestNG/JUnit 4.7 provider) The attribute thread-count allows you to specify how many threads should be
     * allocated for this execution. Only makes sense to use in conjunction with the <code>parallel</code> parameter. (forkMode=perthread
     * does not support/require the <code>parallel</code> parameter)
     *
     * @since 2.2
     */
    @Parameter( property = "threadCount" )
    protected int threadCount;

    /**
     * (JUnit 4.7 provider) Indicates that threadCount is per cpu core.
     *
     * @since 2.5
     */
    @Parameter( property = "perCoreThreadCount", defaultValue = "true" )
    protected boolean perCoreThreadCount;

    /**
     * (JUnit 4.7 provider) Indicates that the thread pool will be unlimited. The <code>parallel</code> parameter and
     * the actual number of classes/methods will decide. Setting this to "true" effectively disables
     * <code>perCoreThreadCount</code> and <code>threadCount</code>. Defaults to "false".
     *
     * @since 2.5
     */
    @Parameter( property = "useUnlimitedThreads", defaultValue = "false" )
    protected boolean useUnlimitedThreads;

    /**
     * (TestNG only) When you use the <code>parallel</code> attribute, TestNG will try to run all your test methods in
     * separate threads, except for methods that depend on each other, which will be run in the same thread in order to
     * respect their order of execution.
     * <p/>
     * (JUnit 4.7 provider) Supports values "classes"/"methods"/"both" to run in separate threads, as controlled by
     * <code>threadCount</code>.
     *
     * @since 2.2
     */
    @Parameter( property = "parallel" )
    protected String parallel;

    /**
     * Whether to trim the stack trace in the reports to just the lines within the test, or show the full trace.
     *
     * @since 2.2
     */
    @Parameter( property = "trimStackTrace", defaultValue = "true" )
    protected boolean trimStackTrace;

    /**
     * Resolves the artifacts needed.
     */
    @Component
    protected ArtifactResolver artifactResolver;

    /**
     * Creates the artifact.
     */
    @Component
    protected ArtifactFactory artifactFactory;

    /**
     * The remote plugin repositories declared in the POM.
     *
     * @since 2.2
     */
    @Parameter( defaultValue = "${project.pluginArtifactRepositories}" )
    protected List<ArtifactRepository> remoteRepositories;

    /**
     * For retrieval of artifact's metadata.
     */
    @Component
    protected ArtifactMetadataSource metadataSource;

    /**
     * Flag to disable the generation of report files in xml format.
     *
     * @since 2.2
     */
    @Parameter( property = "disableXmlReport", defaultValue = "false" )
    protected boolean disableXmlReport;

    /**
     * By default, Surefire enables JVM assertions for the execution of your test cases. To disable the assertions, set
     * this flag to "false".
     *
     * @since 2.3.1
     */
    @Parameter( property = "enableAssertions", defaultValue = "true" )
    protected boolean enableAssertions;

    /**
     * The current build session instance.
     */
    @Component
    protected MavenSession session;

    /**
     * (TestNG only) Define the factory class used to create all test instances.
     *
     * @since 2.5
     */
    @Parameter( property = "objectFactory" )
    protected String objectFactory;

    /**
     *
     */
    @Parameter( defaultValue = "${session.parallel}", readonly = true )
    protected Boolean parallelMavenExecution;

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
    protected String runOrder;

    /**
     *
     */
    @Component
    protected ToolchainManager toolchainManager;

    private Artifact surefireBooterArtifact;

    private Toolchain toolchain;

    protected abstract String getPluginName();

    private SurefireDependencyResolver dependencyResolver;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        // Stuff that should have been final
        setupStuff();

        if ( verifyParameters() && !hasExecutedBefore() )
        {
            DefaultScanResult scan = scanDirectories();
            if ( scan.isEmpty() )
            {
                if ( getEffectiveFailIfNoTests() )
                {
                    throw new MojoFailureException(
                        "No tests were executed!  (Set -DfailIfNoTests=false to ignore this error.)" );
                }
                handleSummary( Summary.notests() );
                return;
            }
            logReportsDirectory();
            executeAfterPreconditionsChecked( scan );
        }
    }

    private void setupStuff()
    {
        createDependencyResolver();
        surefireBooterArtifact = getSurefireBooterArtifact();
        toolchain = getToolchain();
    }

    private DefaultScanResult scanDirectories()
    {
        return new DirectoryScanner( getTestClassesDirectory(), getIncludeList(), getExcludeList(),
                                     getSpecificTests() ).scan();
    }

    boolean verifyParameters()
        throws MojoFailureException
    {
        setProperties( new OrderedProperties( getProperties() ) );
        if ( isSkipExecution() )
        {
            getLog().info( "Tests are skipped." );
            return false;
        }

        String jvmToUse = getJvm();
        if ( toolchain != null )
        {
            getLog().info( "Toolchain in " + getPluginName() + "-plugin: " + toolchain );
            if ( jvmToUse != null )
            {
                getLog().warn( "Toolchains are ignored, 'executable' parameter is set to " + jvmToUse );
            }
        }

        if ( !getTestClassesDirectory().exists() )
        {
            if ( Boolean.TRUE.equals( getFailIfNoTests() ) )
            {
                throw new MojoFailureException( "No tests to run!" );
            }
            getLog().info( "No tests to run." );
        }
        else
        {
            ensureWorkingDirectoryExists();
            ensureParallelRunningCompatibility();
            warnIfUselessUseSystemClassLoaderParameter();
        }
        return true;
    }

    protected abstract boolean isSkipExecution();

    protected void executeAfterPreconditionsChecked( DefaultScanResult scanResult )
        throws MojoExecutionException, MojoFailureException
    {

        Summary summary = executeAllProviders( scanResult );

        handleSummary( summary );
    }


    private void createDependencyResolver()
    {
        dependencyResolver =
            new SurefireDependencyResolver( getArtifactResolver(), getArtifactFactory(), getLog(), getLocalRepository(),
                                            getRemoteRepositories(), getMetadataSource(), getPluginName() );
    }

    protected List<ProviderInfo> createProviders()
        throws MojoFailureException
    {
        try
        {
            final Artifact junitDepArtifact = getJunitDepArtifact();
            ProviderList wellKnownProviders =
                new ProviderList( new DynamicProviderInfo( null ), new TestNgProviderInfo( getTestNgArtifact() ),
                                  new JUnitCoreProviderInfo( getJunitArtifact(), junitDepArtifact ),
                                  new JUnit4ProviderInfo( getJunitArtifact(), junitDepArtifact ),
                                  new JUnit3ProviderInfo() );

            return wellKnownProviders.resolve( getLog() );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new NestedRuntimeException( e );
        }
    }

    private Summary executeAllProviders( DefaultScanResult scanResult )
        throws MojoExecutionException, MojoFailureException
    {
        List<ProviderInfo> providers = createProviders();
        Summary summary = new Summary();

        for ( ProviderInfo provider : providers )
        {
            executeProvider( provider, scanResult, summary );
        }
        return summary;
    }

    private SurefireProperties setupProperties()
    {
        SurefireProperties result =
            SurefireProperties.calculateEffectiveProperties( getSystemProperties(), getSystemPropertiesFile(),
                                                             getSystemPropertyVariables(), getUserProperties(),
                                                             getLog() );

        result.setProperty( "basedir", getBasedir().getAbsolutePath() );
        result.setProperty( "user.dir", getWorkingDirectory().getAbsolutePath() );
        result.setProperty( "localRepository", getLocalRepository().getBasedir() );

        result.verifyLegalSystemProperties( getLog() );
        if ( getLog().isDebugEnabled() )
        {
            result.showToLog( getLog(), "system property" );
        }
        return result;
    }

    private void executeProvider( ProviderInfo provider, DefaultScanResult scanResult, Summary summary )
        throws MojoExecutionException, MojoFailureException
    {
        SurefireProperties effectiveProperties = setupProperties();
        summary.reportForkConfiguration( isForking() );
        ClassLoaderConfiguration classLoaderConfiguration = getClassLoaderConfiguration( isForking() );

        try
        {
            RunOrderParameters runOrderParameters =
                new RunOrderParameters( getRunOrder(), getStatisticsFileName( getConfigChecksum() ) );

            final RunResult result;
            if ( isForkModeNever() )
            {
                effectiveProperties.copyToSystemProperties();
                InPluginVMSurefireStarter surefireStarter =
                    createInprocessStarter( provider, classLoaderConfiguration, runOrderParameters );
                result = surefireStarter.runSuitesInProcess( scanResult );
            }
            else
            {
                ForkConfiguration forkConfiguration = getForkConfiguration();
                if ( getLog().isDebugEnabled() )
                {
                    showMap( getEnvironmentVariables(), "environment variable" );
                }


                Properties originalSystemProperties = (Properties) System.getProperties().clone();
                try
                {
                    ForkStarter forkStarter =
                        createForkStarter( provider, forkConfiguration, classLoaderConfiguration, runOrderParameters,
                                           effectiveProperties );
                    result = forkStarter.run( scanResult, getEffectiveForkMode() );
                }
                finally
                {
                    System.setProperties( originalSystemProperties );
                    cleanupForkConfiguration( forkConfiguration );
                }
            }
            summary.registerRunResult( result );
        }
        catch ( SurefireBooterForkException e )
        {
            summary.registerException( e );
        }
        catch ( SurefireExecutionException e )
        {
            summary.registerException( e );
        }
    }

    protected void cleanupForkConfiguration( ForkConfiguration forkConfiguration )
    {
        if ( !getLog().isDebugEnabled() && forkConfiguration != null )
        {
            File tempDirectory = forkConfiguration.getTempDirectory();
            try
            {
                FileUtils.deleteDirectory( tempDirectory );
            }
            catch ( IOException ioe )
            {
                getLog().warn( "Could not delete temp direcotry " + tempDirectory + " because " + ioe.getMessage() );
            }
        }
    }

    protected abstract void handleSummary( Summary summary )
        throws MojoExecutionException, MojoFailureException;

    protected void logReportsDirectory()
    {
        getLog().info(
            StringUtils.capitalizeFirstLetter( getPluginName() ) + " report directory: " + getReportsDirectory() );
    }


    final Toolchain getToolchain()
    {
        Toolchain tc = null;

        if ( getToolchainManager() != null )
        {
            tc = getToolchainManager().getToolchainFromBuildContext( "jdk", getSession() );
        }

        return tc;
    }

    /**
     * Converts old TestNG configuration parameters over to new properties based configuration
     * method. (if any are defined the old way)
     */
    private void convertTestNGParameters()
    {
        if ( this.getParallel() != null )
        {
            getProperties().setProperty( ProviderParameterNames.PARALLEL_PROP, this.getParallel() );
        }
        convertGroupParameters();

        if ( this.getThreadCount() > 0 )
        {
            getProperties().setProperty( ProviderParameterNames.THREADCOUNT_PROP,
                                         Integer.toString( this.getThreadCount() ) );
        }
        if ( this.getObjectFactory() != null )
        {
            getProperties().setProperty( "objectfactory", this.getObjectFactory() );
        }
        if ( this.getTestClassesDirectory() != null )
        {
            getProperties().setProperty( "testng.test.classpath", getTestClassesDirectory().getAbsolutePath() );
        }


    }

    private void convertGroupParameters()
    {
        if ( this.getExcludedGroups() != null )
        {
            getProperties().setProperty( ProviderParameterNames.TESTNG_EXCLUDEDGROUPS_PROP, this.getExcludedGroups() );
        }
        if ( this.getGroups() != null )
        {
            getProperties().setProperty( ProviderParameterNames.TESTNG_GROUPS_PROP, this.getGroups() );
        }
    }

    protected boolean isAnyConcurrencySelected()
    {
        return this.getParallel() != null && this.getParallel().trim().length() > 0;
    }

    protected boolean isAnyGroupsSelected()
    {
        return this.getGroups() != null || this.getExcludedGroups() != null;
    }

    /**
     * Converts old JUnit configuration parameters over to new properties based configuration
     * method. (if any are defined the old way)
     */
    private void convertJunitCoreParameters()
    {
        if ( this.getParallel() != null )
        {
            getProperties().setProperty( ProviderParameterNames.PARALLEL_PROP, this.getParallel() );
        }
        if ( this.getThreadCount() > 0 )
        {
            getProperties().setProperty( ProviderParameterNames.THREADCOUNT_PROP,
                                         Integer.toString( this.getThreadCount() ) );
        }
        getProperties().setProperty( "perCoreThreadCount", Boolean.toString( getPerCoreThreadCount() ) );
        getProperties().setProperty( "useUnlimitedThreads", Boolean.toString( getUseUnlimitedThreads() ) );
    }

    private boolean isJunit47Compatible( Artifact artifact )
    {
        return dependencyResolver.isWithinVersionSpec( artifact, "[4.7,)" );
    }

    private boolean isAnyJunit4( Artifact artifact )
    {
        return dependencyResolver.isWithinVersionSpec( artifact, "[4.0,)" );
    }

    boolean isForkModeNever()
    {
        return ForkConfiguration.FORK_NEVER.equals( getEffectiveForkMode() );
    }

    boolean isForking()
    {
        return !isForkModeNever();
    }

    private String getEffectiveForkMode()
    {
        String forkMode1 = getForkMode();

        if ( toolchain != null && isForkModeNever() )
        {
            return ForkConfiguration.FORK_ONCE;
        }

        return ForkConfiguration.getEffectiveForkMode( forkMode1 );
    }

    private List<RunOrder> getRunOrders()
    {
        String runOrderString = getRunOrder();
        RunOrder[] runOrder = runOrderString == null ? RunOrder.DEFAULT : RunOrder.valueOfMulti( runOrderString );
        return Arrays.asList( runOrder );
    }

    private boolean requiresRunHistory()
    {
        final List<RunOrder> runOrders = getRunOrders();
        return runOrders.contains( RunOrder.BALANCED ) || runOrders.contains( RunOrder.FAILEDFIRST );
    }

    private boolean getEffectiveFailIfNoTests()
    {
        if ( isSpecificTestSpecified() )
        {
            if ( getFailIfNoSpecifiedTests() != null )
            {
                return getFailIfNoSpecifiedTests();
            }
            else if ( getFailIfNoTests() != null )
            {
                return getFailIfNoTests();
            }
            else
            {
                return true;
            }
        }
        else
        {
            return getFailIfNoTests() != null && getFailIfNoTests();
        }
    }

    private ProviderConfiguration createProviderConfiguration( RunOrderParameters runOrderParameters )
        throws MojoExecutionException, MojoFailureException
    {
        ReporterConfiguration reporterConfiguration =
            new ReporterConfiguration( getReportsDirectory(), isTrimStackTrace() );

        Artifact testNgArtifact;
        try
        {
            testNgArtifact = getTestNgArtifact();
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new MojoExecutionException( "Error determining the TestNG version requested: " + e.getMessage(), e );
        }

        DirectoryScannerParameters directoryScannerParameters = null;
        final boolean isTestNg = testNgArtifact != null;
        TestArtifactInfo testNg =
            isTestNg ? new TestArtifactInfo( testNgArtifact.getVersion(), testNgArtifact.getClassifier() ) : null;
        List<File> testXml = getSuiteXmlFiles() != null ? Arrays.asList( getSuiteXmlFiles() ) : null;
        TestRequest testSuiteDefinition =
            new TestRequest( testXml, getTestSourceDirectory(), getTest(), getTestMethod() );
        final boolean failIfNoTests;

        if ( isValidSuiteXmlFileConfig() && getTest() == null )
        {
            failIfNoTests = getFailIfNoTests() != null && getFailIfNoTests();
            if ( !isTestNg )
            {
                throw new MojoExecutionException( "suiteXmlFiles is configured, but there is no TestNG dependency" );
            }
        }
        else
        {
            if ( isSpecificTestSpecified() )
            {
                failIfNoTests = getEffectiveFailIfNoTests();
                setFailIfNoTests( failIfNoTests );
            }
            else
            {
                failIfNoTests = getFailIfNoTests() != null && getFailIfNoTests();
            }

            List<String> includes = getIncludeList();
            List<String> excludes = getExcludeList();
            List<String> specificTests = getSpecificTests();
            directoryScannerParameters =
                new DirectoryScannerParameters( getTestClassesDirectory(), includes, excludes, specificTests,
                                                failIfNoTests, getRunOrder() );
        }

        Properties providerProperties = getProperties();

        return new ProviderConfiguration( directoryScannerParameters, runOrderParameters, failIfNoTests,
                                          reporterConfiguration, testNg, testSuiteDefinition, providerProperties,
                                          null );
    }

    public String getStatisticsFileName( String configurationHash )
    {
        return getReportsDirectory().getParentFile().getParentFile() + File.separator + ".surefire-"
            + configurationHash;
    }


    StartupConfiguration createStartupConfiguration( ProviderInfo provider,
                                                     ClassLoaderConfiguration classLoaderConfiguration )
        throws MojoExecutionException, MojoFailureException
    {

        try
        {
            provider.addProviderProperties();
            String providerName = provider.getProviderName();
            Classpath providerClasspath = provider.getProviderClasspath();
            Classpath inprocClassPath = new Classpath( providerClasspath );
            Artifact surefireArtifact = getCommonArtifact();
            inprocClassPath.addClassPathElementUrl( surefireArtifact.getFile().getAbsolutePath() );

            final Classpath testClasspath = generateTestClasspath();

            logClasspath( testClasspath, "test classpath" );
            logClasspath( providerClasspath, "provider classpath" );
            final ClasspathConfiguration classpathConfiguration =
                new ClasspathConfiguration( testClasspath, providerClasspath, inprocClassPath, effectiveIsEnableAssertions(),
                                            isChildDelegation() );

            return new StartupConfiguration( providerName, classpathConfiguration, classLoaderConfiguration,
                                             isForking(), false );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException( "Unable to generate classpath: " + e, e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new MojoExecutionException( "Unable to generate classpath: " + e, e );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new MojoExecutionException( "Unable to generate classpath: " + e, e );
        }

    }

    private Artifact getCommonArtifact()
    {
        return getPluginArtifactMap().get( "org.apache.maven.surefire:maven-surefire-common" );
    }

    private StartupReportConfiguration getStartupReportConfiguration( String configChecksum )
    {
        return new StartupReportConfiguration( isUseFile(), isPrintSummary(), getReportFormat(),
                                               isRedirectTestOutputToFile(), isDisableXmlReport(),
                                               getReportsDirectory(), isTrimStackTrace(), getReportNameSuffix(),
                                               configChecksum, requiresRunHistory() );
    }

    void logClasspath( Classpath classpath, String descriptor )
    {
        getLog().debug( descriptor + " classpath:" );
        @SuppressWarnings( "unchecked" ) final List<String> classPath = classpath.getClassPath();
        for ( String classpathElement : classPath )
        {
            if ( classpathElement == null )
            {
                getLog().warn( "The test classpath contains a null element." );
            }
            else
            {
                getLog().debug( "  " + classpathElement );
            }
        }
    }


    private boolean isSpecificTestSpecified()
    {
        return getTest() != null;
    }

    private boolean isValidSuiteXmlFileConfig()
    {
        return getSuiteXmlFiles() != null && getSuiteXmlFiles().length > 0;
    }

    private List<String> getExcludeList()
    {
        List<String> excludes;
        if ( isSpecificTestSpecified() )
        {
            // Check to see if we are running a single test. The raw parameter will
            // come through if it has not been set.
            // FooTest -> **/FooTest.java

            excludes = new ArrayList<String>();
        }
        else
        {

            excludes = this.getExcludes();

            // defaults here, qdox doesn't like the end javadoc value
            // Have to wrap in an ArrayList as surefire expects an ArrayList instead of a List for some reason
            if ( excludes == null || excludes.size() == 0 )
            {
                excludes = new ArrayList<String>( Arrays.asList( new String[]{ "**/*$*" } ) );
            }
        }
        return excludes;
    }

    private List<String> getIncludeList()
    {
        List<String> includes;
        if ( isSpecificTestSpecified() && !isMultipleExecutionBlocksDetected() )
        {
            includes = getSpecificTests();
        }
        else
        {
            includes = this.getIncludes();
        }

        // defaults here, qdox doesn't like the end javadoc value
        // Have to wrap in an ArrayList as surefire expects an ArrayList instead of a List for some reason
        if ( includes == null || includes.size() == 0 )
        {
            includes = new ArrayList<String>( Arrays.asList( getDefaultIncludes() ) );
        }

        return includes;
    }

    private boolean isMultipleExecutionBlocksDetected()
    {
        MavenProject project = getProject();
        if ( project != null )
        {
            String key = getPluginDescriptor().getPluginLookupKey();
            Plugin plugin = (Plugin) project.getBuild().getPluginsAsMap().get( key );

            if ( plugin != null )
            {
                @SuppressWarnings( "rawtypes" ) List executions = plugin.getExecutions();
                return executions != null && executions.size() > 1;
            }
        }

        return false;
    }

    private List<String> getSpecificTests()
    {
        if ( !isSpecificTestSpecified() )
        {
            return Collections.emptyList();
        }

        List<String> specificTests = new ArrayList<String>();
        String[] testRegexes = StringUtils.split( getTest(), "," );

        for ( String testRegexe : testRegexes )
        {
            String testRegex = testRegexe;
            if ( testRegex.endsWith( ".java" ) )
            {
                testRegex = testRegex.substring( 0, testRegex.length() - 5 );
            }
            // Allow paths delimited by '.' or '/'
            testRegex = testRegex.replace( '.', '/' );
            specificTests.add( "**/" + testRegex + ".java" );
        }

        return specificTests;
    }

    private Artifact getTestNgArtifact()
        throws MojoFailureException, InvalidVersionSpecificationException
    {
        Artifact artifact = getProjectArtifactMap().get( getTestNGArtifactName() );

        if ( artifact != null )
        {
            VersionRange range = VersionRange.createFromVersionSpec( "[4.7,)" );
            if ( !range.containsVersion( new DefaultArtifactVersion( artifact.getVersion() ) ) )
            {
                throw new MojoFailureException(
                    "TestNG support requires version 4.7 or above. You have declared version "
                        + artifact.getVersion() );
            }
        }
        return artifact;

    }

    private Artifact getJunitArtifact()
    {
        return getProjectArtifactMap().get( getJunitArtifactName() );
    }

    private Artifact getJunitDepArtifact()
    {
        return getProjectArtifactMap().get( "junit:junit-dep" );
    }

    protected ForkStarter createForkStarter( ProviderInfo provider, ForkConfiguration forkConfiguration,
                                             ClassLoaderConfiguration classLoaderConfiguration,
                                             RunOrderParameters runOrderParameters,
                                             SurefireProperties effectiveSystemProperties )
        throws MojoExecutionException, MojoFailureException
    {
        StartupConfiguration startupConfiguration = createStartupConfiguration( provider, classLoaderConfiguration );
        String configChecksum = getConfigChecksum();
        StartupReportConfiguration startupReportConfiguration = getStartupReportConfiguration( configChecksum );
        ProviderConfiguration providerConfiguration = createProviderConfiguration( runOrderParameters );
        return new ForkStarter( providerConfiguration, startupConfiguration, forkConfiguration,
                                getForkedProcessTimeoutInSeconds(), startupReportConfiguration,
                                effectiveSystemProperties );
    }

    protected InPluginVMSurefireStarter createInprocessStarter( ProviderInfo provider,
                                                                ClassLoaderConfiguration classLoaderConfiguration,
                                                                RunOrderParameters runOrderParameters )
        throws MojoExecutionException, MojoFailureException
    {
        StartupConfiguration startupConfiguration = createStartupConfiguration( provider, classLoaderConfiguration );
        String configChecksum = getConfigChecksum();
        StartupReportConfiguration startupReportConfiguration = getStartupReportConfiguration( configChecksum );
        ProviderConfiguration providerConfiguration = createProviderConfiguration( runOrderParameters );
        return new InPluginVMSurefireStarter( startupConfiguration, providerConfiguration, startupReportConfiguration );

    }

    protected ForkConfiguration getForkConfiguration()
    {
        File tmpDir = getSurefireTempDir();
        //noinspection ResultOfMethodCallIgnored
        tmpDir.mkdirs();

        Artifact shadeFire = getPluginArtifactMap().get( "org.apache.maven.surefire:surefire-shadefire" );

        final Classpath bootClasspathConfiguration =
            getArtifactClasspath( shadeFire != null ? shadeFire : surefireBooterArtifact );

        return new ForkConfiguration( bootClasspathConfiguration, tmpDir, getEffectiveDebugForkedProcess(),
                               getEffectiveJvm(),
                               getWorkingDirectory() != null ? getWorkingDirectory() : getBasedir(), getArgLine(),
                               getEnvironmentVariables(), getLog().isDebugEnabled(), getEffectiveForkCount() );
    }


    private int getEffectiveForkCount()
    {
        return ( ForkConfiguration.FORK_PERTHREAD.equals( getEffectiveForkMode() ) ) ? getThreadCount() : 1;
    }

    private String getEffectiveDebugForkedProcess()
    {
        String debugForkedProcess = getDebugForkedProcess();
        if ( "true".equals( debugForkedProcess ) )
        {
            return "-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005";
        }
        return debugForkedProcess;
    }

    private String getEffectiveJvm()
    {
        String jvmToUse = getJvm();
        if ( toolchain != null && jvmToUse == null )
        {
            jvmToUse = toolchain.findTool( "java" ); //NOI18N
        }

        if ( StringUtils.isEmpty( jvmToUse ) )
        {
            // use the same JVM as the one used to run Maven (the "java.home" one)
            jvmToUse = System.getProperty( "java.home" ) + File.separator + "bin" + File.separator + "java";
            getLog().debug( "Using JVM: " + jvmToUse );
        }

        return jvmToUse;
    }


    private Artifact getSurefireBooterArtifact()
    {
        Artifact artifact = getPluginArtifactMap().get( "org.apache.maven.surefire:surefire-booter" );
        if ( artifact == null )
        {
            throw new RuntimeException( "Unable to locate surefire-booter in the list of plugin artifacts" );
        }
        artifact.isSnapshot(); // MNG-2961: before Maven 2.0.8, fixes getBaseVersion to be -SNAPSHOT if needed
        return artifact;
    }


    /**
     * Where surefire stores its own temp files
     *
     * @return A file pointing to the location of surefire's own temp files
     */
    private File getSurefireTempDir()
    {
        return new File( getReportsDirectory().getParentFile(), "surefire" );
    }

    /**
     * Operates on raw plugin paramenters, not the "effective" values.
     *
     * @return The checksum
     */
    private String getConfigChecksum()
    {
        ChecksumCalculator checksum = new ChecksumCalculator();
        checksum.add( getPluginName() );
        checksum.add( isSkipTests() );
        checksum.add( isSkipExec() );
        checksum.add( isSkip() );
        checksum.add( getTestClassesDirectory() );
        checksum.add( getClassesDirectory() );
        checksum.add( getClasspathDependencyExcludes() );
        checksum.add( getClasspathDependencyScopeExclude() );
        checksum.add( getAdditionalClasspathElements() );
        checksum.add( getReportsDirectory() );
        checksum.add( getTestSourceDirectory() );
        checksum.add( getTest() );
        checksum.add( getIncludes() );
        checksum.add( getExcludes() );
        checksum.add( getLocalRepository() );
        checksum.add( getSystemProperties() );
        checksum.add( getSystemPropertyVariables() );
        checksum.add( getSystemPropertiesFile() );
        checksum.add( getProperties() );
        checksum.add( isPrintSummary() );
        checksum.add( getReportFormat() );
        checksum.add( getReportNameSuffix() );
        checksum.add( isUseFile() );
        checksum.add( isRedirectTestOutputToFile() );
        checksum.add( getForkMode() );
        checksum.add( getJvm() );
        checksum.add( getArgLine() );
        checksum.add( getDebugForkedProcess() );
        checksum.add( getForkedProcessTimeoutInSeconds() );
        checksum.add( getEnvironmentVariables() );
        checksum.add( getWorkingDirectory() );
        checksum.add( isChildDelegation() );
        checksum.add( getGroups() );
        checksum.add( getExcludedGroups() );
        checksum.add( getSuiteXmlFiles() );
        checksum.add( getJunitArtifact() );
        checksum.add( getTestNGArtifactName() );
        checksum.add( getThreadCount() );
        checksum.add( getPerCoreThreadCount() );
        checksum.add( getUseUnlimitedThreads() );
        checksum.add( getParallel() );
        checksum.add( isTrimStackTrace() );
        checksum.add( getRemoteRepositories() );
        checksum.add( isDisableXmlReport() );
        checksum.add( isUseSystemClassLoader() );
        checksum.add( isUseManifestOnlyJar() );
        checksum.add( isEnableAssertions() );
        checksum.add( getObjectFactory() );
        checksum.add( getFailIfNoTests() );
        checksum.add( getRunOrder() );
        addPluginSpecificChecksumItems( checksum );
        return checksum.getSha1();

    }

    protected void addPluginSpecificChecksumItems( ChecksumCalculator checksum )
    {

    }

    protected boolean hasExecutedBefore()
    {
        // A tribute to Linus Torvalds
        String configChecksum = getConfigChecksum();
        @SuppressWarnings( "unchecked" ) Map<String, String> pluginContext = getPluginContext();
        if ( pluginContext.containsKey( configChecksum ) )
        {
            getLog().info( "Skipping execution of surefire because it has already been run for this configuration" );
            return true;
        }
        pluginContext.put( configChecksum, configChecksum );

        return false;
    }

    protected ClassLoaderConfiguration getClassLoaderConfiguration( boolean isForking )
    {
        return isForking
            ? new ClassLoaderConfiguration( isUseSystemClassLoader(), isUseManifestOnlyJar() )
            : new ClassLoaderConfiguration( false, false );
    }

    protected abstract String[] getDefaultIncludes();

    /**
     * Generate the test classpath.
     *
     * @return List containing the classpath elements
     * @throws InvalidVersionSpecificationException
     *                                     when it happens
     * @throws MojoFailureException        when it happens
     * @throws ArtifactNotFoundException   when it happens
     * @throws ArtifactResolutionException when it happens
     */
    Classpath generateTestClasspath()
        throws InvalidVersionSpecificationException, MojoFailureException, ArtifactResolutionException,
        ArtifactNotFoundException
    {
        List<String> classpath = new ArrayList<String>( 2 + getProject().getArtifacts().size() );

        classpath.add( getTestClassesDirectory().getAbsolutePath() );

        classpath.add( getClassesDirectory().getAbsolutePath() );

        @SuppressWarnings( "unchecked" ) Set<Artifact> classpathArtifacts = getProject().getArtifacts();

        if ( getClasspathDependencyScopeExclude() != null && !getClasspathDependencyScopeExclude().equals( "" ) )
        {
            ArtifactFilter dependencyFilter = new ScopeArtifactFilter( getClasspathDependencyScopeExclude() );
            classpathArtifacts = this.filterArtifacts( classpathArtifacts, dependencyFilter );
        }

        if ( getClasspathDependencyExcludes() != null )
        {
            ArtifactFilter dependencyFilter = new PatternIncludesArtifactFilter( getClasspathDependencyExcludes() );
            classpathArtifacts = this.filterArtifacts( classpathArtifacts, dependencyFilter );
        }

        for ( Artifact artifact : classpathArtifacts )
        {
            if ( artifact.getArtifactHandler().isAddedToClasspath() )
            {
                File file = artifact.getFile();
                if ( file != null )
                {
                    classpath.add( file.getPath() );
                }
            }
        }

        // Add additional configured elements to the classpath
        if ( getAdditionalClasspathElements() != null )
        {
            for ( String classpathElement : getAdditionalClasspathElements() )
            {
                if ( classpathElement != null )
                {
                    classpath.add( classpathElement );
                }
            }
        }

        // adding TestNG MethodSelector to the classpath
        // Todo: move
        if ( getTestNgArtifact() != null )
        {
            addTestNgUtilsArtifacts( classpath );
        }

        return new Classpath( classpath );
    }

    void addTestNgUtilsArtifacts( List<String> classpath )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        Artifact surefireArtifact = getPluginArtifactMap().get( "org.apache.maven.surefire:surefire-booter" );
        String surefireVersion = surefireArtifact.getBaseVersion();

        Artifact[] extraTestNgArtifacts =
            { getArtifactFactory().createArtifact( "org.apache.maven.surefire", "surefire-testng-utils",
                                                   surefireVersion, "runtime", "jar" ),
                getArtifactFactory().createArtifact( "org.apache.maven.surefire", "surefire-grouper", surefireVersion,
                                                     "runtime", "jar" ) };

        for ( Artifact artifact : extraTestNgArtifacts )
        {
            getArtifactResolver().resolve( artifact, getRemoteRepositories(), getLocalRepository() );

            String path = artifact.getFile().getPath();
            classpath.add( path );
        }
    }

    /**
     * Return a new set containing only the artifacts accepted by the given filter.
     *
     * @param artifacts The unfiltered artifacts
     * @param filter    The filter to apply
     * @return The filtered result
     */
    private Set<Artifact> filterArtifacts( Set<Artifact> artifacts, ArtifactFilter filter )
    {
        Set<Artifact> filteredArtifacts = new LinkedHashSet<Artifact>();

        for ( Artifact artifact : artifacts )
        {
            if ( !filter.include( artifact ) )
            {
                filteredArtifacts.add( artifact );
            }
        }

        return filteredArtifacts;
    }

    private void showMap( Map<?, ?> map, String setting )
    {
        for ( Object o : map.keySet() )
        {
            String key = (String) o;
            String value = (String) map.get( key );
            getLog().debug( "Setting " + setting + " [" + key + "]=[" + value + "]" );
        }
    }


    private ArtifactResolutionResult resolveArtifact( Artifact filteredArtifact, Artifact providerArtifact )
    {
        ArtifactFilter filter = null;
        if ( filteredArtifact != null )
        {
            filter = new ExcludesArtifactFilter(
                Collections.singletonList( filteredArtifact.getGroupId() + ":" + filteredArtifact.getArtifactId() ) );
        }

        Artifact originatingArtifact = getArtifactFactory().createBuildArtifact( "dummy", "dummy", "1.0", "jar" );

        try
        {
            return getArtifactResolver().resolveTransitively( Collections.singleton( providerArtifact ),
                                                              originatingArtifact, getLocalRepository(),
                                                              getRemoteRepositories(), getMetadataSource(), filter );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new NestedRuntimeException( e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new NestedRuntimeException( e );
        }
    }

    private Classpath getArtifactClasspath( Artifact surefireArtifact )
    {
        ArtifactResolutionResult result = resolveArtifact( null, surefireArtifact );

        List<String> items = new ArrayList<String>();
        for ( Object o : result.getArtifacts() )
        {
            Artifact artifact = (Artifact) o;

            getLog().debug(
                "Adding to " + getPluginName() + " booter test classpath: " + artifact.getFile().getAbsolutePath() +
                    " Scope: " + artifact.getScope() );

            items.add( artifact.getFile().getAbsolutePath() );
        }
        return new Classpath( items );
    }

    private Properties getUserProperties()
    {
        Properties props = null;
        try
        {
            // try calling MavenSession.getUserProperties() from Maven 2.1.0-M1+
            Method getUserProperties = getSession().getClass().getMethod( "getUserProperties" );
            props = (Properties) getUserProperties.invoke( getSession() );
        }
        catch ( Exception e )
        {
            String msg = "Build uses Maven 2.0.x, cannot propagate system properties"
                + " from command line to tests (cf. SUREFIRE-121)";
            if ( getLog().isDebugEnabled() )
            {
                getLog().warn( msg, e );
            }
            else
            {
                getLog().warn( msg );
            }
        }
        if ( props == null )
        {
            props = new Properties();
        }
        return props;
    }


    void ensureWorkingDirectoryExists()
        throws MojoFailureException
    {
        if ( getWorkingDirectory() == null )
        {
            throw new MojoFailureException( "workingDirectory cannot be null" );
        }

        if ( !getWorkingDirectory().exists() )
        {
            if ( !getWorkingDirectory().mkdirs() )
            {
                throw new MojoFailureException( "Cannot create workingDirectory " + getWorkingDirectory() );
            }
        }

        if ( !getWorkingDirectory().isDirectory() )
        {
            throw new MojoFailureException(
                "workingDirectory " + getWorkingDirectory() + " exists and is not a directory" );
        }
    }

    void ensureParallelRunningCompatibility()
        throws MojoFailureException
    {
        if ( isMavenParallel() && isForkModeNever() )
        {
            throw new MojoFailureException( "parallel maven execution is not compatible with surefire forkmode NEVER" );
        }
    }

    void warnIfUselessUseSystemClassLoaderParameter()
    {
        if ( isUseSystemClassLoader() && isForkModeNever() )
        {
            getLog().warn( "useSystemClassloader setting has no effect when not forking" );
        }
    }

    class TestNgProviderInfo
        implements ProviderInfo
    {
        private final Artifact testNgArtifact;

        TestNgProviderInfo( Artifact testNgArtifact )
        {
            this.testNgArtifact = testNgArtifact;
        }

        public String getProviderName()
        {
            return "org.apache.maven.surefire.testng.TestNGProvider";
        }

        public boolean isApplicable()
        {
            return testNgArtifact != null;
        }

        public void addProviderProperties()
        {
            convertTestNGParameters();
        }

        public Classpath getProviderClasspath()
            throws ArtifactResolutionException, ArtifactNotFoundException
        {
            Artifact surefireArtifact = getPluginArtifactMap().get( "org.apache.maven.surefire:surefire-booter" );
            return dependencyResolver.getProviderClasspath( "surefire-testng", surefireArtifact.getBaseVersion(),
                                                            testNgArtifact );
        }
    }

    class JUnit3ProviderInfo
        implements ProviderInfo
    {
        public String getProviderName()
        {
            return "org.apache.maven.surefire.junit.JUnit3Provider";
        }

        public boolean isApplicable()
        {
            return true;
        }

        public void addProviderProperties()
        {
        }

        public Classpath getProviderClasspath()
            throws ArtifactResolutionException, ArtifactNotFoundException
        {
            // add the JUnit provider as default - it doesn't require JUnit to be present,
            // since it supports POJO tests.
            return dependencyResolver.getProviderClasspath( "surefire-junit3", surefireBooterArtifact.getBaseVersion(),
                                                            null );

        }

    }

    class JUnit4ProviderInfo
        implements ProviderInfo
    {
        private final Artifact junitArtifact;

        private final Artifact junitDepArtifact;

        JUnit4ProviderInfo( Artifact junitArtifact, Artifact junitDepArtifact )
        {
            this.junitArtifact = junitArtifact;
            this.junitDepArtifact = junitDepArtifact;
        }

        public String getProviderName()
        {
            return "org.apache.maven.surefire.junit4.JUnit4Provider";
        }

        public boolean isApplicable()
        {
            return junitDepArtifact != null || isAnyJunit4( junitArtifact );
        }

        public void addProviderProperties()
        {
        }

        public Classpath getProviderClasspath()
            throws ArtifactResolutionException, ArtifactNotFoundException
        {
            return dependencyResolver.getProviderClasspath( "surefire-junit4", surefireBooterArtifact.getBaseVersion(),
                                                            null );

        }

    }

    class JUnitCoreProviderInfo
        implements ProviderInfo
    {
        private final Artifact junitArtifact;

        private final Artifact junitDepArtifact;

        JUnitCoreProviderInfo( Artifact junitArtifact, Artifact junitDepArtifact )
        {
            this.junitArtifact = junitArtifact;
            this.junitDepArtifact = junitDepArtifact;
        }

        public String getProviderName()
        {
            return "org.apache.maven.surefire.junitcore.JUnitCoreProvider";
        }

        private boolean is47CompatibleJunitDep()
        {
            return junitDepArtifact != null && isJunit47Compatible( junitDepArtifact );
        }

        public boolean isApplicable()
        {
            final boolean isJunitArtifact47 = isAnyJunit4( junitArtifact ) && isJunit47Compatible( junitArtifact );
            final boolean isAny47ProvidersForcers = isAnyConcurrencySelected() || isAnyGroupsSelected();
            return isAny47ProvidersForcers && ( isJunitArtifact47 || is47CompatibleJunitDep() );
        }

        public void addProviderProperties()
        {
            convertJunitCoreParameters();
            convertGroupParameters();
        }

        public Classpath getProviderClasspath()
            throws ArtifactResolutionException, ArtifactNotFoundException
        {
            return dependencyResolver.getProviderClasspath( "surefire-junit47", surefireBooterArtifact.getBaseVersion(),
                                                            null );
        }

    }

    public class DynamicProviderInfo
        implements ConfigurableProviderInfo
    {
        final String providerName;

        DynamicProviderInfo( String providerName )
        {
            this.providerName = providerName;
        }

        public ProviderInfo instantiate( String providerName )
        {
            return new DynamicProviderInfo( providerName );
        }

        public String getProviderName()
        {
            return providerName;
        }

        public boolean isApplicable()
        {
            return true;
        }

        public void addProviderProperties()
        {
            // Ok this is a bit lazy.
            convertJunitCoreParameters();
            convertTestNGParameters();
        }


        public Classpath getProviderClasspath()
            throws ArtifactResolutionException, ArtifactNotFoundException
        {
            final Map<String, Artifact> pluginArtifactMap = getPluginArtifactMap();
            Artifact plugin = pluginArtifactMap.get( "org.apache.maven.plugins:maven-surefire-plugin" );
            return dependencyResolver.addProviderToClasspath( pluginArtifactMap, plugin );
        }

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

    /**
     * @noinspection deprecation
     */
    public Properties getSystemProperties()
    {
        return systemProperties;
    }

    @SuppressWarnings( { "UnusedDeclaration", "deprecation" } )
    public void setSystemProperties( Properties systemProperties )
    {
        this.systemProperties = systemProperties;
    }

    public Map<String, String> getSystemPropertyVariables()
    {
        return systemPropertyVariables;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setSystemPropertyVariables( Map<String, String> systemPropertyVariables )
    {
        this.systemPropertyVariables = systemPropertyVariables;
    }

    public File getSystemPropertiesFile()
    {
        return systemPropertiesFile;
    }

    @SuppressWarnings( "UnusedDeclaration" )
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

    @SuppressWarnings( "UnusedDeclaration" )
    public void setPluginArtifactMap( Map<String, Artifact> pluginArtifactMap )
    {
        this.pluginArtifactMap = pluginArtifactMap;
    }

    public Map<String, Artifact> getProjectArtifactMap()
    {
        return projectArtifactMap;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setProjectArtifactMap( Map<String, Artifact> projectArtifactMap )
    {
        this.projectArtifactMap = projectArtifactMap;
    }


    public String getReportNameSuffix()
    {
        return reportNameSuffix;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setReportNameSuffix( String reportNameSuffix )
    {
        this.reportNameSuffix = reportNameSuffix;
    }


    public boolean isRedirectTestOutputToFile()
    {
        return redirectTestOutputToFile;
    }

    @SuppressWarnings( "UnusedDeclaration" )
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

    @SuppressWarnings( "UnusedDeclaration" )
    public void setForkMode( String forkMode )
    {
        this.forkMode = forkMode;
    }

    public String getJvm()
    {
        return jvm;
    }

    public String getArgLine()
    {
        return argLine;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setArgLine( String argLine )
    {
        this.argLine = argLine;
    }


    public Map<String, String> getEnvironmentVariables()
    {
        return environmentVariables;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setEnvironmentVariables( Map<String, String> environmentVariables )
    {
        this.environmentVariables = environmentVariables;
    }

    public File getWorkingDirectory()
    {
        return workingDirectory;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setWorkingDirectory( File workingDirectory )
    {
        this.workingDirectory = workingDirectory;
    }

    public boolean isChildDelegation()
    {
        return childDelegation;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setChildDelegation( boolean childDelegation )
    {
        this.childDelegation = childDelegation;
    }

    public String getGroups()
    {
        return groups;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setGroups( String groups )
    {
        this.groups = groups;
    }

    public String getExcludedGroups()
    {
        return excludedGroups;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setExcludedGroups( String excludedGroups )
    {
        this.excludedGroups = excludedGroups;
    }

    public File[] getSuiteXmlFiles()
    {
        return suiteXmlFiles;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setSuiteXmlFiles( File[] suiteXmlFiles )
    {
        this.suiteXmlFiles = suiteXmlFiles;
    }

    public String getJunitArtifactName()
    {
        return junitArtifactName;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setJunitArtifactName( String junitArtifactName )
    {
        this.junitArtifactName = junitArtifactName;
    }

    public String getTestNGArtifactName()
    {
        return testNGArtifactName;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setTestNGArtifactName( String testNGArtifactName )
    {
        this.testNGArtifactName = testNGArtifactName;
    }

    public int getThreadCount()
    {
        return threadCount;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setThreadCount( int threadCount )
    {
        this.threadCount = threadCount;
    }

    public boolean getPerCoreThreadCount()
    {
        return perCoreThreadCount;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setPerCoreThreadCount( boolean perCoreThreadCount )
    {
        this.perCoreThreadCount = perCoreThreadCount;
    }

    public boolean getUseUnlimitedThreads()
    {
        return useUnlimitedThreads;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setUseUnlimitedThreads( boolean useUnlimitedThreads )
    {
        this.useUnlimitedThreads = useUnlimitedThreads;
    }

    public String getParallel()
    {
        return parallel;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setParallel( String parallel )
    {
        this.parallel = parallel;
    }

    public boolean isTrimStackTrace()
    {
        return trimStackTrace;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setTrimStackTrace( boolean trimStackTrace )
    {
        this.trimStackTrace = trimStackTrace;
    }

    public ArtifactResolver getArtifactResolver()
    {
        return artifactResolver;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setArtifactResolver( ArtifactResolver artifactResolver )
    {
        this.artifactResolver = artifactResolver;
    }

    public ArtifactFactory getArtifactFactory()
    {
        return artifactFactory;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setArtifactFactory( ArtifactFactory artifactFactory )
    {
        this.artifactFactory = artifactFactory;
    }

    public List<ArtifactRepository> getRemoteRepositories()
    {
        return remoteRepositories;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setRemoteRepositories( List<ArtifactRepository> remoteRepositories )
    {
        this.remoteRepositories = remoteRepositories;
    }

    public ArtifactMetadataSource getMetadataSource()
    {
        return metadataSource;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setMetadataSource( ArtifactMetadataSource metadataSource )
    {
        this.metadataSource = metadataSource;
    }


    public boolean isDisableXmlReport()
    {
        return disableXmlReport;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setDisableXmlReport( boolean disableXmlReport )
    {
        this.disableXmlReport = disableXmlReport;
    }


    public boolean isEnableAssertions()
    {
        return enableAssertions;
    }

    public boolean effectiveIsEnableAssertions(){
        if ( getArgLine() != null )
        {
            List<String> args = Arrays.asList( getArgLine().split( " " ) );
            if ( args.contains( "-da" ) || args.contains( "-disableassertions" ) )
            {
                return false;
            }
        }
        return isEnableAssertions();
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setEnableAssertions( boolean enableAssertions )
    {
        this.enableAssertions = enableAssertions;
    }

    public MavenSession getSession()
    {
        return session;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setSession( MavenSession session )
    {
        this.session = session;
    }

    public String getObjectFactory()
    {
        return objectFactory;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setObjectFactory( String objectFactory )
    {
        this.objectFactory = objectFactory;
    }

    public ToolchainManager getToolchainManager()
    {
        return toolchainManager;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setToolchainManager( ToolchainManager toolchainManager )
    {
        this.toolchainManager = toolchainManager;
    }

    public boolean isMavenParallel()
    {
        return parallelMavenExecution != null && parallelMavenExecution;
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

    public PluginDescriptor getPluginDescriptor()
    {
        return pluginDescriptor;
    }

    public MavenProject getProject()
    {
        return project;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    public File getTestSourceDirectory()
    {
        return testSourceDirectory;
    }

    public void setTestSourceDirectory( File testSourceDirectory )
    {
        this.testSourceDirectory = testSourceDirectory;
    }

}
