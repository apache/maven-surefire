// CHECKSTYLE_OFF: FileLength|RegexpHeader
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

import javax.annotation.Nonnull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
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
import org.apache.maven.plugin.surefire.booterclient.ClasspathForkConfiguration;
import org.apache.maven.plugin.surefire.booterclient.ForkConfiguration;
import org.apache.maven.plugin.surefire.booterclient.ForkStarter;
import org.apache.maven.plugin.surefire.booterclient.JarManifestForkConfiguration;
import org.apache.maven.plugin.surefire.booterclient.ModularClasspathForkConfiguration;
import org.apache.maven.plugin.surefire.booterclient.Platform;
import org.apache.maven.plugin.surefire.extensions.LegacyForkNodeFactory;
import org.apache.maven.plugin.surefire.extensions.SurefireConsoleOutputReporter;
import org.apache.maven.plugin.surefire.extensions.SurefireStatelessReporter;
import org.apache.maven.plugin.surefire.extensions.SurefireStatelessTestsetInfoReporter;
import org.apache.maven.plugin.surefire.log.PluginConsoleLogger;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.util.DependencyScanner;
import org.apache.maven.plugin.surefire.util.DirectoryScanner;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.artifact.filter.PatternIncludesArtifactFilter;
import org.apache.maven.surefire.api.booter.ProviderParameterNames;
import org.apache.maven.surefire.api.booter.Shutdown;
import org.apache.maven.surefire.api.cli.CommandLineOption;
import org.apache.maven.surefire.api.report.ReporterConfiguration;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.api.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.api.testset.RunOrderParameters;
import org.apache.maven.surefire.api.testset.TestArtifactInfo;
import org.apache.maven.surefire.api.testset.TestListResolver;
import org.apache.maven.surefire.api.testset.TestRequest;
import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.apache.maven.surefire.api.util.DefaultScanResult;
import org.apache.maven.surefire.api.util.RunOrder;
import org.apache.maven.surefire.booter.ClassLoaderConfiguration;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.ClasspathConfiguration;
import org.apache.maven.surefire.booter.KeyValueSource;
import org.apache.maven.surefire.booter.ModularClasspath;
import org.apache.maven.surefire.booter.ModularClasspathConfiguration;
import org.apache.maven.surefire.booter.ProcessCheckerType;
import org.apache.maven.surefire.booter.ProviderConfiguration;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.SurefireBooterForkException;
import org.apache.maven.surefire.booter.SurefireExecutionException;
import org.apache.maven.surefire.extensions.ForkNodeFactory;
import org.apache.maven.surefire.providerapi.ConfigurableProviderInfo;
import org.apache.maven.surefire.providerapi.ProviderDetector;
import org.apache.maven.surefire.providerapi.ProviderInfo;
import org.apache.maven.surefire.providerapi.ProviderRequirements;
import org.apache.maven.surefire.shared.utils.io.FileUtils;
import org.apache.maven.toolchain.DefaultToolchain;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.apache.maven.toolchain.java.DefaultJavaToolChain;
import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor;
import org.codehaus.plexus.languages.java.jpms.LocationManager;
import org.codehaus.plexus.languages.java.jpms.ResolvePathRequest;
import org.codehaus.plexus.languages.java.jpms.ResolvePathResult;
import org.codehaus.plexus.languages.java.jpms.ResolvePathsRequest;
import org.codehaus.plexus.languages.java.jpms.ResolvePathsResult;
import org.codehaus.plexus.logging.Logger;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static java.util.Collections.addAll;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.apache.maven.plugin.surefire.AbstractSurefireMojo.PluginFailureReason.COULD_NOT_RUN_DEFAULT_TESTS;
import static org.apache.maven.plugin.surefire.AbstractSurefireMojo.PluginFailureReason.COULD_NOT_RUN_SPECIFIED_TESTS;
import static org.apache.maven.plugin.surefire.AbstractSurefireMojo.PluginFailureReason.NONE;
import static org.apache.maven.plugin.surefire.SurefireDependencyResolver.isWithinVersionSpec;
import static org.apache.maven.plugin.surefire.SurefireHelper.replaceThreadNumberPlaceholders;
import static org.apache.maven.plugin.surefire.util.DependencyScanner.filter;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.EXCLUDE_JUNIT5_ENGINES_PROP;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.INCLUDE_JUNIT5_ENGINES_PROP;
import static org.apache.maven.surefire.api.suite.RunResult.failure;
import static org.apache.maven.surefire.api.suite.RunResult.noTestsRun;
import static org.apache.maven.surefire.api.util.ReflectionUtils.invokeMethodWithArray;
import static org.apache.maven.surefire.api.util.ReflectionUtils.tryGetMethod;
import static org.apache.maven.surefire.booter.Classpath.emptyClasspath;
import static org.apache.maven.surefire.booter.SystemUtils.endsWithJavaPath;
import static org.apache.maven.surefire.booter.SystemUtils.isBuiltInJava9AtLeast;
import static org.apache.maven.surefire.booter.SystemUtils.isJava9AtLeast;
import static org.apache.maven.surefire.booter.SystemUtils.toJdkHomeFromJvmExec;
import static org.apache.maven.surefire.booter.SystemUtils.toJdkVersionFromReleaseFile;
import static org.apache.maven.surefire.shared.lang3.JavaVersion.JAVA_RECENT;
import static org.apache.maven.surefire.shared.lang3.StringUtils.substringBeforeLast;
import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.apache.maven.surefire.shared.utils.StringUtils.capitalizeFirstLetter;
import static org.apache.maven.surefire.shared.utils.StringUtils.isEmpty;
import static org.apache.maven.surefire.shared.utils.StringUtils.isNotBlank;
import static org.apache.maven.surefire.shared.utils.StringUtils.isNotEmpty;
import static org.apache.maven.surefire.shared.utils.StringUtils.split;
import static org.apache.maven.surefire.shared.utils.cli.ShutdownHookUtils.addShutDownHook;
import static org.apache.maven.surefire.shared.utils.cli.ShutdownHookUtils.removeShutdownHook;

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
    private static final String FORK_ONCE = "once";
    private static final String FORK_ALWAYS = "always";
    private static final String FORK_NEVER = "never";
    private static final String FORK_PERTHREAD = "perthread";
    private static final Map<String, String> JAVA_9_MATCHER_OLD_NOTATION = singletonMap( "version", "[1.9,)" );
    private static final Map<String, String> JAVA_9_MATCHER = singletonMap( "version", "[9,)" );
    private static final Platform PLATFORM = new Platform();

    private final ClasspathCache classpathCache = new ClasspathCache();

    /**
     * Note: use the legacy system property <em>disableXmlReport</em> set to {@code true} to disable the report.
     */
    @Parameter
    private SurefireStatelessReporter statelessTestsetReporter;

    @Parameter
    private SurefireConsoleOutputReporter consoleOutputReporter;

    @Parameter
    private SurefireStatelessTestsetInfoReporter statelessTestsetInfoReporter;

    /**
     * Information about this plugin, mainly used to lookup this plugin's configuration from the currently executing
     * project.
     *
     * @since 2.12
     */
    @Parameter( defaultValue = "${plugin}", readonly = true, required = true )
    private PluginDescriptor pluginDescriptor;

    /**
     * Set this to "true" to skip running tests, but still compile them. Its use is NOT RECOMMENDED, but quite
     * convenient on occasion.<br>
     * Failsafe plugin deprecated the parameter {@code skipTests} and the parameter will be removed in
     * <i>Failsafe 3.0.0</i> as it is a source of conflicts between Failsafe and Surefire plugin.
     *
     * @since 2.4
     */
    @Parameter( property = "skipTests", defaultValue = "false" )
    protected boolean skipTests;

    /**
     * This old parameter is just like {@code skipTests}, but bound to the old property "maven.test.skip.exec".
     *
     * @since 2.3
     * @deprecated Use skipTests instead.
     */
    @Deprecated
    @Parameter( property = "maven.test.skip.exec" )
    protected boolean skipExec;

    /**
     * Set this to "true" to bypass unit tests entirely. Its use is NOT RECOMMENDED, especially if you enable it using
     * the "maven.test.skip" property, because maven.test.skip disables both running the tests and compiling the tests.
     * Consider using the {@code skipTests} parameter instead.
     */
    @Parameter( property = "maven.test.skip", defaultValue = "false" )
    protected boolean skip;

    /**
     * The Maven Project Object.
     */
    @Parameter( defaultValue = "${project}", required = true, readonly = true )
    private MavenProject project;

    /**
     * The base directory of the project being tested. This can be obtained in your integration test via
     * System.getProperty("basedir").
     */
    @Parameter( defaultValue = "${basedir}", readonly = true, required = true )
    protected File basedir;

    /**
     * The directory containing generated test classes of the project being tested. This will be included at the
     * beginning of the test classpath. *
     */
    @Parameter( defaultValue = "${project.build.testOutputDirectory}" )
    protected File testClassesDirectory;

    /**
     * List of dependencies to exclude from the test classpath. Each dependency string must follow the format
     * <i>groupId:artifactId</i>. For example: <i>org.acme:project-a</i>
     *
     * @since 2.6
     */
    @Parameter( property = "maven.test.dependency.excludes" )
    private String[] classpathDependencyExcludes;

    /**
     * A dependency scope to exclude from the test classpath. The scope should be one of the scopes defined by
     * org.apache.maven.artifact.Artifact. This includes the following:
     * <br>
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
    @Parameter( property = "maven.test.additionalClasspath" )
    private String[] additionalClasspathElements;

    /**
     * The test source directory containing test class sources.
     * Important <b>only</b> for TestNG HTML reports.
     *
     * @since 2.2
     */
    @Parameter( defaultValue = "${project.build.testSourceDirectory}" )
    private File testSourceDirectory;

    /**
     * ArtifactRepository of the localRepository. To obtain the directory of localRepository in unit tests use
     * System.getProperty("localRepository").
     */
    @Parameter( defaultValue = "${localRepository}", required = true, readonly = true )
    private ArtifactRepository localRepository;

    /**
     * List of System properties to pass to a provider.
     *
     * @deprecated Use systemPropertyVariables instead.
     */
    @Deprecated
    @Parameter
    private Properties systemProperties;

    /**
     * List of System properties to pass to a provider.
     *
     * @since 2.5
     */
    @Parameter
    private Map<String, String> systemPropertyVariables;

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
    @Parameter( property = "plugin.artifactMap", required = true, readonly = true )
    private Map<String, Artifact> pluginArtifactMap;

    /**
     * Map of project artifacts.
     */
    @Parameter( property = "project.artifactMap", readonly = true, required = true )
    private Map<String, Artifact> projectArtifactMap;

    /**
     * Add custom text into report filename: TEST-testClassName-reportNameSuffix.xml,
     * testClassName-reportNameSuffix.txt and testClassName-reportNameSuffix-output.txt.
     * File TEST-testClassName-reportNameSuffix.xml has changed attributes 'testsuite'--'name'
     * and 'testcase'--'classname' - reportNameSuffix is added to the attribute value.
     */
    @Parameter( property = "surefire.reportNameSuffix", defaultValue = "" )
    private String reportNameSuffix;

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
    @Parameter( property = "failIfNoTests", defaultValue = "false" )
    private boolean failIfNoTests;

    /**
     * <strong>DEPRECATED</strong> since version 2.14. Use {@code forkCount} and {@code reuseForks} instead.
     * <br>
     * <br>
     * Option to specify the forking mode. Can be {@code never}, {@code once}, {@code always}, {@code perthread}.<br>
     * The {@code none} and {@code pertest} are also accepted for backwards compatibility.<br>
     * The {@code always} forks for each test-class.<br>
     * The {@code perthread} creates the number of parallel forks specified by {@code threadCount}, where each forked
     * JVM is executing one test-class. See also the parameter {@code reuseForks} for the lifetime of JVM.
     *
     * @since 2.1
     * @deprecated
     */
    @Deprecated
    @Parameter( property = "forkMode", defaultValue = "once" )
    private String forkMode;

    /**
     * Relative path to <i>temporary-surefire-boot</i> directory containing internal Surefire temporary files.
     * <br>
     * The <i>temporary-surefire-boot</i> directory is <i>project.build.directory</i> on most platforms or
     * <i>system default temporary-directory</i> specified by the system property {@code java.io.tmpdir}
     * on Windows (see <a href="https://issues.apache.org/jira/browse/SUREFIRE-1400">SUREFIRE-1400</a>).
     * <br>
     * It is deleted after the test set has completed.
     *
     * @since 2.20
     */
    @Parameter( property = "tempDir", defaultValue = "surefire" )
    private String tempDir;

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
     * <br>
     * <br>
     * Since the Version 2.17 using an alternate syntax for {@code argLine}, <b>@{...}</b> allows late replacement
     * of properties when the plugin is executed, so properties that have been modified by other plugins will be picked
     * up correctly.
     * See the Frequently Asked Questions page with more details:<br>
     * <a href="http://maven.apache.org/surefire/maven-surefire-plugin/faq.html">
     *     http://maven.apache.org/surefire/maven-surefire-plugin/faq.html</a>
     * <br>
     * <a href="http://maven.apache.org/surefire/maven-failsafe-plugin/faq.html">
     *     http://maven.apache.org/surefire/maven-failsafe-plugin/faq.html</a>
     *
     * @since 2.1
     */
    @Parameter( property = "argLine" )
    private String argLine;

    /**
     * Additional environment variables to set on the command line.
     *
     * @since 2.1.3
     */
    @Parameter
    private Map<String, String> environmentVariables = new HashMap<>();

    /**
     * Command line working directory.
     *
     * @since 2.1.3
     */
    @Parameter( property = "basedir" )
    private File workingDirectory;

    /**
     * When false it makes tests run using the standard classloader delegation instead of the default Maven isolated
     * classloader. Only used when forking ({@code forkMode} is not {@code none}).<br>
     * Setting it to false helps with some problems caused by conflicts between xml parsers in the classpath and the
     * Java 5 provider parser.
     *
     * @since 2.1
     */
    @Parameter( property = "childDelegation", defaultValue = "false" )
    private boolean childDelegation;

    /**
     * (TestNG/JUnit47 provider with JUnit4.8+ only and JUnit5+ provider since 2.22.0) Groups/categories/tags for this
     * test. Only classes/methods/etc decorated with one of the groups/categories/tags specified here will be included
     * in test run, if specified.<br>
     * For JUnit4 tests, this parameter forces the use of the 4.7 provider. For JUnit5 tests, this parameter forces
     * the use of the JUnit platform provider.<br>
     * This parameter is ignored if the {@code suiteXmlFiles} parameter is specified.<br>
     * Since version 2.18.1 and JUnit 4.12, the {@code @Category} annotation type is automatically inherited from
     * superclasses, see {@code @java.lang.annotation.Inherited}. Make sure that test class inheritance still makes
     * sense together with {@code @Category} annotation of the JUnit 4.12 or higher appeared in superclass.
     *
     * @since 2.2
     */
    @Parameter( property = "groups" )
    private String groups;

    /**
     * (TestNG/JUnit47 provider with JUnit4.8+ only and JUnit5+ provider since 2.22.0) Excluded groups/categories/tags.
     * Any methods/classes/etc with one of the groups/categories/tags specified in this list will specifically not be
     * run.<br>
     * For JUnit4, this parameter forces the use of the 4.7 provider. For JUnit5, this parameter forces the use of the
     * JUnit platform provider.<br>
     * This parameter is ignored if the {@code suiteXmlFiles} parameter is specified.<br>
     * Since version 2.18.1 and JUnit 4.12, the {@code @Category} annotation type is automatically inherited from
     * superclasses, see {@code @java.lang.annotation.Inherited}. Make sure that test class inheritance still makes
     * sense together with {@code @Category} annotation of the JUnit 4.12 or higher appeared in superclass.
     *
     * @since 2.2
     */
    @Parameter( property = "excludedGroups" )
    private String excludedGroups;

    /**
     * Allows you to specify the name of the JUnit artifact. If not set, {@code junit:junit} will be used.
     *
     * @since 2.3.1
     */
    @Parameter( property = "junitArtifactName", defaultValue = "junit:junit" )
    private String junitArtifactName;

    /**
     * Allows you to specify the name of the TestNG artifact. If not set, {@code org.testng:testng} will be used.
     *
     * @since 2.3.1
     */
    @Parameter( property = "testNGArtifactName", defaultValue = "org.testng:testng" )
    private String testNGArtifactName;

    /**
     * (TestNG/JUnit 4.7 provider) The attribute thread-count allows you to specify how many threads should be
     * allocated for this execution. Only makes sense to use in conjunction with the {@code parallel} parameter.
     *
     * @since 2.2
     */
    @Parameter( property = "threadCount" )
    private int threadCount;

    /**
     * Option to specify the number of VMs to fork in parallel in order to execute the tests. When terminated with "C",
     * the number part is multiplied with the number of CPU cores. Floating point value are only accepted together with
     * "C". If set to "0", no VM is forked and all tests are executed within the main process.<br>
     * <br>
     * Example values: "1.5C", "4"<br>
     * <br>
     * The system properties and the {@code argLine} of the forked processes may contain the place holder string
     * <code>${surefire.forkNumber}</code>, which is replaced with a fixed number for each of the parallel forks,
     * ranging from <b>1</b> to the effective value of {@code forkCount} times the maximum number of parallel
     * Surefire executions in maven parallel builds, i.e. the effective value of the <b>-T</b> command line
     * argument of maven core.
     *
     * @since 2.14
     */
    @Parameter( property = "forkCount", defaultValue = "1" )
    private String forkCount;

    /**
     * Indicates if forked VMs can be reused. If set to "false", a new VM is forked for each test class to be executed.
     * If set to "true", up to {@code forkCount} VMs will be forked and then reused to execute all tests.
     *
     * @since 2.13
     */

    @Parameter( property = "reuseForks", defaultValue = "true" )
    private boolean reuseForks;

    /**
     * (JUnit 4.7 provider) Indicates that threadCount, threadCountSuites, threadCountClasses, threadCountMethods
     * are per cpu core.
     *
     * @since 2.5
     */
    @Parameter( property = "perCoreThreadCount", defaultValue = "true" )
    private boolean perCoreThreadCount;

    /**
     * (JUnit 4.7 provider) Indicates that the thread pool will be unlimited. The {@code parallel} parameter and
     * the actual number of classes/methods will decide. Setting this to "true" effectively disables
     * {@code perCoreThreadCount} and {@code threadCount}. Defaults to "false".
     *
     * @since 2.5
     */
    @Parameter( property = "useUnlimitedThreads", defaultValue = "false" )
    private boolean useUnlimitedThreads;

    /**
     * (TestNG provider) When you use the parameter {@code parallel}, TestNG will try to run all your test methods
     * in separate threads, except for methods that depend on each other, which will be run in the same thread in order
     * to respect their order of execution.  Supports two values: {@code classes} or {@code methods}.
     * <br>
     * (JUnit 4.7 provider) Supports values {@code classes}, {@code methods}, {@code both} to run
     * in separate threads been controlled by {@code threadCount}.
     * <br>
     * <br>
     * Since version 2.16 (JUnit 4.7 provider), the value {@code both} is <strong>DEPRECATED</strong>.
     * Use {@code classesAndMethods} instead.
     * <br>
     * <br>
     * Since version 2.16 (JUnit 4.7 provider), additional vales are available:
     * <br>
     * {@code suites}, {@code suitesAndClasses}, {@code suitesAndMethods}, {@code classesAndMethods}, {@code all}.
     * <br>
     * By default, Surefire does not execute tests in parallel. You can set the parameter {@code parallel} to
     * {@code none} to explicitly disable parallel execution (e.g. when disabling parallel execution in special Maven
     * profiles when executing coverage analysis).
     *
     * @since 2.2
     */
    @Parameter( property = "parallel" )
    private String parallel;

    /**
     * (JUnit 4.7 / provider only) The thread counts do not exceed the number of parallel suite, class runners and
     * average number of methods per class if set to <strong>true</strong>.
     * <br>
     * True by default.
     *
     * @since 2.17
     */
    @Parameter( property = "parallelOptimized", defaultValue = "true" )
    private boolean parallelOptimized;

    /**
     * (JUnit 4.7 provider) This attribute allows you to specify the concurrency in test suites, i.e.:
     * <ul>
     *  <li>number of concurrent suites if {@code threadCount} is 0 or unspecified</li>
     *  <li>limited suites concurrency if {@code useUnlimitedThreads} is set to <strong>true</strong></li>
     *  <li>if {@code threadCount} and certain thread-count parameters are &gt; 0 for {@code parallel}, the
     *  concurrency is computed from ratio. For instance {@code parallel=all} and the ratio between
     *      {@code threadCountSuites}:{@code threadCountClasses}:{@code threadCountMethods} is
     *      <b>2</b>:3:5, there is 20% of {@code threadCount} which appeared in concurrent suites.</li>
     * </ul>
     *
     * Only makes sense to use in conjunction with the {@code parallel} parameter.
     * The default value <b>0</b> behaves same as unspecified one.
     *
     * @since 2.16
     */
    @Parameter( property = "threadCountSuites", defaultValue = "0" )
    private int threadCountSuites;

    /**
     * (JUnit 4.7 provider) This attribute allows you to specify the concurrency in test classes, i.e.:
     * <ul>
     *  <li>number of concurrent classes if {@code threadCount} is 0 or unspecified</li>
     *  <li>limited classes concurrency if {@code useUnlimitedThreads} is set to <strong>true</strong></li>
     *  <li>if {@code threadCount} and certain thread-count parameters are &gt; 0 for {@code parallel}, the
     *  concurrency is computed from ratio. For instance {@code parallel=all} and the ratio between
     *      {@code threadCountSuites}:{@code threadCountClasses}:{@code threadCountMethods} is
     *      2:<b>3</b>:5, there is 30% of {@code threadCount} in concurrent classes.</li>
     *  <li>as in the previous case but without this leaf thread-count. Example: {@code parallel=suitesAndClasses},
     *  {@code threadCount=16}, {@code threadCountSuites=5}, {@code threadCountClasses} is unspecified leaf, the number
     *  of concurrent classes is varying from &gt;= 11 to 14 or 15. The {@code threadCountSuites} become
     *  given number of threads.</li>
     * </ul>
     *
     * Only makes sense to use in conjunction with the {@code parallel} parameter.
     * The default value <b>0</b> behaves same as unspecified one.
     *
     * @since 2.16
     */
    @Parameter( property = "threadCountClasses", defaultValue = "0" )
    private int threadCountClasses;

    /**
     * (JUnit 4.7 provider) This attribute allows you to specify the concurrency in test methods, i.e.:
     * <ul>
     * <li>number of concurrent methods if {@code threadCount} is 0 or unspecified</li>
     * <li>limited concurrency of methods if {@code useUnlimitedThreads} is set to <strong>true</strong></li>
     * <li>if {@code threadCount} and certain thread-count parameters are &gt; 0 for {@code parallel}, the
     * concurrency is computed from ratio. For instance parallel=all and the ratio between
     * {@code threadCountSuites}:{@code threadCountClasses}:{@code threadCountMethods} is 2:3:<b>5</b>,
     * there is 50% of {@code threadCount} which appears in concurrent methods.</li>
     * <li>as in the previous case but without this leaf thread-count. Example: {@code parallel=all},
     * {@code threadCount=16}, {@code threadCountSuites=2}, {@code threadCountClasses=3}, but {@code threadCountMethods}
     * is unspecified leaf, the number of concurrent methods is varying from &gt;= 11 to 14 or 15.
     * The {@code threadCountSuites} and {@code threadCountClasses} become given number of threads.</li>
     * </ul>
     * Only makes sense to use in conjunction with the {@code parallel} parameter. The default value <b>0</b>
     * behaves same as unspecified one.
     *
     * @since 2.16
     */
    @Parameter( property = "threadCountMethods", defaultValue = "0" )
    private int threadCountMethods;

    /**
     * Whether to trim the stack trace in the reports to just the lines within the test, or show the full trace.
     *
     * @since 2.2
     */
    @Parameter( property = "trimStackTrace", defaultValue = "false" )
    private boolean trimStackTrace;

    /**
     * The remote plugin repositories declared in the POM.
     *
     * @since 2.2
     */
    @Parameter( defaultValue = "${project.pluginArtifactRepositories}", readonly = true, required = true )
    private List<ArtifactRepository> remoteRepositories;

    @Parameter( defaultValue = "${project.remoteArtifactRepositories}", required = true, readonly = true )
    private List<ArtifactRepository> projectRemoteRepositories;

    /**
     * Flag to disable the generation of report files in xml format.
     * Deprecated since 3.0.0-M4.
     * Instead use <em>disable</em> within {@code statelessTestsetReporter} since of 3.0.0-M6.
     * @since 2.2
     */
    @Deprecated // todo make readonly to handle system property
    @Parameter( property = "disableXmlReport", defaultValue = "false" )
    private boolean disableXmlReport;

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
    @Parameter( defaultValue = "${session}", required = true, readonly = true )
    private MavenSession session;

    @Component
    private Logger logger;

    /**
     * (TestNG only) Define the factory class used to create all test instances.
     *
     * @since 2.5
     */
    @Parameter( property = "objectFactory" )
    private String objectFactory;

    /**
     * Parallel Maven Execution.
     */
    @Parameter( defaultValue = "${session.parallel}", readonly = true )
    private Boolean parallelMavenExecution;

    /**
     * Read-only parameter with value of Maven property <i>project.build.directory</i>.
     * @since 2.20
     */
    @Parameter( defaultValue = "${project.build.directory}", readonly = true, required = true )
    private File projectBuildDirectory;

    /**
     * List of dependencies to scan for test classes to include in the test run.
     * The child elements of this element must be &lt;dependency&gt; elements, and the
     * contents of each of these elements must be a string which follows the general form:
     *
     * <p>{@code groupId[:artifactId[:type[:classifier][:version]]]}</p>
     *
     * <p>The wildcard character <code>*</code> can be used within the sub parts of those composite identifiers to
     * do glob-like pattern matching. The classifier may be omitted when matching dependencies without a classifier.</p>
     *
     * <p>Examples:</p>
     *
     * <ul>
     *     <li>{@code group} or, equivalently, {@code group:*}</li>
     *     <li>{@code g*p:*rtifac*}</li>
     *     <li>{@code group:*:jar}</li>
     *     <li>{@code group:artifact:*:1.0.0} (no classifier)</li>
     *     <li>{@code group:*:test-jar:tests}</li>
     *     <li>{@code *:artifact:*:*:1.0.0}</li>
     * </ul>
     *
     * <p>Since version 2.22.0 you can scan for test classes from a project
     * dependency of your multi-module project.</p>
     *
     * <p>In versions before 3.0.0-M4, only <code>groupId:artifactId</code> is supported.</p>
     *
     * @since 2.15
     */
    @Parameter( property = "dependenciesToScan" )
    private String[] dependenciesToScan;

    /**
     * <p>
     *     Allow for configuration of the test jvm via maven toolchains.
     *     This permits a configuration where the project is built with one jvm and tested with another.
     *     This is similar to {@link #jvm}, but avoids hardcoding paths.
     *     The two parameters are mutually exclusive (jvm wins)
     * </p>
     *
     * <p>Examples:</p>
     * (see <a href="https://maven.apache.org/guides/mini/guide-using-toolchains.html">
     *     Guide to Toolchains</a> for more info)
     *
     * <pre>
     * {@code
     *    <configuration>
     *        ...
     *        <jdkToolchain>
     *            <version>1.11</version>
     *        </jdkToolchain>
     *    </configuration>
     *
     *    <configuration>
     *        ...
     *        <jdkToolchain>
     *            <version>1.8</version>
     *            <vendor>zulu</vendor>
     *        </jdkToolchain>
     *    </configuration>
     *    }
     * </pre>
     *
     * @since 3.0.0-M5 and Maven 3.3.x
     */
    @Parameter
    private Map<String, String> jdkToolchain;

    /**
     *
     */
    @Component
    private ToolchainManager toolchainManager;

    @Component
    private LocationManager locationManager;

    @Component
    private RepositorySystem repositorySystem;

    @Component
    private ProviderDetector providerDetector;

    @Component
    private ResolutionErrorHandler resolutionErrorHandler;

    private Toolchain toolchain;

    private int effectiveForkCount = -1;

    protected abstract String getPluginName();

    protected abstract int getRerunFailingTestsCount();

    @Override
    public abstract List<String> getIncludes();

    public abstract File getIncludesFile();

    @Override
    public abstract void setIncludes( List<String> includes );

    public abstract File getExcludesFile();

    /**
     * Calls {@link #getSuiteXmlFiles()} as {@link List list}.
     * Never returns <code>null</code>.
     *
     * @return list of TestNG suite XML files provided by MOJO
     */
    protected abstract List<File> suiteXmlFiles();

    /**
     * @return {@code true} if {@link #getSuiteXmlFiles() suite-xml files array} is not empty.
     */
    protected abstract boolean hasSuiteXmlFiles();

    protected abstract String[] getExcludedEnvironmentVariables();

    public abstract File[] getSuiteXmlFiles();

    public abstract void setSuiteXmlFiles( File[] suiteXmlFiles );

    public abstract String getRunOrder();

    public abstract void setRunOrder( String runOrder );

    public abstract Long getRunOrderRandomSeed();

    public abstract void setRunOrderRandomSeed( Long runOrderRandomSeed );

    protected abstract void handleSummary( RunResult summary, Exception firstForkException )
        throws MojoExecutionException, MojoFailureException;

    protected abstract boolean isSkipExecution();

    protected abstract String[] getDefaultIncludes();

    protected abstract String getReportSchemaLocation();

    protected abstract boolean useModulePath();

    protected abstract void setUseModulePath( boolean useModulePath );

    protected abstract String getEnableProcessChecker();

    protected abstract ForkNodeFactory getForkNode();

    /**
     * This plugin MOJO artifact.
     *
     * @return non-null plugin artifact
     */
    protected Artifact getMojoArtifact()
    {
        return getPluginDescriptor().getPluginArtifact();
    }

    private String getDefaultExcludes()
    {
        return "**/*$*";
    }

    private SurefireDependencyResolver surefireDependencyResolver;

    private TestListResolver specificTests;

    private TestListResolver includedExcludedTests;

    private List<CommandLineOption> cli;

    private volatile PluginConsoleLogger consoleLogger;

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        cli = commandLineOptions();
        // Stuff that should have been final
        setupStuff();
        Platform platform = PLATFORM.withJdkExecAttributesForTests( getEffectiveJvm() );
        Thread shutdownThread = new Thread( platform::setShutdownState );
        addShutDownHook( shutdownThread );
        try
        {
            if ( verifyParameters() && !hasExecutedBefore() )
            {
                DefaultScanResult scan = scanForTestClasses();
                if ( !hasSuiteXmlFiles() && scan.isEmpty() )
                {
                    switch ( getEffectiveFailIfNoTests() )
                    {
                        case COULD_NOT_RUN_DEFAULT_TESTS:
                            throw new MojoFailureException(
                                "No tests were executed!  (Set -DfailIfNoTests=false to ignore this error.)" );
                        case COULD_NOT_RUN_SPECIFIED_TESTS:
                            throw new MojoFailureException( "No tests matching pattern \""
                                + getSpecificTests().toString()
                                + "\" were executed! (Set "
                                + "-D" + getPluginName()
                                + ".failIfNoSpecifiedTests=false to ignore this error.)" );
                        default:
                            handleSummary( noTestsRun(), null );
                            return;
                    }
                }
                logReportsDirectory();
                executeAfterPreconditionsChecked( scan, platform );
            }
        }
        finally
        {
            platform.clearShutdownState();
            removeShutdownHook( shutdownThread );
        }
    }

    void setLogger( Logger logger )
    {
        this.logger = logger;
    }

    @Nonnull
    protected final PluginConsoleLogger getConsoleLogger()
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

    private static <T extends ToolchainManager> Toolchain getToolchainMaven33x( Class<T> toolchainManagerType,
                                                                                T toolchainManager,
                                                                                MavenSession session,
                                                                                Map<String, String> toolchainArgs )
        throws MojoFailureException
    {
        Method getToolchainsMethod =
            tryGetMethod( toolchainManagerType, "getToolchains", MavenSession.class, String.class, Map.class );
        if ( getToolchainsMethod != null )
        {
            //noinspection unchecked
            List<Toolchain> tcs = invokeMethodWithArray( toolchainManager,
                getToolchainsMethod, session, "jdk", toolchainArgs );
            if ( tcs.isEmpty() )
            {
                throw new MojoFailureException(
                    "Requested toolchain specification did not match any configured toolchain: " + toolchainArgs );
            }
            return tcs.get( 0 );
        }
        return null;
    }

    //TODO remove the part with ToolchainManager lookup once we depend on
    //3.0.9 (have it as prerequisite). Define as regular component field then.
    private Toolchain getToolchain() throws MojoFailureException
    {
        Toolchain tc = null;

        if ( getJdkToolchain() != null )
        {
            tc = getToolchainMaven33x( ToolchainManager.class, getToolchainManager(), getSession(), getJdkToolchain() );
        }

        if ( tc == null )
        {
            tc = getToolchainManager().getToolchainFromBuildContext( "jdk", getSession() );
        }

        return tc;
    }

    private void setupStuff() throws MojoFailureException
    {
        surefireDependencyResolver = new SurefireDependencyResolver( getRepositorySystem(),
                getConsoleLogger(), getLocalRepository(),
                getRemoteRepositories(),
                getProjectRemoteRepositories(),
                resolutionErrorHandler,
                getPluginName(),
                getSession().isOffline() );

        if ( getBooterArtifact() == null )
        {
            throw new RuntimeException( "Unable to locate surefire-booter in the list of plugin artifacts" );
        }

        if ( getToolchainManager() != null )
        {
            toolchain = getToolchain();
        }
    }

    @Nonnull
    private DefaultScanResult scanForTestClasses()
        throws MojoFailureException
    {
        DefaultScanResult scan = scanDirectories();
        DefaultScanResult scanDeps = scanDependencies();
        return scan.append( scanDeps );
    }

    private DefaultScanResult scanDirectories()
        throws MojoFailureException
    {
        DirectoryScanner scanner = new DirectoryScanner( getTestClassesDirectory(), getIncludedAndExcludedTests() );
        return scanner.scan();
    }

    List<Artifact> getProjectTestArtifacts()
    {
        return project.getTestArtifacts();
    }

    DefaultScanResult scanDependencies() throws MojoFailureException
    {
        if ( getDependenciesToScan() == null )
        {
            return null;
        }
        else
        {
            try
            {
                DefaultScanResult result = null;

                List<Artifact> dependenciesToScan =
                        filter( getProjectTestArtifacts(), asList( getDependenciesToScan() ) );

                for ( Artifact artifact : dependenciesToScan )
                {
                    String type = artifact.getType();
                    File out = artifact.getFile();
                    if ( out == null || !out.exists()
                            || !( "jar".equals( type ) || out.isDirectory() || out.getName().endsWith( ".jar" ) ) )
                    {
                        continue;
                    }

                    if ( out.isFile() )
                    {
                        DependencyScanner scanner =
                                new DependencyScanner( singletonList( out ), getIncludedAndExcludedTests() );
                        result = result == null ? scanner.scan() : result.append( scanner.scan() );
                    }
                    else if ( out.isDirectory() )
                    {
                        DirectoryScanner scanner =
                                new DirectoryScanner( out, getIncludedAndExcludedTests() );
                        result = result == null ? scanner.scan() : result.append( scanner.scan() );
                    }
                }

                return result;
            }
            catch ( Exception e )
            {
                throw new MojoFailureException( e.getLocalizedMessage(), e );
            }
        }
    }

    boolean verifyParameters()
        throws MojoFailureException, MojoExecutionException
    {
        setProperties( new SurefireProperties( getProperties() ) );
        if ( isSkipExecution() )
        {
            getConsoleLogger().info( "Tests are skipped." );
            return false;
        }

        String jvmToUse = getJvm();
        if ( toolchain != null )
        {
            getConsoleLogger().info( "Toolchain in maven-" + getPluginName() + "-plugin: " + toolchain );
            if ( jvmToUse != null )
            {
                getConsoleLogger().warning( "Toolchains are ignored, 'jvm' parameter is set to " + jvmToUse );
            }
        }

        if ( !getTestClassesDirectory().exists()
            && ( getDependenciesToScan() == null || getDependenciesToScan().length == 0 ) )
        {
            if ( getFailIfNoTests() )
            {
                throw new MojoFailureException( "No tests to run!" );
            }
            getConsoleLogger().info( "No tests to run." );
        }
        else
        {
            ensureEnableProcessChecker();
            convertDeprecatedForkMode();
            ensureWorkingDirectoryExists();
            ensureParallelRunningCompatibility();
            ensureThreadCountWithPerThread();
            warnIfUselessUseSystemClassLoaderParameter();
            warnIfDefunctGroupsCombinations();
            warnIfRerunClashes();
            warnIfWrongShutdownValue();
            warnIfNotApplicableSkipAfterFailureCount();
            warnIfIllegalTempDir();
            warnIfForkCountIsZero();
            warnIfIllegalFailOnFlakeCount();
            printDefaultSeedIfNecessary();
        }
        return true;
    }

    private void warnIfForkCountIsZero()
    {
        if ( "0".equals( getForkCount() ) )
        {
            getConsoleLogger().warning( "The parameter forkCount should likely not be 0. Forking a JVM for tests "
                + "improves test accuracy. Ensure to have a <forkCount> >= 1." );
        }
    }

    private void executeAfterPreconditionsChecked( @Nonnull DefaultScanResult scanResult, @Nonnull Platform platform )
        throws MojoExecutionException, MojoFailureException
    {
        TestClassPath testClasspath = generateTestClasspath();
        List<ProviderInfo> providers = createProviders( testClasspath );
        ResolvePathResultWrapper wrapper = findModuleDescriptor( platform.getJdkExecAttributesForTests().getJdkHome() );

        RunResult current = noTestsRun();

        Exception firstForkException = null;
        for ( ProviderInfo provider : providers )
        {
            try
            {
                current =
                    current.aggregate( executeProvider( provider, scanResult, testClasspath, platform, wrapper ) );
            }
            catch ( SurefireBooterForkException | SurefireExecutionException | TestSetFailedException e )
            {
                if ( firstForkException == null )
                {
                    firstForkException = e;
                }
            }
        }

        if ( firstForkException != null )
        {
            current = failure( current, firstForkException );
        }

        handleSummary( current, firstForkException );
    }

    protected List<ProviderInfo> createProviders( TestClassPath testClasspath )
        throws MojoExecutionException
    {
        Artifact junitDepArtifact = getJunitDepArtifact();
        return providerDetector.resolve( new DynamicProviderInfo( null ),
            new JUnitPlatformProviderInfo( getJUnitPlatformRunnerArtifact(), getJUnit5Artifact(), testClasspath ),
            new TestNgProviderInfo( getTestNgArtifact() ),
            new JUnitCoreProviderInfo( getJunitArtifact(), junitDepArtifact ),
            new JUnit4ProviderInfo( getJunitArtifact(), junitDepArtifact ),
            new JUnit3ProviderInfo() );
    }

    private SurefireProperties setupProperties()
    {
        SurefireProperties sysProps = null;
        try
        {
            sysProps = SurefireProperties.loadProperties( getSystemPropertiesFile() );
        }
        catch ( IOException e )
        {
            String msg = "The file '" + getSystemPropertiesFile().getAbsolutePath() + "' can't be read.";
            if ( getConsoleLogger().isDebugEnabled() )
            {
                getConsoleLogger().debug( msg, e );
            }
            else
            {
                getConsoleLogger().warning( msg );
            }
        }

        SurefireProperties result =
            SurefireProperties.calculateEffectiveProperties( getSystemProperties(), getSystemPropertyVariables(),
                                                             getUserProperties(), sysProps );

        result.setProperty( "basedir", getBasedir().getAbsolutePath() );
        result.setProperty( "localRepository", getLocalRepository().getBasedir() );
        if ( isForking() )
        {
            for ( Object o : result.propertiesThatCannotBeSetASystemProperties() )
            {
                if ( getArgLine() == null || !getArgLine().contains( "-D" + o + "=" ) )
                {
                    getConsoleLogger().warning( o + " cannot be set as system property, use <argLine>-D"
                                                        + o + "=...</argLine> instead"
                    );
                }
            }
            for ( Object systemPropertyMatchingArgLine : systemPropertiesMatchingArgLine( result ) )
            {
                getConsoleLogger()
                        .warning( "The system property "
                                          + systemPropertyMatchingArgLine
                                          + " is configured twice! "
                                          + "The property appears in <argLine/> and any of <systemPropertyVariables/>, "
                                          + "<systemProperties/> or user property."
                        );
            }
        }
        else
        {
            result.setProperty( "user.dir", getWorkingDirectory().getAbsolutePath() );
        }

        if ( getConsoleLogger().isDebugEnabled() )
        {
            showToLog( result, getConsoleLogger() );
        }

        return result;
    }

    private Set<Object> systemPropertiesMatchingArgLine( SurefireProperties result )
    {
        Set<Object> intersection = new HashSet<>();
        if ( isNotBlank( getArgLine() ) )
        {
            for ( Object systemProperty : result.getStringKeySet() )
            {
                if ( getArgLine().contains( "-D" + systemProperty + "=" ) )
                {
                    intersection.add( systemProperty );
                }
            }

            Set<Object> ignored = result.propertiesThatCannotBeSetASystemProperties();
            intersection.removeAll( ignored );
        }
        return intersection;
    }

    private void showToLog( SurefireProperties props, ConsoleLogger log )
    {
        for ( Object key : props.getStringKeySet() )
        {
            String value = props.getProperty( (String) key );
            log.debug( "Setting system property [" + key + "]=[" + value + "]" );
        }
    }

    @Nonnull
    private RunResult executeProvider( @Nonnull ProviderInfo provider, @Nonnull DefaultScanResult scanResult,
                                       @Nonnull TestClassPath testClasspathWrapper, @Nonnull Platform platform,
                                       @Nonnull ResolvePathResultWrapper resolvedJavaModularityResult )
        throws MojoExecutionException, MojoFailureException, SurefireExecutionException, SurefireBooterForkException,
        TestSetFailedException
    {
        getConsoleLogger().debug( "Using the provider " + provider.getProviderName() );
        SurefireProperties effectiveProperties = setupProperties();
        ClassLoaderConfiguration classLoaderConfiguration = getClassLoaderConfiguration();
        provider.addProviderProperties();
        RunOrderParameters runOrderParameters =
            new RunOrderParameters( getRunOrder(), getStatisticsFile( getConfigChecksum() ), getRunOrderRandomSeed() );

        if ( isNotForking() )
        {
            Properties originalSystemProperties = (Properties) System.getProperties().clone();
            try
            {
                createCopyAndReplaceForkNumPlaceholder( effectiveProperties, 1 ).copyToSystemProperties();

                InPluginVMSurefireStarter surefireStarter = createInprocessStarter( provider, classLoaderConfiguration,
                        runOrderParameters, scanResult, platform, testClasspathWrapper );
                return surefireStarter.runSuitesInProcess( scanResult );
            }
            finally
            {
                System.setProperties( originalSystemProperties );
            }
        }
        else
        {
            ForkConfiguration forkConfiguration = createForkConfiguration( platform, resolvedJavaModularityResult );
            if ( getConsoleLogger().isDebugEnabled() )
            {
                showMap( getEnvironmentVariables(), "environment variable" );
                showArray( getExcludedEnvironmentVariables(), "excluded environment variable" );
            }

            Properties originalSystemProperties = (Properties) System.getProperties().clone();
            ForkStarter forkStarter = null;
            try
            {
                forkStarter = createForkStarter( provider, forkConfiguration, classLoaderConfiguration,
                                                       runOrderParameters, getConsoleLogger(), scanResult,
                                                       testClasspathWrapper, platform, resolvedJavaModularityResult );

                return forkStarter.run( effectiveProperties, scanResult );
            }
            catch ( SurefireBooterForkException e )
            {
                forkStarter.killOrphanForks();
                throw e;
            }
            finally
            {
                System.setProperties( originalSystemProperties );
                cleanupForkConfiguration( forkConfiguration );
            }
        }
    }

    public static SurefireProperties createCopyAndReplaceForkNumPlaceholder(
        SurefireProperties effectiveSystemProperties, int threadNumber )
    {
        SurefireProperties filteredProperties = new SurefireProperties( ( KeyValueSource) effectiveSystemProperties );
        for ( Entry<Object, Object> entry : effectiveSystemProperties.entrySet() )
        {
            if ( entry.getValue() instanceof String )
            {
                String value = (String) entry.getValue();
                filteredProperties.put( entry.getKey(), replaceThreadNumberPlaceholders( value, threadNumber ) );
            }
        }
        return filteredProperties;
    }

    protected void cleanupForkConfiguration( ForkConfiguration forkConfiguration )
    {
        if ( !getConsoleLogger().isDebugEnabled() && forkConfiguration != null )
        {
            File tempDirectory = forkConfiguration.getTempDirectory();
            try
            {
                FileUtils.deleteDirectory( tempDirectory );
            }
            catch ( IOException e )
            {
                getConsoleLogger()
                        .warning( "Could not delete temp directory " + tempDirectory + " because " + e.getMessage() );
            }
        }
    }

    protected void logReportsDirectory()
    {
        logDebugOrCliShowErrors(
            capitalizeFirstLetter( getPluginName() ) + " report directory: " + getReportsDirectory() );
    }

    public RepositorySystem getRepositorySystem()
    {
        return repositorySystem;
    }

    public void setRepositorySystem( RepositorySystem repositorySystem )
    {
        this.repositorySystem = repositorySystem;
    }

    private boolean existsModuleDescriptor( ResolvePathResultWrapper resolvedJavaModularityResult )
    {
        return resolvedJavaModularityResult.getResolvePathResult() != null;
    }

    private ResolvePathResultWrapper findModuleDescriptor( File jdkHome )
    {
        ResolvePathResultWrapper test = findModuleDescriptor( jdkHome, getTestClassesDirectory(), false );
        return test.getResolvePathResult() == null ? findModuleDescriptor( jdkHome, getMainBuildPath(), true ) : test;
    }

    private ResolvePathResultWrapper findModuleDescriptor( File jdkHome, File buildPath, boolean isMainDescriptor )
    {
        boolean isJpmsModule =
            buildPath.isDirectory() ? new File( buildPath, "module-info.class" ).exists() : isModule( buildPath );

        if ( !isJpmsModule )
        {
            return new ResolvePathResultWrapper( null, isMainDescriptor );
        }

        try
        {
            ResolvePathRequest<?> request = ResolvePathRequest.ofFile( buildPath ).setJdkHome( jdkHome );
            ResolvePathResult result = getLocationManager().resolvePath( request );
            boolean isEmpty = result.getModuleNameSource() == null;
            return new ResolvePathResultWrapper( isEmpty ? null : result, isMainDescriptor );
        }
        catch ( Exception e )
        {
            return new ResolvePathResultWrapper( null, isMainDescriptor );
        }
    }

    private static boolean isModule( File jar )
    {
        try ( ZipFile zip = new ZipFile( jar ) )
        {
            return zip.getEntry( "module-info.class" ) != null;
        }
        catch ( IOException e )
        {
            return false;
        }
    }

    private boolean canExecuteProviderWithModularPath( @Nonnull Platform platform,
                                                       @Nonnull ResolvePathResultWrapper resolvedJavaModularityResult )
    {
        return useModulePath()
                && platform.getJdkExecAttributesForTests().isJava9AtLeast()
                && existsModuleDescriptor( resolvedJavaModularityResult );
    }

    /**
     * Converts old TestNG configuration parameters over to new properties based configuration
     * method. (if any are defined the old way)
     */
    private void convertTestNGParameters() throws MojoExecutionException
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

        Artifact testNgArtifact = getTestNgArtifact();
        if ( testNgArtifact != null )
        {
            DefaultArtifactVersion defaultArtifactVersion = new DefaultArtifactVersion( testNgArtifact.getVersion() );
            getProperties().setProperty( "testng.configurator", getConfiguratorName( defaultArtifactVersion,
                                                                                           getConsoleLogger()
                    )
            );
        }
    }

    private static String getConfiguratorName( ArtifactVersion version, PluginConsoleLogger log )
        throws MojoExecutionException
    {
        try
        {
            VersionRange range = VersionRange.createFromVersionSpec( "[4.7,5.2)" );
            if ( range.containsVersion( version ) )
            {
                return "org.apache.maven.surefire.testng.conf.TestNG4751Configurator";
            }
            range = VersionRange.createFromVersionSpec( "[5.2,5.3)" );
            if ( range.containsVersion( version ) )
            {
                return "org.apache.maven.surefire.testng.conf.TestNG52Configurator";
            }
            range = VersionRange.createFromVersionSpec( "[5.3,5.10)" );
            if ( range.containsVersion( version ) )
            {
                return "org.apache.maven.surefire.testng.conf.TestNGMapConfigurator";
            }
            range = VersionRange.createFromVersionSpec( "[5.10,5.13)" );
            if ( range.containsVersion( version ) )
            {
                return "org.apache.maven.surefire.testng.conf.TestNG510Configurator";
            }
            range = VersionRange.createFromVersionSpec( "[5.13,5.14.1)" );
            if ( range.containsVersion( version ) )
            {
                return "org.apache.maven.surefire.testng.conf.TestNG513Configurator";
            }
            range = VersionRange.createFromVersionSpec( "[5.14.1,5.14.3)" );
            if ( range.containsVersion( version ) )
            {
                log.warning( "The 'reporter' or 'listener' may not work properly in TestNG 5.14.1 and 5.14.2." );
                return "org.apache.maven.surefire.testng.conf.TestNG5141Configurator";
            }
            range = VersionRange.createFromVersionSpec( "[5.14.3,6.0)" );
            if ( range.containsVersion( version ) )
            {
                if ( version.equals( new DefaultArtifactVersion( "[5.14.3,5.14.5]" ) ) )
                {
                    throw new MojoExecutionException( "TestNG 5.14.3-5.14.5 is not supported. "
                            + "System dependency org.testng:guice missed path." );
                }
                return "org.apache.maven.surefire.testng.conf.TestNG5143Configurator";
            }
            range = VersionRange.createFromVersionSpec( "[6.0,7.4.0)" );
            if ( range.containsVersion( version ) )
            {
                return "org.apache.maven.surefire.testng.conf.TestNG60Configurator";
            }
            range = VersionRange.createFromVersionSpec( "[7.4.0,)" );
            if ( range.containsVersion( version ) )
            {
                return "org.apache.maven.surefire.testng.conf.TestNG740Configurator";
            }

            throw new MojoExecutionException( "Unknown TestNG version " + version );
        }
        catch ( InvalidVersionSpecificationException invsex )
        {
            throw new MojoExecutionException( "Bug in plugin. Please report it with the attached stacktrace", invsex );
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

    private void convertJunitEngineParameters()
    {
        if ( getIncludeJUnit5Engines() != null && getIncludeJUnit5Engines().length != 0 )
        {
            getProperties()
                .setProperty( INCLUDE_JUNIT5_ENGINES_PROP, join( getIncludeJUnit5Engines() ) );
        }

        if ( getExcludeJUnit5Engines() != null && getExcludeJUnit5Engines().length != 0 )
        {
            getProperties()
                .setProperty( EXCLUDE_JUNIT5_ENGINES_PROP, join( getExcludeJUnit5Engines() ) );
        }
    }

    private static String join( String[] array )
    {
        StringBuilder stringBuilder = new StringBuilder();
        for ( int i = 0, length = array.length; i < length; i++ )
        {
            stringBuilder.append( array[i] );
            if ( i < length - 1 )
            {
                stringBuilder.append( ',' );
            }
        }
        return stringBuilder.toString();
    }

    protected boolean isAnyConcurrencySelected()
    {
        return getParallel() != null && !getParallel().trim().isEmpty();
    }

    protected boolean isAnyGroupsSelected()
    {
        return this.getGroups() != null || this.getExcludedGroups() != null;
    }

    /**
     * Converts old JUnit configuration parameters over to new properties based configuration
     * method. (if any are defined the old way)
     */
    private void convertJunitCoreParameters() throws MojoExecutionException
    {
        checkThreadCountEntity( getThreadCountSuites(), "suites" );
        checkThreadCountEntity( getThreadCountClasses(), "classes" );
        checkThreadCountEntity( getThreadCountMethods(), "methods" );

        String usedParallel = ( getParallel() != null ) ? getParallel() : "none";

        if ( !"none".equals( usedParallel ) )
        {
            checkNonForkedThreads( parallel );
        }

        getProperties().setProperty( ProviderParameterNames.PARALLEL_PROP, usedParallel );
        if ( this.getThreadCount() > 0 )
        {
            getProperties().setProperty( ProviderParameterNames.THREADCOUNT_PROP,
                                         Integer.toString( getThreadCount() ) );
        }
        getProperties().setProperty( "perCoreThreadCount", Boolean.toString( getPerCoreThreadCount() ) );
        getProperties().setProperty( "useUnlimitedThreads", Boolean.toString( getUseUnlimitedThreads() ) );
        getProperties().setProperty( ProviderParameterNames.THREADCOUNTSUITES_PROP,
                                     Integer.toString( getThreadCountSuites() ) );
        getProperties().setProperty( ProviderParameterNames.THREADCOUNTCLASSES_PROP,
                                     Integer.toString( getThreadCountClasses() ) );
        getProperties().setProperty( ProviderParameterNames.THREADCOUNTMETHODS_PROP,
                                     Integer.toString( getThreadCountMethods() ) );
        getProperties().setProperty( ProviderParameterNames.PARALLEL_TIMEOUT_PROP,
                                     Double.toString( getParallelTestsTimeoutInSeconds() ) );
        getProperties().setProperty( ProviderParameterNames.PARALLEL_TIMEOUTFORCED_PROP,
                                     Double.toString( getParallelTestsTimeoutForcedInSeconds() ) );
        getProperties().setProperty( ProviderParameterNames.PARALLEL_OPTIMIZE_PROP,
                                     Boolean.toString( isParallelOptimized() ) );

        String message = "parallel='" + usedParallel + '\''
            + ", perCoreThreadCount=" + getPerCoreThreadCount()
            + ", threadCount=" + getThreadCount()
            + ", useUnlimitedThreads=" + getUseUnlimitedThreads()
            + ", threadCountSuites=" + getThreadCountSuites()
            + ", threadCountClasses=" + getThreadCountClasses()
            + ", threadCountMethods=" + getThreadCountMethods()
            + ", parallelOptimized=" + isParallelOptimized();

        logDebugOrCliShowErrors( message );
    }

    private void checkNonForkedThreads( String parallel ) throws MojoExecutionException
    {
        if ( "suites".equals( parallel ) )
        {
            if ( !( getUseUnlimitedThreads() || getThreadCount() > 0 ^ getThreadCountSuites() > 0 ) )
            {
                throw new MojoExecutionException(
                        "Use threadCount or threadCountSuites > 0 or useUnlimitedThreads=true for parallel='suites'" );
            }
            setThreadCountClasses( 0 );
            setThreadCountMethods( 0 );
        }
        else if ( "classes".equals( parallel ) )
        {
            if ( !( getUseUnlimitedThreads() || getThreadCount() > 0 ^ getThreadCountClasses() > 0 ) )
            {
                throw new MojoExecutionException(
                        "Use threadCount or threadCountClasses > 0 or useUnlimitedThreads=true for parallel='classes'"
                      );
            }
            setThreadCountSuites( 0 );
            setThreadCountMethods( 0 );
        }
        else if ( "methods".equals( parallel ) )
        {
            if ( !( getUseUnlimitedThreads() || getThreadCount() > 0 ^ getThreadCountMethods() > 0 ) )
            {
                throw new MojoExecutionException(
                        "Use threadCount or threadCountMethods > 0 or useUnlimitedThreads=true for parallel='methods'"
                      );
            }
            setThreadCountSuites( 0 );
            setThreadCountClasses( 0 );
        }
        else if ( "suitesAndClasses".equals( parallel ) )
        {
            if ( !( getUseUnlimitedThreads()
                    || onlyThreadCount()
                    || getThreadCountSuites() > 0 && getThreadCountClasses() > 0
                        && getThreadCount() == 0 && getThreadCountMethods() == 0
                    || getThreadCount() > 0 && getThreadCountSuites() > 0 && getThreadCountClasses() > 0
                        && getThreadCountMethods() == 0
                    || getThreadCount() > 0 && getThreadCountSuites() > 0 && getThreadCount() > getThreadCountSuites()
                        && getThreadCountClasses() == 0 && getThreadCountMethods() == 0 ) )
            {
                throw new MojoExecutionException( "Use useUnlimitedThreads=true, "
                        + "or only threadCount > 0, "
                        + "or (threadCountSuites > 0 and threadCountClasses > 0), "
                        + "or (threadCount > 0 and threadCountSuites > 0 and threadCountClasses > 0) "
                        + "or (threadCount > 0 and threadCountSuites > 0 and threadCount > threadCountSuites) "
                        + "for parallel='suitesAndClasses' or 'both'" );
            }
            setThreadCountMethods( 0 );
        }
        else if ( "suitesAndMethods".equals( parallel ) )
        {
            if ( !( getUseUnlimitedThreads()
                    || onlyThreadCount()
                    || getThreadCountSuites() > 0 && getThreadCountMethods() > 0
                        && getThreadCount() == 0 && getThreadCountClasses() == 0
                    || getThreadCount() > 0 && getThreadCountSuites() > 0 && getThreadCountMethods() > 0
                        && getThreadCountClasses() == 0
                    || getThreadCount() > 0 && getThreadCountSuites() > 0 && getThreadCount() > getThreadCountSuites()
                        && getThreadCountClasses() == 0 && getThreadCountMethods() == 0 ) )
            {
                throw new MojoExecutionException( "Use useUnlimitedThreads=true, "
                        + "or only threadCount > 0, "
                        + "or (threadCountSuites > 0 and threadCountMethods > 0), "
                        + "or (threadCount > 0 and threadCountSuites > 0 and threadCountMethods > 0), "
                        + "or (threadCount > 0 and threadCountSuites > 0 and threadCount > threadCountSuites) "
                        + "for parallel='suitesAndMethods'" );
            }
            setThreadCountClasses( 0 );
        }
        else if ( "both".equals( parallel ) || "classesAndMethods".equals( parallel ) )
        {
            if ( !( getUseUnlimitedThreads()
                    || onlyThreadCount()
                    || getThreadCountClasses() > 0 && getThreadCountMethods() > 0
                        && getThreadCount() == 0 && getThreadCountSuites() == 0
                    || getThreadCount() > 0 && getThreadCountClasses() > 0 && getThreadCountMethods() > 0
                        && getThreadCountSuites() == 0
                    || getThreadCount() > 0 && getThreadCountClasses() > 0 && getThreadCount() > getThreadCountClasses()
                        && getThreadCountSuites() == 0 && getThreadCountMethods() == 0 ) )
            {
                throw new MojoExecutionException( "Use useUnlimitedThreads=true, "
                        + "or only threadCount > 0, "
                        + "or (threadCountClasses > 0 and threadCountMethods > 0), "
                        + "or (threadCount > 0 and threadCountClasses > 0 and threadCountMethods > 0), "
                        + "or (threadCount > 0 and threadCountClasses > 0 and threadCount > threadCountClasses) "
                        + "for parallel='both' or parallel='classesAndMethods'" );
            }
            setThreadCountSuites( 0 );
        }
        else if ( "all".equals( parallel ) )
        {
            if ( !( getUseUnlimitedThreads()
                    || onlyThreadCount()
                    || getThreadCountSuites() > 0 && getThreadCountClasses() > 0 && getThreadCountMethods() > 0
                    || getThreadCount() > 0 && getThreadCountSuites() > 0 && getThreadCountClasses() > 0
                        && getThreadCountMethods() == 0
                        && getThreadCount() > ( getThreadCountSuites() + getThreadCountClasses() ) ) )
            {
                throw new MojoExecutionException( "Use useUnlimitedThreads=true, "
                        + "or only threadCount > 0, "
                        + "or (threadCountSuites > 0 and threadCountClasses > 0 and threadCountMethods > 0), "
                        + "or every thread-count is specified, "
                        + "or (threadCount > 0 and threadCountSuites > 0 and threadCountClasses > 0 "
                        + "and threadCount > threadCountSuites + threadCountClasses) "
                        + "for parallel='all'" );
            }
        }
        else
        {
            throw new MojoExecutionException( "Illegal parallel='" + parallel + "'" );
        }
    }

    private boolean onlyThreadCount()
    {
        return getThreadCount() > 0 && getThreadCountSuites() == 0 && getThreadCountClasses() == 0
                && getThreadCountMethods() == 0;
    }

    private static void checkThreadCountEntity( int count, String entity )
        throws MojoExecutionException
    {
        if ( count < 0 )
        {
            throw new MojoExecutionException(
                    "parallel maven execution does not allow negative thread-count" + entity );
        }
    }

    private boolean isJunit47Compatible( Artifact artifact )
    {
        return isWithinVersionSpec( artifact, "[4.7,)" );
    }

    private boolean isAnyJunit4( Artifact artifact )
    {
        return isWithinVersionSpec( artifact, "[4.0,)" );
    }

    private static boolean isForkModeNever( String forkMode )
    {
        return FORK_NEVER.equals( forkMode );
    }

    protected boolean isForking()
    {
        return 0 < getEffectiveForkCount();
    }

    String getEffectiveForkMode()
    {
        String forkMode1 = getForkMode();

        if ( toolchain != null && isForkModeNever( forkMode1 ) )
        {
            return FORK_ONCE;
        }

        return getEffectiveForkMode( forkMode1 );
    }

    private List<RunOrder> getRunOrders()
    {
        String runOrderString = getRunOrder();
        RunOrder[] runOrder = runOrderString == null ? RunOrder.DEFAULT : RunOrder.valueOfMulti( runOrderString );
        return asList( runOrder );
    }

    private boolean requiresRunHistory()
    {
        final List<RunOrder> runOrders = getRunOrders();
        return runOrders.contains( RunOrder.BALANCED ) || runOrders.contains( RunOrder.FAILEDFIRST );
    }

    private PluginFailureReason getEffectiveFailIfNoTests()
    {
        if ( isSpecificTestSpecified() )
        {
            return getFailIfNoSpecifiedTests() ? COULD_NOT_RUN_SPECIFIED_TESTS : NONE;
        }
        else
        {
            return getFailIfNoTests() ? COULD_NOT_RUN_DEFAULT_TESTS : NONE;
        }
    }

    private ProviderConfiguration createProviderConfiguration( RunOrderParameters runOrderParameters )
        throws MojoExecutionException, MojoFailureException
    {
        final ReporterConfiguration reporterConfiguration =
            new ReporterConfiguration( getReportsDirectory(), isTrimStackTrace() );

        final Artifact testNgArtifact = getTestNgArtifact();
        final boolean isTestNg = testNgArtifact != null;
        final TestArtifactInfo testNg =
            isTestNg ? new TestArtifactInfo( testNgArtifact.getVersion(), testNgArtifact.getClassifier() ) : null;
        final TestRequest testSuiteDefinition = new TestRequest( suiteXmlFiles(),
                                                                 getTestSourceDirectory(),
                                                                 getSpecificTests(),
                                                                 getRerunFailingTestsCount() );

        final boolean actualFailIfNoTests;
        DirectoryScannerParameters directoryScannerParameters = null;
        if ( hasSuiteXmlFiles() && !isSpecificTestSpecified() )
        {
            actualFailIfNoTests = getFailIfNoTests();
            if ( !isTestNg )
            {
                throw new MojoExecutionException( "suiteXmlFiles is configured, but there is no TestNG dependency" );
            }
        }
        else
        {
            // @todo remove these three params and use DirectoryScannerParameters to pass into DirectoryScanner only
            // @todo or remove it in next major version :: 3.0
            // @todo remove deprecated methods in ProviderParameters => included|excluded|specificTests not needed here

            List<String> actualIncludes = getIncludeList(); // Collections.emptyList(); behaves same
            List<String> actualExcludes = getExcludeList(); // Collections.emptyList(); behaves same
            // Collections.emptyList(); behaves same
            List<String> specificTests = Collections.emptyList();

            directoryScannerParameters =
                new DirectoryScannerParameters( getTestClassesDirectory(), actualIncludes, actualExcludes,
                                                specificTests, getRunOrder() );
        }

        Map<String, String> providerProperties = toStringProperties( getProperties() );

        return new ProviderConfiguration( directoryScannerParameters, runOrderParameters,
                                          reporterConfiguration,
                                          testNg, // Not really used in provider. Limited to de/serializer.
                                          testSuiteDefinition, providerProperties, null,
                                          false, cli, getSkipAfterFailureCount(),
                                          Shutdown.parameterOf( getShutdown() ),
                                          getForkedProcessExitTimeoutInSeconds() );
    }

    private static Map<String, String> toStringProperties( Properties properties )
    {
        Map<String, String> h = new ConcurrentHashMap<>( properties.size() );
        for ( Enumeration<?> e = properties.keys() ; e.hasMoreElements() ; )
        {
            Object k = e.nextElement();
            Object v = properties.get( k );
            if ( k.getClass() == String.class && v.getClass() == String.class )
            {
                h.put( (String) k, (String) v );
            }
        }
        return h;
    }

    private File getStatisticsFile( String configurationHash )
    {
        return new File( getBasedir(), ".surefire-" + configurationHash );
    }

    private StartupConfiguration createStartupConfiguration( @Nonnull ProviderInfo provider, boolean isForking,
                                                             @Nonnull ClassLoaderConfiguration classLoaderConfiguration,
                                                             @Nonnull DefaultScanResult scanResult,
                                                             @Nonnull TestClassPath testClasspathWrapper,
                                                             @Nonnull Platform platform,
                                                             @Nonnull ResolvePathResultWrapper resolvedJavaModularity )
        throws MojoExecutionException
    {
        try
        {
            if ( isForking && canExecuteProviderWithModularPath( platform, resolvedJavaModularity ) )
            {
                File jdkHome = platform.getJdkExecAttributesForTests().getJdkHome();
                return newStartupConfigWithModularPath( classLoaderConfiguration, provider, resolvedJavaModularity,
                    scanResult, jdkHome.getAbsolutePath(), testClasspathWrapper );
            }
            else
            {
                return newStartupConfigWithClasspath( classLoaderConfiguration, provider, testClasspathWrapper );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

    private StartupConfiguration newStartupConfigWithClasspath(
        @Nonnull ClassLoaderConfiguration classLoaderConfiguration, @Nonnull ProviderInfo providerInfo,
        @Nonnull TestClassPath testClasspathWrapper ) throws MojoExecutionException
    {
        Classpath testClasspath = testClasspathWrapper.toClasspath();
        Set<Artifact> providerArtifacts = providerInfo.getProviderClasspath();
        String providerName = providerInfo.getProviderName();
        Classpath providerClasspath = classpathCache.getCachedClassPath( providerName );
        if ( providerClasspath == null )
        {
            providerClasspath = classpathCache.setCachedClasspath( providerName, providerArtifacts );
        }

        getConsoleLogger().debug( testClasspath.getLogMessage( "test classpath:" ) );
        getConsoleLogger().debug( providerClasspath.getLogMessage( "provider classpath:" ) );
        getConsoleLogger().debug( testClasspath.getCompactLogMessage( "test(compact) classpath:" ) );
        getConsoleLogger().debug( providerClasspath.getCompactLogMessage( "provider(compact) classpath:" ) );

        Artifact[] additionalInProcArtifacts = { getCommonArtifact(), getBooterArtifact(), getExtensionsArtifact(),
            getApiArtifact(), getSpiArtifact(), getLoggerApiArtifact(), getSurefireSharedUtilsArtifact() };
        Set<Artifact> inProcArtifacts = retainInProcArtifactsUnique( providerArtifacts, additionalInProcArtifacts );
        Classpath inProcClasspath = createInProcClasspath( providerClasspath, inProcArtifacts );
        getConsoleLogger().debug( inProcClasspath.getLogMessage( "in-process classpath:" ) );
        getConsoleLogger().debug( inProcClasspath.getCompactLogMessage( "in-process(compact) classpath:" ) );

        ClasspathConfiguration classpathConfiguration = new ClasspathConfiguration( testClasspath, providerClasspath,
                inProcClasspath, effectiveIsEnableAssertions(), isChildDelegation() );
        ProviderRequirements forkRequirements = new ProviderRequirements( false, false, false );
        return new StartupConfiguration( providerName, classpathConfiguration, classLoaderConfiguration,
            ProcessCheckerType.toEnum( getEnableProcessChecker() ), providerInfo.getJpmsArguments( forkRequirements ) );
    }

    private static Set<Artifact> retainInProcArtifactsUnique( Set<Artifact> providerArtifacts,
                                                         Artifact... inPluginArtifacts )
    {
        Set<Artifact> result = new LinkedHashSet<>();
        for ( Artifact inPluginArtifact : inPluginArtifacts )
        {
            boolean contains = false;
            for ( Artifact providerArtifact : providerArtifacts )
            {
                if ( hasGroupArtifactId( providerArtifact.getGroupId(), providerArtifact.getArtifactId(),
                        inPluginArtifact ) )
                {
                    contains = true;
                    break;
                }
            }
            if ( !contains )
            {
                result.add( inPluginArtifact );
            }
        }
        return result;
    }

    private static boolean hasGroupArtifactId( String groupId, String artifactId, Artifact artifact )
    {
        return groupId.equals( artifact.getGroupId() ) && artifactId.equals( artifact.getArtifactId() );
    }

    private static Classpath createInProcClasspath( Classpath providerClasspath, Set<Artifact> newArtifacts )
    {
        Classpath inprocClasspath = providerClasspath.clone();
        for ( Artifact newArtifact : newArtifacts )
        {
            inprocClasspath = inprocClasspath.addClassPathElementUrl( newArtifact.getFile().getAbsolutePath() );
        }
        return inprocClasspath;
    }

    /**
     * For testing purposes - Mockito.
     * @return plexus component
     */
    private LocationManager getLocationManager()
    {
        return locationManager;
    }

    private StartupConfiguration newStartupConfigWithModularPath(
        @Nonnull ClassLoaderConfiguration classLoaderConfiguration, @Nonnull ProviderInfo providerInfo,
        @Nonnull ResolvePathResultWrapper moduleDescriptor, @Nonnull DefaultScanResult scanResult,
        @Nonnull String javaHome, @Nonnull TestClassPath testClasspathWrapper )
            throws MojoExecutionException, IOException
    {
        boolean isMainDescriptor = moduleDescriptor.isMainModuleDescriptor();
        JavaModuleDescriptor javaModuleDescriptor = moduleDescriptor.getResolvePathResult().getModuleDescriptor();
        SortedSet<String> packages = new TreeSet<>();

        Classpath testClasspath = testClasspathWrapper.toClasspath();
        Set<Artifact> providerArtifacts = providerInfo.getProviderClasspath();
        String providerName = providerInfo.getProviderName();
        Classpath providerClasspath = classpathCache.getCachedClassPath( providerName );
        if ( providerClasspath == null )
        {
            providerClasspath = classpathCache.setCachedClasspath( providerName, providerArtifacts );
        }

        final ProviderRequirements providerRequirements;
        final Classpath testModulepath;
        if ( isMainDescriptor )
        {
            providerRequirements = new ProviderRequirements( true, true, false );
            ResolvePathsRequest<String> req = ResolvePathsRequest.ofStrings( testClasspath.getClassPath() )
                    .setIncludeAllProviders( true )
                    .setJdkHome( javaHome )
                    .setModuleDescriptor( javaModuleDescriptor );

            ResolvePathsResult<String> result = getLocationManager().resolvePaths( req );
            for ( Entry<String, Exception> entry : result.getPathExceptions().entrySet() )
            {
                // Probably JDK version < 9. Other known causes: passing a non-jar or a corrupted jar.
                getConsoleLogger()
                        .warning( "Exception for '" + entry.getKey() + "'.", entry.getValue() );
            }

            testClasspath = new Classpath( result.getClasspathElements() );
            testModulepath = new Classpath( result.getModulepathElements().keySet() );

            for ( String className : scanResult.getClasses() )
            {
                packages.add( substringBeforeLast( className, "." ) );
            }
        }
        else
        {
            providerRequirements = new ProviderRequirements( true, false, true );
            testModulepath = testClasspath;
            testClasspath = emptyClasspath();
        }

        getConsoleLogger().debug( "main module descriptor name: " + javaModuleDescriptor.name() );

        ModularClasspath modularClasspath = new ModularClasspath( javaModuleDescriptor.name(),
            testModulepath.getClassPath(), packages, isMainDescriptor ? getTestClassesDirectory() : null,
            isMainDescriptor );

        Artifact[] additionalInProcArtifacts = { getCommonArtifact(), getBooterArtifact(), getExtensionsArtifact(),
            getApiArtifact(), getSpiArtifact(), getLoggerApiArtifact(), getSurefireSharedUtilsArtifact() };
        Set<Artifact> inProcArtifacts = retainInProcArtifactsUnique( providerArtifacts, additionalInProcArtifacts );
        Classpath inProcClasspath = createInProcClasspath( providerClasspath, inProcArtifacts );

        ModularClasspathConfiguration classpathConfiguration = new ModularClasspathConfiguration( modularClasspath,
                testClasspath, providerClasspath, inProcClasspath, effectiveIsEnableAssertions(), isChildDelegation() );

        getConsoleLogger().debug( testClasspath.getLogMessage( "test classpath:" ) );
        getConsoleLogger().debug( testModulepath.getLogMessage( "test modulepath:" ) );
        getConsoleLogger().debug( providerClasspath.getLogMessage( "provider classpath:" ) );
        getConsoleLogger().debug( testClasspath.getCompactLogMessage( "test(compact) classpath:" ) );
        getConsoleLogger().debug( testModulepath.getCompactLogMessage( "test(compact) modulepath:" ) );
        getConsoleLogger().debug( providerClasspath.getCompactLogMessage( "provider(compact) classpath:" ) );
        getConsoleLogger().debug( inProcClasspath.getLogMessage( "in-process classpath:" ) );
        getConsoleLogger().debug( inProcClasspath.getCompactLogMessage( "in-process(compact) classpath:" ) );

        ProcessCheckerType processCheckerType = ProcessCheckerType.toEnum( getEnableProcessChecker() );
        List<String[]> jpmsArgs = providerInfo.getJpmsArguments( providerRequirements );
        return new StartupConfiguration( providerName, classpathConfiguration, classLoaderConfiguration,
            processCheckerType, jpmsArgs );
    }

    private Artifact getCommonArtifact()
    {
        return getPluginArtifactMap().get( "org.apache.maven.surefire:maven-surefire-common" );
    }

    private Artifact getExtensionsArtifact()
    {
        return getPluginArtifactMap().get( "org.apache.maven.surefire:surefire-extensions-api" );
    }

    private Artifact getSpiArtifact()
    {
        return getPluginArtifactMap().get( "org.apache.maven.surefire:surefire-extensions-spi" );
    }

    private Artifact getApiArtifact()
    {
        return getPluginArtifactMap().get( "org.apache.maven.surefire:surefire-api" );
    }

    private Artifact getSurefireSharedUtilsArtifact()
    {
        return getPluginArtifactMap().get( "org.apache.maven.surefire:surefire-shared-utils" );
    }

    private Artifact getLoggerApiArtifact()
    {
        return getPluginArtifactMap().get( "org.apache.maven.surefire:surefire-logger-api" );
    }

    private Artifact getBooterArtifact()
    {
        return getPluginArtifactMap().get( "org.apache.maven.surefire:surefire-booter" );
    }

    private Artifact getShadefireArtifact()
    {
        return getPluginArtifactMap().get( "org.apache.maven.surefire:surefire-shadefire" );
    }

    private StartupReportConfiguration getStartupReportConfiguration( String configChecksum, boolean isForkMode )
    {
        SurefireStatelessReporter xmlReporter =
                statelessTestsetReporter == null
                        ? new SurefireStatelessReporter( /*todo call def. constr.*/ isDisableXmlReport(), "3.0" )
                        : statelessTestsetReporter;

        xmlReporter.setDisable( isDisableXmlReport() ); // todo change to Boolean in the version 3.0.0-M6

        SurefireConsoleOutputReporter outReporter =
                consoleOutputReporter == null ? new SurefireConsoleOutputReporter() : consoleOutputReporter;

        SurefireStatelessTestsetInfoReporter testsetReporter =
                statelessTestsetInfoReporter == null
                        ? new SurefireStatelessTestsetInfoReporter() : statelessTestsetInfoReporter;

        return new StartupReportConfiguration( isUseFile(), isPrintSummary(), getReportFormat(),
                                               isRedirectTestOutputToFile(),
                                               getReportsDirectory(), isTrimStackTrace(), getReportNameSuffix(),
                                               getStatisticsFile( configChecksum ), requiresRunHistory(),
                                               getRerunFailingTestsCount(), getReportSchemaLocation(), getEncoding(),
                                               isForkMode, xmlReporter, outReporter, testsetReporter );
    }

    private boolean isSpecificTestSpecified()
    {
        return isNotBlank( getTest() );
    }

    @Nonnull private List<String> readListFromFile( @Nonnull final File file )
    {
        getConsoleLogger().debug( "Reading list from: " + file );

        if ( !file.exists() )
        {
            throw new RuntimeException( "Failed to load list from file: " + file );
        }

        try
        {
            List<String> list = FileUtils.loadFile( file );

            if ( getConsoleLogger().isDebugEnabled() )
            {
                getConsoleLogger().debug( "List contents:" );
                for ( String entry : list )
                {
                    getConsoleLogger().debug( "  " + entry );
                }
            }
            return list;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Failed to load list from file: " + file, e );
        }
    }

    @Nonnull
    private List<String> getExcludedScanList()
        throws MojoFailureException
    {
        return getExcludeList( true );
    }

    @Nonnull
    private List<String> getExcludeList()
        throws MojoFailureException
    {
        return getExcludeList( false );
    }

    /**
     * Computes a merge list of test exclusions.
     * Used only in {@link #getExcludeList()} and {@link #getExcludedScanList()}.
     * @param asScanList true if dependency or directory scanner
     * @return list of patterns
     * @throws MojoFailureException if the excludes breaks a pattern format
     */
    @Nonnull
    private List<String> getExcludeList( boolean asScanList )
        throws MojoFailureException
    {
        List<String> excludes;
        if ( isSpecificTestSpecified() )
        {
            excludes = Collections.emptyList();
        }
        else
        {
            excludes = new ArrayList<>();
            if ( asScanList )
            {
                if ( getExcludes() != null )
                {
                    excludes.addAll( getExcludes() );
                }
                checkMethodFilterInIncludesExcludes( excludes );
            }

            if ( getExcludesFile() != null )
            {
                excludes.addAll( readListFromFile( getExcludesFile() ) );
            }

            if ( asScanList && excludes.isEmpty() )
            {
                excludes = Collections.singletonList( getDefaultExcludes() );
            }
        }
        return filterNulls( excludes );
    }

    @Nonnull
    private List<String> getIncludedScanList()
        throws MojoFailureException
    {
        return getIncludeList( true );
    }

    @Nonnull
    private List<String> getIncludeList()
        throws MojoFailureException
    {
        return getIncludeList( false );
    }

    /**
     * Computes a merge list of test inclusions.
     * Used only in {@link #getIncludeList()} and {@link #getIncludedScanList()}.
     * @param asScanList true if dependency or directory scanner
     * @return list of patterns
     * @throws MojoFailureException if the includes breaks a pattern format
     */
    @Nonnull
    private List<String> getIncludeList( boolean asScanList )
        throws MojoFailureException
    {
        final List<String> includes = new ArrayList<>();
        if ( isSpecificTestSpecified() )
        {
            addAll( includes, split( getTest(), "," ) );
        }
        else
        {
            if ( asScanList )
            {
                if ( getIncludes() != null )
                {
                    includes.addAll( getIncludes() );
                }
                checkMethodFilterInIncludesExcludes( includes );
            }

            if ( getIncludesFile() != null )
            {
                includes.addAll( readListFromFile( getIncludesFile() ) );
            }

            if ( asScanList && includes.isEmpty() )
            {
                addAll( includes, getDefaultIncludes() );
            }
        }

        return filterNulls( includes );
    }

    private void checkMethodFilterInIncludesExcludes( Iterable<String> patterns )
        throws MojoFailureException
    {
        for ( String pattern : patterns )
        {
            if ( pattern != null && pattern.contains( "#" ) )
            {
                throw new MojoFailureException( "Method filter prohibited in includes|excludes parameter: "
                    + pattern );
            }
        }
    }

    private TestListResolver getIncludedAndExcludedTests()
        throws MojoFailureException
    {
        if ( includedExcludedTests == null )
        {
            includedExcludedTests = new TestListResolver( getIncludedScanList(), getExcludedScanList() );
            getConsoleLogger().debug( "Resolved included and excluded patterns: " + includedExcludedTests );
        }
        return includedExcludedTests;
    }

    public TestListResolver getSpecificTests()
        throws MojoFailureException
    {
        if ( specificTests == null )
        {
            specificTests = new TestListResolver( getIncludeList(), getExcludeList() );
        }
        return specificTests;
    }

    @Nonnull private List<String> filterNulls( @Nonnull List<String> toFilter )
    {
        List<String> result = new ArrayList<>( toFilter.size() );
        for ( String item : toFilter )
        {
            if ( item != null )
            {
                item = item.trim();
                if ( !item.isEmpty() )
                {
                    result.add( item );
                }
            }
        }

        return result;
    }

    private Artifact getTestNgArtifact()
        throws MojoExecutionException
    {
        Artifact artifact = getProjectArtifactMap().get( getTestNGArtifactName() );
        Artifact projectArtifact = project.getArtifact();
        String projectArtifactName = projectArtifact.getGroupId() + ":" + projectArtifact.getArtifactId();

        if ( artifact != null )
        {
            VersionRange range = createVersionRange();
            if ( !range.containsVersion( new DefaultArtifactVersion( artifact.getVersion() ) ) )
            {
                throw new MojoExecutionException(
                    "TestNG support requires version 4.7 or above. You have declared version "
                        + artifact.getVersion() );
            }
        }
        else if ( projectArtifactName.equals( getTestNGArtifactName() ) )
        {
            artifact = projectArtifact;
        }

        return artifact;

    }

    private VersionRange createVersionRange()
    {
        try
        {
            return VersionRange.createFromVersionSpec( "[4.7,)" );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new RuntimeException( e );
        }
    }

    private Artifact getJunitArtifact()
    {
        Artifact artifact = getProjectArtifactMap().get( getJunitArtifactName() );
        Artifact projectArtifact = project.getArtifact();
        String projectArtifactName = projectArtifact.getGroupId() + ":" + projectArtifact.getArtifactId();

        if ( artifact == null && projectArtifactName.equals( getJunitArtifactName() ) )
        {
            artifact = projectArtifact;
        }

        return artifact;
    }

    private Artifact getJunitDepArtifact()
    {
        return getProjectArtifactMap().get( "junit:junit-dep" );
    }

    private Artifact getJUnitPlatformRunnerArtifact()
    {
        return getProjectArtifactMap().get( "org.junit.platform:junit-platform-runner" );
    }

    private Artifact getJUnit5Artifact()
    {
        Artifact artifact = getPluginArtifactMap().get( "org.junit.platform:junit-platform-engine" );
        if ( artifact == null )
        {
            return getProjectArtifactMap().get( "org.junit.platform:junit-platform-commons" );
        }

        return artifact;
    }

    private ForkStarter createForkStarter( @Nonnull ProviderInfo provider, @Nonnull ForkConfiguration forkConfiguration,
                                           @Nonnull ClassLoaderConfiguration classLoaderConfiguration,
                                           @Nonnull RunOrderParameters runOrderParameters, @Nonnull ConsoleLogger log,
                                           @Nonnull DefaultScanResult scanResult,
                                           @Nonnull TestClassPath testClasspathWrapper, @Nonnull Platform platform,
                                           @Nonnull ResolvePathResultWrapper resolvedJavaModularityResult )
        throws MojoExecutionException, MojoFailureException
    {
        StartupConfiguration startupConfiguration = createStartupConfiguration( provider, true,
                classLoaderConfiguration, scanResult, testClasspathWrapper, platform, resolvedJavaModularityResult );
        String configChecksum = getConfigChecksum();
        StartupReportConfiguration startupReportConfiguration = getStartupReportConfiguration( configChecksum, true );
        ProviderConfiguration providerConfiguration = createProviderConfiguration( runOrderParameters );
        return new ForkStarter( providerConfiguration, startupConfiguration, forkConfiguration,
                                getForkedProcessTimeoutInSeconds(), startupReportConfiguration, log );
    }

    private InPluginVMSurefireStarter createInprocessStarter( @Nonnull ProviderInfo provider,
                                                              @Nonnull ClassLoaderConfiguration classLoaderConfig,
                                                              @Nonnull RunOrderParameters runOrderParameters,
                                                              @Nonnull DefaultScanResult scanResult,
                                                              @Nonnull Platform platform,
                                                              @Nonnull TestClassPath testClasspathWrapper )
        throws MojoExecutionException, MojoFailureException
    {
        StartupConfiguration startupConfiguration = createStartupConfiguration( provider, false, classLoaderConfig,
                scanResult, testClasspathWrapper, platform, new ResolvePathResultWrapper( null, true ) );
        String configChecksum = getConfigChecksum();
        StartupReportConfiguration startupReportConfiguration = getStartupReportConfiguration( configChecksum, false );
        ProviderConfiguration providerConfiguration = createProviderConfiguration( runOrderParameters );
        return new InPluginVMSurefireStarter( startupConfiguration, providerConfiguration, startupReportConfiguration,
                                              getConsoleLogger(), platform );
    }

    // todo this is in separate method and can be better tested than whole method createForkConfiguration()
    @Nonnull
    private ForkNodeFactory getForkNodeFactory()
    {
        ForkNodeFactory forkNode = getForkNode();
        return forkNode == null ? new LegacyForkNodeFactory() : forkNode;
    }

    @Nonnull
    private ForkConfiguration createForkConfiguration( @Nonnull Platform platform,
                                                       @Nonnull ResolvePathResultWrapper resolvedJavaModularityResult )
        throws MojoExecutionException
    {
        File tmpDir = getSurefireTempDir();

        Artifact shadeFire = getShadefireArtifact();

        Classpath bootClasspath = getArtifactClasspath( shadeFire != null ? shadeFire : getBooterArtifact() );

        ForkNodeFactory forkNode = getForkNodeFactory();

        getConsoleLogger().debug( "Found implementation of fork node factory: " + forkNode.getClass().getName() );

        if ( canExecuteProviderWithModularPath( platform, resolvedJavaModularityResult ) )
        {
            return new ModularClasspathForkConfiguration( bootClasspath,
                    tmpDir,
                    getEffectiveDebugForkedProcess(),
                    getWorkingDirectory() != null ? getWorkingDirectory() : getBasedir(),
                    getProject().getModel().getProperties(),
                    getArgLine(),
                    getEnvironmentVariables(),
                    getExcludedEnvironmentVariables(),
                    getConsoleLogger().isDebugEnabled(),
                    getEffectiveForkCount(),
                    reuseForks,
                    platform,
                    getConsoleLogger(),
                    forkNode );
        }
        else if ( getClassLoaderConfiguration().isManifestOnlyJarRequestedAndUsable() )
        {
            return new JarManifestForkConfiguration( bootClasspath,
                    tmpDir,
                    getEffectiveDebugForkedProcess(),
                    getWorkingDirectory() != null ? getWorkingDirectory() : getBasedir(),
                    getProject().getModel().getProperties(),
                    getArgLine(),
                    getEnvironmentVariables(),
                    getExcludedEnvironmentVariables(),
                    getConsoleLogger().isDebugEnabled(),
                    getEffectiveForkCount(),
                    reuseForks,
                    platform,
                    getConsoleLogger(),
                    forkNode );
        }
        else
        {
            return new ClasspathForkConfiguration( bootClasspath,
                    tmpDir,
                    getEffectiveDebugForkedProcess(),
                    getWorkingDirectory() != null ? getWorkingDirectory() : getBasedir(),
                    getProject().getModel().getProperties(),
                    getArgLine(),
                    getEnvironmentVariables(),
                    getExcludedEnvironmentVariables(),
                    getConsoleLogger().isDebugEnabled(),
                    getEffectiveForkCount(),
                    reuseForks,
                    platform,
                    getConsoleLogger(),
                    forkNode );
        }
    }

    private void ensureEnableProcessChecker() throws MojoFailureException
    {
        if ( !ProcessCheckerType.isValid( getEnableProcessChecker() ) )
        {
            throw new MojoFailureException( "Unexpected value '"
                    + getEnableProcessChecker()
                    + "' in the configuration parameter 'enableProcessChecker'." );
        }
    }

    private void convertDeprecatedForkMode()
    {
        String effectiveForkMode = getEffectiveForkMode();
        // FORK_ONCE (default) is represented by the default values of forkCount and reuseForks
        if ( FORK_PERTHREAD.equals( effectiveForkMode ) )
        {
            forkCount = String.valueOf( threadCount );
        }
        else if ( FORK_NEVER.equals( effectiveForkMode ) )
        {
            forkCount = "0";
        }
        else if ( FORK_ALWAYS.equals( effectiveForkMode ) )
        {
            forkCount = "1";
            reuseForks = false;
        }

        if ( !FORK_ONCE.equals( getForkMode() ) )
        {
            getConsoleLogger().warning( "The parameter forkMode is deprecated since version 2.14. "
                                                + "Use forkCount and reuseForks instead." );
        }
    }

    @SuppressWarnings( "checkstyle:emptyblock" )
    protected int getEffectiveForkCount()
    {
        if ( effectiveForkCount < 0 )
        {
            try
            {
                effectiveForkCount = convertWithCoreCount( forkCount );
            }
            catch ( NumberFormatException ignored )
            {
            }

            if ( effectiveForkCount < 0 )
            {
                throw new IllegalArgumentException( "Fork count " + forkCount.trim() + " is not a legal value." );
            }
        }

        return effectiveForkCount;
    }

    protected int convertWithCoreCount( String count )
    {
        String trimmed = count.trim();
        if ( trimmed.endsWith( "C" ) )
        {
            double multiplier = Double.parseDouble( trimmed.substring( 0, trimmed.length() - 1 ) );
            double calculated = multiplier * ( (double) Runtime.getRuntime().availableProcessors() );
            return calculated > 0d ? Math.max( (int) calculated, 1 ) : 0;
        }
        else
        {
            return parseInt( trimmed );
        }
    }

    private String getEffectiveDebugForkedProcess()
    {
        String debugForkedProcess = getDebugForkedProcess();
        if ( "true".equals( debugForkedProcess ) )
        {
            return "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=localhost:5005";
        }
        return debugForkedProcess;
    }

    private JdkAttributes getEffectiveJvm() throws MojoFailureException
    {
        if ( isNotEmpty( getJvm() ) )
        {
            File pathToJava = new File( getJvm() ).getAbsoluteFile();
            if ( !endsWithJavaPath( pathToJava.getPath() ) )
            {
                throw new MojoFailureException( "Given path does not end with java executor \""
                                                        + pathToJava.getPath() + "\"." );
            }

            if ( !( pathToJava.isFile()
                            || "java".equals( pathToJava.getName() ) && pathToJava.getParentFile().isDirectory() ) )
            {
                throw new MojoFailureException( "Given path to java executor does not exist \""
                                                        + pathToJava.getPath() + "\"." );
            }

            File jdkHome = toJdkHomeFromJvmExec( pathToJava.getPath() );
            if ( jdkHome == null )
            {
                getConsoleLogger().warning( "Cannot determine JAVA_HOME of jvm exec path " + pathToJava );
            }
            else if ( !getEnvironmentVariables().containsKey( "JAVA_HOME" ) )
            {
                getEnvironmentVariables().put( "JAVA_HOME", jdkHome.getAbsolutePath() );
            }
            BigDecimal version = jdkHome == null ? null : toJdkVersionFromReleaseFile( jdkHome );
            boolean javaVersion9 = version == null ? isJava9AtLeast( pathToJava.getPath() ) : isJava9AtLeast( version );
            return new JdkAttributes( pathToJava, jdkHome, javaVersion9 );
        }

        if ( toolchain != null )
        {
            String jvmToUse = toolchain.findTool( "java" );
            if ( isNotEmpty( jvmToUse ) )
            {
                boolean javaVersion9 = false;
                String jdkHome = null;

                if ( toolchain instanceof DefaultToolchain )
                {
                    DefaultToolchain defaultToolchain = (DefaultToolchain) toolchain;
                    javaVersion9 = defaultToolchain.matchesRequirements( JAVA_9_MATCHER )
                                             || defaultToolchain.matchesRequirements( JAVA_9_MATCHER_OLD_NOTATION );
                }

                if ( toolchain instanceof DefaultJavaToolChain )
                {
                    DefaultJavaToolChain defaultJavaToolChain = (DefaultJavaToolChain) toolchain;
                    if ( !getEnvironmentVariables().containsKey( "JAVA_HOME" ) )
                    {
                        jdkHome = defaultJavaToolChain.getJavaHome();
                        getEnvironmentVariables().put( "JAVA_HOME", jdkHome );
                    }
                }

                if ( !javaVersion9 )
                {
                    javaVersion9 = isJava9AtLeast( jvmToUse );
                }

                return new JdkAttributes( new File( jvmToUse ),
                    jdkHome == null ? toJdkHomeFromJvmExec( jvmToUse ) : new File( jdkHome ), javaVersion9 );
            }
        }

        // use the same JVM as the one used to run Maven (the "java.home" one)
        String jvmToUse = System.getProperty( "java.home" ) + File.separator + "bin" + File.separator + "java";
        getConsoleLogger().debug( "Using JVM: " + jvmToUse + " with Java version " + JAVA_RECENT );

        return new JdkAttributes( jvmToUse, isBuiltInJava9AtLeast() );
    }

    /**
     * Where surefire stores its own temp files
     *
     * @return A file pointing to the location of surefire's own temp files
     */
    File getSurefireTempDir()
    {
        File result = IS_OS_WINDOWS ? createSurefireBootDirectoryInTemp() : createSurefireBootDirectoryInBuild();
        try
        {
            File canonical = result.getCanonicalFile();
            if ( !result.equals( canonical ) )
            {
                getConsoleLogger()
                        .debug( "Canonicalized tempDir path '" + result + "' to '" + canonical + "'" );
            }
            return canonical;
        }
        catch ( IOException e )
        {
            getConsoleLogger()
                    .error( "Could not canonicalize tempDir path '" + result + "'", e );
        }
        return result;
    }

    /**
     * Operates on raw plugin parameters, not the "effective" values.
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
        checksum.add( getMainBuildPath() );
        checksum.add( getClasspathDependencyExcludes() );
        checksum.add( getClasspathDependencyScopeExclude() );
        checksum.add( getAdditionalClasspathElements() );
        checksum.add( getReportsDirectory() );
        checksum.add( getProjectBuildDirectory() );
        checksum.add( getTestSourceDirectory() );
        checksum.add( getTest() );
        checksum.add( getIncludes() );
        checksum.add( getSkipAfterFailureCount() );
        checksum.add( getShutdown() );
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
        checksum.add( getForkCount() );
        checksum.add( isReuseForks() );
        checksum.add( getJvm() );
        checksum.add( getArgLine() );
        checksum.add( getDebugForkedProcess() );
        checksum.add( getForkedProcessTimeoutInSeconds() );
        checksum.add( getParallelTestsTimeoutInSeconds() );
        checksum.add( getParallelTestsTimeoutForcedInSeconds() );
        checksum.add( getEnvironmentVariables() );
        checksum.add( getExcludedEnvironmentVariables() );
        checksum.add( getWorkingDirectory() );
        checksum.add( isChildDelegation() );
        checksum.add( getGroups() );
        checksum.add( getExcludedGroups() );
        checksum.add( getIncludeJUnit5Engines() );
        checksum.add( getExcludeJUnit5Engines() );
        checksum.add( getSuiteXmlFiles() );
        checksum.add( getJunitArtifact() );
        checksum.add( getTestNGArtifactName() );
        checksum.add( getThreadCount() );
        checksum.add( getThreadCountSuites() );
        checksum.add( getThreadCountClasses() );
        checksum.add( getThreadCountMethods() );
        checksum.add( getPerCoreThreadCount() );
        checksum.add( getUseUnlimitedThreads() );
        checksum.add( getParallel() );
        checksum.add( isParallelOptimized() );
        checksum.add( isTrimStackTrace() );
        checksum.add( getRemoteRepositories() );
        checksum.add( getProjectRemoteRepositories() );
        checksum.add( isDisableXmlReport() );
        checksum.add( isUseSystemClassLoader() );
        checksum.add( isUseManifestOnlyJar() );
        checksum.add( getEncoding() );
        checksum.add( isEnableAssertions() );
        checksum.add( getObjectFactory() );
        checksum.add( getFailIfNoTests() );
        checksum.add( getRunOrder() );
        checksum.add( getDependenciesToScan() );
        checksum.add( getForkedProcessExitTimeoutInSeconds() );
        checksum.add( getRerunFailingTestsCount() );
        checksum.add( getTempDir() );
        checksum.add( useModulePath() );
        checksum.add( getEnableProcessChecker() );
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
            getConsoleLogger()
                    .info( "Skipping execution of surefire because it has already been run for this configuration" );
            return true;
        }
        pluginContext.put( configChecksum, configChecksum );

        return false;
    }

    @Nonnull
    protected ClassLoaderConfiguration getClassLoaderConfiguration()
    {
        return isForking()
            ? new ClassLoaderConfiguration( isUseSystemClassLoader(), isUseManifestOnlyJar() )
            : new ClassLoaderConfiguration( false, false );
    }

    /**
     * Generates the test classpath.
     *
     * @return the classpath elements
     */
    private TestClassPath generateTestClasspath()
    {
        Set<Artifact> classpathArtifacts = getProject().getArtifacts();

        if ( getClasspathDependencyScopeExclude() != null && !getClasspathDependencyScopeExclude().isEmpty() )
        {
            ArtifactFilter dependencyFilter = new ScopeArtifactFilter( getClasspathDependencyScopeExclude() );
            classpathArtifacts = filterArtifacts( classpathArtifacts, dependencyFilter );
        }

        if ( getClasspathDependencyExcludes() != null )
        {
            List<String> excludedDependencies = asList( getClasspathDependencyExcludes() );
            ArtifactFilter dependencyFilter = new PatternIncludesArtifactFilter( excludedDependencies );
            classpathArtifacts = filterArtifacts( classpathArtifacts, dependencyFilter );
        }

        return new TestClassPath( classpathArtifacts, getMainBuildPath(),
                getTestClassesDirectory(), getAdditionalClasspathElements() );
    }

    /**
     * Return a new set containing only the artifacts accepted by the given filter.
     *
     * @param artifacts The unfiltered artifacts
     * @param filter    The filter to apply
     * @return The filtered result
     */
    private static Set<Artifact> filterArtifacts( Set<Artifact> artifacts, ArtifactFilter filter )
    {
        Set<Artifact> filteredArtifacts = new LinkedHashSet<>();

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
            getConsoleLogger().debug( "Setting " + setting + " [" + key + "]=[" + value + "]" );
        }
    }

    private <T> void showArray( T[] array, String setting )
    {
        for ( T e : array )
        {
            getConsoleLogger().debug( "Setting " + setting + " [" + e + "]" );
        }
    }

    private Classpath getArtifactClasspath( Artifact surefireArtifact ) throws MojoExecutionException
    {
        Classpath existing = classpathCache.getCachedClassPath( surefireArtifact.getArtifactId() );
        if ( existing == null )
        {
            List<String> items = new ArrayList<>();
            Set<Artifact> booterArtifacts =
                    surefireDependencyResolver.resolvePluginArtifact( surefireArtifact ).getArtifacts();
            for ( Artifact artifact : booterArtifacts )
            {
                getConsoleLogger().debug(
                    "Adding to " + getPluginName() + " booter test classpath: " + artifact.getFile().getAbsolutePath()
                    + " Scope: " + artifact.getScope() );
                items.add( artifact.getFile().getAbsolutePath() );
            }
            existing = new Classpath( items );
            classpathCache.setCachedClasspath( surefireArtifact.getArtifactId(), existing );
        }
        return existing;
    }

    private Properties getUserProperties()
    {
        return getSession().getUserProperties();
    }

    private void ensureWorkingDirectoryExists()
        throws MojoFailureException
    {
        if ( getWorkingDirectory() == null )
        {
            throw new MojoFailureException( "workingDirectory cannot be null" );
        }

        if ( isForking() )
        {
            // Postpone directory creation till forked JVM creation
            // see ForkConfiguration.createCommandLine
            return;
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

    private void ensureParallelRunningCompatibility()
        throws MojoFailureException
    {
        if ( isMavenParallel() && isNotForking() )
        {
            throw new MojoFailureException( "parallel maven execution is not compatible with surefire forkCount 0" );
        }
    }

    private void ensureThreadCountWithPerThread()
        throws MojoFailureException
    {
        if ( FORK_PERTHREAD.equals( getEffectiveForkMode() ) && getThreadCount() < 1 )
        {
            throw new MojoFailureException( "Fork mode perthread requires a thread count" );
        }
    }

    private void warnIfUselessUseSystemClassLoaderParameter()
    {
        if ( isUseSystemClassLoader() && isNotForking() )
        {
            getConsoleLogger().warning( "useSystemClassLoader setting has no effect when not forking" );
        }
    }

    private boolean isNotForking()
    {
        return !isForking();
    }

    private List<CommandLineOption> commandLineOptions()
    {
        return SurefireHelper.commandLineOptions( getSession(), getConsoleLogger() );
    }

    private void warnIfDefunctGroupsCombinations()
        throws MojoFailureException, MojoExecutionException
    {
        if ( isAnyGroupsSelected() )
        {
            if ( getTestNgArtifact() == null )
            {
                Artifact junitArtifact = getJunitArtifact();
                boolean junit47Compatible = isJunit47Compatible( junitArtifact );
                boolean junit5PlatformCompatible = getJUnit5Artifact() != null;
                if ( !junit47Compatible && !junit5PlatformCompatible )
                {
                    if ( junitArtifact != null )
                    {
                        throw new MojoFailureException( "groups/excludedGroups are specified but JUnit version on "
                                                            + "classpath is too old to support groups. "
                                                            + "Check your dependency:tree to see if your project "
                                                            + "is picking up an old junit version" );
                    }
                    throw new MojoFailureException( "groups/excludedGroups require TestNG, JUnit48+ or JUnit 5 "
                            + "(a specific engine required on classpath) on project test classpath" );
                }
            }

        }
    }

    private void warnIfRerunClashes()
        throws MojoFailureException
    {
        if ( getRerunFailingTestsCount() < 0 )
        {
            throw new MojoFailureException( "Parameter \"rerunFailingTestsCount\" should not be negative." );
        }

        if ( getSkipAfterFailureCount() < 0 )
        {
            throw new MojoFailureException( "Parameter \"skipAfterFailureCount\" should not be negative." );
        }
    }

    private void warnIfWrongShutdownValue()
        throws MojoFailureException
    {
        if ( !Shutdown.isKnown( getShutdown() ) )
        {
            throw new MojoFailureException( "Parameter \"shutdown\" should have values " + Shutdown.listParameters() );
        }
    }

    private void warnIfNotApplicableSkipAfterFailureCount()
        throws MojoFailureException
    {
        int skipAfterFailureCount = getSkipAfterFailureCount();

        if ( skipAfterFailureCount < 0 )
        {
            throw new MojoFailureException( "Parameter \"skipAfterFailureCount\" should not be negative." );
        }
        else if ( skipAfterFailureCount > 0 )
        {
            try
            {
                Artifact testng = getTestNgArtifact();
                if ( testng != null )
                {
                    VersionRange range = VersionRange.createFromVersionSpec( "[5.10,)" );
                    if ( !range.containsVersion( new DefaultArtifactVersion( testng.getVersion() ) ) )
                    {
                        throw new MojoFailureException(
                            "Parameter \"skipAfterFailureCount\" expects TestNG Version 5.10 or higher. "
                                + "java.lang.NoClassDefFoundError: org/testng/IInvokedMethodListener" );
                    }
                }
                else
                {
                    // TestNG is dependent on JUnit
                    Artifact junit = getJunitArtifact();
                    if ( junit != null )
                    {
                        VersionRange range = VersionRange.createFromVersionSpec( "[4.0,)" );
                        if ( !range.containsVersion( new DefaultArtifactVersion( junit.getVersion() ) ) )
                        {
                            throw new MojoFailureException(
                                "Parameter \"skipAfterFailureCount\" expects JUnit Version 4.0 or higher. "
                                    + "java.lang.NoSuchMethodError: "
                                    + "org.junit.runner.notification.RunNotifier.pleaseStop()V" );
                        }
                    }
                }
            }
            catch ( MojoExecutionException e )
            {
                throw new MojoFailureException( e.getLocalizedMessage() );
            }
            catch ( InvalidVersionSpecificationException e )
            {
                throw new RuntimeException( e );
            }
        }
    }

    private void warnIfIllegalTempDir() throws MojoFailureException
    {
        if ( isEmpty( getTempDir() ) )
        {
            throw new MojoFailureException( "Parameter 'tempDir' should not be blank string." );
        }
    }

    protected void warnIfIllegalFailOnFlakeCount() throws MojoFailureException
    {

    }

    private void printDefaultSeedIfNecessary()
    {
        if ( getRunOrderRandomSeed() == null && getRunOrder().equals( RunOrder.RANDOM.name() ) )
        {
            setRunOrderRandomSeed( System.nanoTime() );
            getConsoleLogger().info(
                "Tests will run in random order. To reproduce ordering use flag -D"
                    + getPluginName() + ".runOrder.random.seed=" + getRunOrderRandomSeed() );
        }
    }

    final class TestNgProviderInfo
        implements ProviderInfo
    {
        private final Artifact testNgArtifact;

        TestNgProviderInfo( Artifact testNgArtifact )
        {
            this.testNgArtifact = testNgArtifact;
        }

        @Override
        @Nonnull public String getProviderName()
        {
            return "org.apache.maven.surefire.testng.TestNGProvider";
        }

        @Override
        public boolean isApplicable()
        {
            return testNgArtifact != null;
        }

        @Override
        public void addProviderProperties() throws MojoExecutionException
        {
            convertTestNGParameters();
        }

        @Nonnull
        @Override
        public List<String[]> getJpmsArguments( @Nonnull ProviderRequirements forkRequirements )
        {
            return emptyList();
        }

        @Override
        @Nonnull
        public Set<Artifact> getProviderClasspath() throws MojoExecutionException
        {
            Artifact surefireArtifact = getBooterArtifact();
            String version = surefireArtifact.getBaseVersion();
            return surefireDependencyResolver.getProviderClasspath( "surefire-testng", version );
        }
    }

    final class JUnit3ProviderInfo
        implements ProviderInfo
    {
        @Override
        @Nonnull public String getProviderName()
        {
            return "org.apache.maven.surefire.junit.JUnit3Provider";
        }

        @Override
        public boolean isApplicable()
        {
            return true;
        }

        @Override
        public void addProviderProperties()
        {
        }

        @Nonnull
        @Override
        public List<String[]> getJpmsArguments( @Nonnull ProviderRequirements forkRequirements )
        {
            return emptyList();
        }

        @Override
        @Nonnull
        public Set<Artifact> getProviderClasspath() throws MojoExecutionException
        {
            // add the JUnit provider as default - it doesn't require JUnit to be present,
            // since it supports POJO tests.
            String version = getBooterArtifact().getBaseVersion();
            return surefireDependencyResolver.getProviderClasspath( "surefire-junit3", version );
        }
    }

    final class JUnit4ProviderInfo
        implements ProviderInfo
    {
        private final Artifact junitArtifact;

        private final Artifact junitDepArtifact;

        JUnit4ProviderInfo( Artifact junitArtifact, Artifact junitDepArtifact )
        {
            this.junitArtifact = junitArtifact;
            this.junitDepArtifact = junitDepArtifact;
        }

        @Override
        @Nonnull public String getProviderName()
        {
            return "org.apache.maven.surefire.junit4.JUnit4Provider";
        }

        @Override
        public boolean isApplicable()
        {
            return junitDepArtifact != null || isAnyJunit4( junitArtifact );
        }

        @Override
        public void addProviderProperties()
        {
        }

        @Nonnull
        @Override
        public List<String[]> getJpmsArguments( @Nonnull ProviderRequirements forkRequirements )
        {
            return emptyList();
        }

        @Override
        @Nonnull
        public Set<Artifact> getProviderClasspath() throws MojoExecutionException
        {
            String version = getBooterArtifact().getBaseVersion();
            return surefireDependencyResolver.getProviderClasspath( "surefire-junit4", version );
        }
    }

    final class JUnitPlatformProviderInfo
        implements ProviderInfo
    {
        private static final String PROVIDER_DEP_GID = "org.junit.platform";
        private static final String PROVIDER_DEP_AID = "junit-platform-launcher";

        private final Artifact junitPlatformRunnerArtifact;
        private final Artifact junitPlatformArtifact;
        private final TestClassPath testClasspath;

        JUnitPlatformProviderInfo( Artifact junitPlatformRunnerArtifact, Artifact junitPlatformArtifact,
                                   @Nonnull TestClassPath testClasspath )
        {
            this.junitPlatformRunnerArtifact = junitPlatformRunnerArtifact;
            this.junitPlatformArtifact = junitPlatformArtifact;
            this.testClasspath = testClasspath;
        }

        @Override
        @Nonnull
        public String getProviderName()
        {
            return "org.apache.maven.surefire.junitplatform.JUnitPlatformProvider";
        }

        @Override
        public boolean isApplicable()
        {
            return junitPlatformRunnerArtifact == null && junitPlatformArtifact != null;
        }

        @Override
        public void addProviderProperties()
        {
            convertGroupParameters();
            convertJunitEngineParameters();
        }

        @Nonnull
        @Override
        public List<String[]> getJpmsArguments( @Nonnull ProviderRequirements forkRequirements )
        {
            boolean hasTestDescriptor = forkRequirements.isModularPath() && forkRequirements.hasTestModuleDescriptor();
            return hasTestDescriptor ? getJpmsArgs() : Collections.<String[]>emptyList();
        }

        @Override
        @Nonnull
        public Set<Artifact> getProviderClasspath() throws MojoExecutionException
        {
            String surefireVersion = getBooterArtifact().getBaseVersion();
            Map<String, Artifact> providerArtifacts =
                    surefireDependencyResolver.getProviderClasspathAsMap( "surefire-junit-platform", surefireVersion );
            Map<String, Artifact> testDeps = testClasspath.getTestDependencies();

            Plugin plugin = getPluginDescriptor().getPlugin();
            Map<String, Artifact> pluginDeps =
                surefireDependencyResolver.resolvePluginDependencies( plugin, getPluginArtifactMap() );

            if ( hasDependencyPlatformEngine( pluginDeps ) )
            {
                providerArtifacts.putAll( pluginDeps );
            }
            else
            {
                String engineVersion = null;
                if ( hasDependencyJupiterAPI( testDeps )
                    && !testDeps.containsKey( "org.junit.jupiter:junit-jupiter-engine" ) )
                {
                    String engineGroupId = "org.junit.jupiter";
                    String engineArtifactId = "junit-jupiter-engine";
                    String engineCoordinates = engineGroupId + ":" + engineArtifactId;
                    String api = "org.junit.jupiter:junit-jupiter-api";
                    engineVersion = testDeps.get( api ).getBaseVersion();
                    getConsoleLogger().debug( "Test dependencies contain "
                        + api + ". Resolving " + engineCoordinates + ":" + engineVersion );
                    addEngineByApi( engineGroupId, engineArtifactId, engineVersion, providerArtifacts );
                }

                if ( ( testDeps.containsKey( "junit:junit" ) || testDeps.containsKey( "junit:junit-dep" ) )
                    && !testDeps.containsKey( "org.junit.vintage:junit-vintage-engine" ) )
                {
                    String engineGroupId = "org.junit.vintage";
                    String engineArtifactId = "junit-vintage-engine";
                    String engineCoordinates = engineGroupId + ":" + engineArtifactId;

                    if ( engineVersion != null )
                    {
                        getConsoleLogger().debug( "Test dependencies contain JUnit4. Resolving "
                            + engineCoordinates + ":" + engineVersion );
                        addEngineByApi( engineGroupId, engineArtifactId, engineVersion, providerArtifacts );
                    }
                }
            }

            narrowDependencies( providerArtifacts, testDeps );
            alignProviderVersions( providerArtifacts );

            return new LinkedHashSet<>( providerArtifacts.values() );
        }

        private List<String[]> getJpmsArgs()
        {
            List<String[]> args = new ArrayList<>();

            args.add( new String[] {
                "--add-opens",
                "org.junit.platform.commons/org.junit.platform.commons.util=ALL-UNNAMED"
            } );

            args.add( new String[] {
                "--add-opens",
                "org.junit.platform.commons/org.junit.platform.commons.logging=ALL-UNNAMED"
            } );

            return args;
        }

        private void addEngineByApi( String engineGroupId, String engineArtifactId, String engineVersion,
                                     Map<String, Artifact> providerArtifacts ) throws MojoExecutionException
        {
            for ( Artifact dep : resolve( engineGroupId, engineArtifactId, engineVersion, null, "jar" ) )
            {
                String key = dep.getGroupId() + ":" + dep.getArtifactId();
                providerArtifacts.put( key, dep );
            }
        }

        private void narrowDependencies( Map<String, Artifact> providerArtifacts,
                                         Map<String, Artifact> testDependencies )
        {
            providerArtifacts.keySet().removeAll( testDependencies.keySet() );
        }

        private void alignProviderVersions( Map<String, Artifact> providerArtifacts ) throws MojoExecutionException
        {
            String version = junitPlatformArtifact.getBaseVersion();
            for ( Artifact launcherArtifact : resolve( PROVIDER_DEP_GID, PROVIDER_DEP_AID, version, null, "jar" ) )
            {
                String key = launcherArtifact.getGroupId() + ":" + launcherArtifact.getArtifactId();
                if ( providerArtifacts.containsKey( key ) )
                {
                    providerArtifacts.put( key, launcherArtifact );
                }
            }
        }

        private Set<Artifact> resolve( String g, String a, String v, String c, String t ) throws MojoExecutionException
        {
            ArtifactHandler handler = junitPlatformArtifact.getArtifactHandler();
            Artifact artifact = new DefaultArtifact( g, a, v, null, t, c, handler );
            getConsoleLogger().debug( "Resolving artifact " + g + ":" + a + ":" + v );
            Set<Artifact> r = surefireDependencyResolver.resolveProjectArtifact( artifact ).getArtifacts();
            getConsoleLogger().debug( "Resolved artifact " + g + ":" + a + ":" + v + " to " + r );
            return r;
        }

        private boolean hasDependencyJupiterAPI( Map<String, Artifact> dependencies )
        {
            return dependencies.containsKey( "org.junit.jupiter:junit-jupiter-api" );
        }

        private boolean hasDependencyPlatformEngine( Map<String, Artifact> dependencies )
        {
            for ( Entry<String, Artifact> dependency : dependencies.entrySet() )
            {
                if ( dependency.getKey().equals( "org.junit.platform:junit-platform-engine" ) )
                {
                    return true;
                }
            }

            return false;
        }
    }

    final class JUnitCoreProviderInfo
        implements ProviderInfo
    {
        private final Artifact junitArtifact;

        private final Artifact junitDepArtifact;

        JUnitCoreProviderInfo( Artifact junitArtifact, Artifact junitDepArtifact )
        {
            this.junitArtifact = junitArtifact;
            this.junitDepArtifact = junitDepArtifact;
        }

        @Override
        @Nonnull public String getProviderName()
        {
            return "org.apache.maven.surefire.junitcore.JUnitCoreProvider";
        }

        private boolean is47CompatibleJunitDep()
        {
            return isJunit47Compatible( junitDepArtifact );
        }

        @Override
        public boolean isApplicable()
        {
            final boolean isJunitArtifact47 = isAnyJunit4( junitArtifact ) && isJunit47Compatible( junitArtifact );
            final boolean isAny47ProvidersForces = isAnyConcurrencySelected() || isAnyGroupsSelected();
            return isAny47ProvidersForces && ( isJunitArtifact47 || is47CompatibleJunitDep() );
        }

        @Override
        public void addProviderProperties() throws MojoExecutionException
        {
            convertJunitCoreParameters();
            convertGroupParameters();
            convertJunitEngineParameters();
        }

        @Nonnull
        @Override
        public List<String[]> getJpmsArguments( @Nonnull ProviderRequirements forkRequirements )
        {
            return emptyList();
        }

        @Override
        @Nonnull
        public Set<Artifact> getProviderClasspath() throws MojoExecutionException
        {
            String version = getBooterArtifact().getBaseVersion();
            return surefireDependencyResolver.getProviderClasspath( "surefire-junit47", version );
        }
    }

    /**
     * Provides the Provider information for manually configured providers.
     */
    final class DynamicProviderInfo
        implements ConfigurableProviderInfo
    {
        final String providerName;

        DynamicProviderInfo( String providerName )
        {
            this.providerName = providerName;
        }

        @Override
        public ProviderInfo instantiate( String providerName )
        {
            return new DynamicProviderInfo( providerName );
        }

        @Override
        @Nonnull
        public String getProviderName()
        {
            return providerName;
        }

        @Override
        public boolean isApplicable()
        {
            return true;
        }

        @Override
        public void addProviderProperties() throws MojoExecutionException
        {
            // Ok this is a bit lazy.
            convertJunitCoreParameters();
            convertTestNGParameters();
        }

        @Nonnull
        @Override
        public List<String[]> getJpmsArguments( @Nonnull ProviderRequirements forkRequirements )
        {
            return emptyList();
        }

        @Override
        @Nonnull
        public Set<Artifact> getProviderClasspath() throws MojoExecutionException
        {
            Plugin plugin = getPluginDescriptor().getPlugin();
            Map<String, Artifact> providerArtifacts =
                surefireDependencyResolver.resolvePluginDependencies( plugin, getPluginArtifactMap() );
            return new LinkedHashSet<>( providerArtifacts.values() );
        }
    }

    File createSurefireBootDirectoryInBuild()
    {
        File tmp = new File( getProjectBuildDirectory(), getTempDir() );
        //noinspection ResultOfMethodCallIgnored
        tmp.mkdirs();
        return tmp;
    }

    File createSurefireBootDirectoryInTemp()
    {
        try
        {
            return Files.createTempDirectory( getTempDir() ).toFile();
        }
        catch ( IOException e )
        {
            return createSurefireBootDirectoryInBuild();
        }
    }

    @Override
    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    @Override
    public void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    public Properties getSystemProperties()
    {
        return systemProperties;
    }

    @SuppressWarnings( "UnusedDeclaration" )
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

    /**
     * List of System properties, loaded from a file, to pass to the JUnit tests.
     *
     * @since 2.8.2
     */
    public abstract File getSystemPropertiesFile();

    @SuppressWarnings( "UnusedDeclaration" )
    public abstract void setSystemPropertiesFile( File systemPropertiesFile );

    private Properties getProperties()
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


    public boolean getFailIfNoTests()
    {
        return failIfNoTests;
    }

    public void setFailIfNoTests( boolean failIfNoTests )
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

    public boolean isParallelOptimized()
    {
        return parallelOptimized;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setParallelOptimized( boolean parallelOptimized )
    {
        this.parallelOptimized = parallelOptimized;
    }

    public int getThreadCountSuites()
    {
        return threadCountSuites;
    }

    public void setThreadCountSuites( int threadCountSuites )
    {
        this.threadCountSuites = threadCountSuites;
    }

    public int getThreadCountClasses()
    {
        return threadCountClasses;
    }

    public void setThreadCountClasses( int threadCountClasses )
    {
        this.threadCountClasses = threadCountClasses;
    }

    public int getThreadCountMethods()
    {
        return threadCountMethods;
    }

    public void setThreadCountMethods( int threadCountMethods )
    {
        this.threadCountMethods = threadCountMethods;
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

    public List<ArtifactRepository> getProjectRemoteRepositories()
    {
        return projectRemoteRepositories;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void setProjectRemoteRepositories( List<ArtifactRepository> projectRemoteRepositories )
    {
        this.projectRemoteRepositories = projectRemoteRepositories;
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

    public boolean effectiveIsEnableAssertions()
    {
        if ( getArgLine() != null )
        {
            List<String> args = asList( getArgLine().split( " " ) );
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

    public String[] getDependenciesToScan()
    {
        return dependenciesToScan;
    }

    public void setDependenciesToScan( String[] dependenciesToScan )
    {
        this.dependenciesToScan = dependenciesToScan;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    void setPluginDescriptor( PluginDescriptor pluginDescriptor )
    {
        this.pluginDescriptor = pluginDescriptor;
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

    @Override
    public File getTestSourceDirectory()
    {
        return testSourceDirectory;
    }

    @Override
    public void setTestSourceDirectory( File testSourceDirectory )
    {
        this.testSourceDirectory = testSourceDirectory;
    }

    public String getForkCount()
    {
        return forkCount;
    }

    public boolean isReuseForks()
    {
        return reuseForks;
    }

    public String[] getAdditionalClasspathElements()
    {
        return additionalClasspathElements;
    }

    public void setAdditionalClasspathElements( String[] additionalClasspathElements )
    {
        this.additionalClasspathElements = additionalClasspathElements;
    }

    public String[] getClasspathDependencyExcludes()
    {
        return classpathDependencyExcludes;
    }

    public void setClasspathDependencyExcludes( String[] classpathDependencyExcludes )
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

    public File getProjectBuildDirectory()
    {
        return projectBuildDirectory;
    }

    public void setProjectBuildDirectory( File projectBuildDirectory )
    {
        this.projectBuildDirectory = projectBuildDirectory;
    }

    protected void logDebugOrCliShowErrors( String s )
    {
        SurefireHelper.logDebugOrCliShowErrors( s, getConsoleLogger(), cli );
    }

    public Map<String, String> getJdkToolchain()
    {
        return jdkToolchain;
    }

    public void setJdkToolchain( Map<String, String> jdkToolchain )
    {
        this.jdkToolchain = jdkToolchain;
    }

    public String getTempDir()
    {
        return tempDir;
    }

    public void setTempDir( String tempDir )
    {
        this.tempDir = tempDir;
    }

    public void setResolutionErrorHandler( ResolutionErrorHandler resolutionErrorHandler )
    {
        this.resolutionErrorHandler = resolutionErrorHandler;
    }

    private static String getEffectiveForkMode( String forkMode )
    {
        if ( "pertest".equalsIgnoreCase( forkMode ) )
        {
            return FORK_ALWAYS;
        }
        else if ( "none".equalsIgnoreCase( forkMode ) )
        {
            return FORK_NEVER;
        }
        else if ( forkMode.equals( FORK_NEVER ) || forkMode.equals( FORK_ONCE )
                || forkMode.equals( FORK_ALWAYS ) || forkMode.equals( FORK_PERTHREAD ) )
        {
            return forkMode;
        }
        else
        {
            throw new IllegalArgumentException( "Fork mode " + forkMode + " is not a legal value" );
        }
    }

    private static final class ClasspathCache
    {
        private final Map<String, Classpath> classpaths = new HashMap<>( 4 );

        private Classpath getCachedClassPath( @Nonnull String artifactId )
        {
            return classpaths.get( artifactId );
        }

        private void setCachedClasspath( @Nonnull String key, @Nonnull Classpath classpath )
        {
            classpaths.put( key, classpath );
        }

        private Classpath setCachedClasspath( @Nonnull String key, @Nonnull Set<Artifact> artifacts )
        {
            Collection<String> files = new ArrayList<>();
            for ( Artifact artifact : artifacts )
            {
                files.add( artifact.getFile().getAbsolutePath() );
            }
            Classpath classpath = new Classpath( files );
            setCachedClasspath( key, classpath );
            return classpath;
        }
    }

    /**
     * Determines whether the plugin should fail if no tests found to run.
     */
    enum PluginFailureReason
    {
        NONE,
        COULD_NOT_RUN_SPECIFIED_TESTS,
        COULD_NOT_RUN_DEFAULT_TESTS,
    }
}
