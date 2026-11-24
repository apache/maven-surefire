Warning: truncated output (original token count: 43332)
Total output lines: 3997

/Users/efegokdemir/.zshenv:.:1: no such file or directory: /private/tmp/oss-apache-datafusion-comet-7748.9Fxtj6/repo/tooling/cargo/env
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugin.surefire;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.text.ChoiceFormat;
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
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
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
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.PatternIncludesArtifactFilter;
import org.apache.maven.surefire.api.booter.ProviderParameterNames;
import org.apache.maven.surefire.api.booter.Shutdown;
import org.apache.maven.surefire.api.cli.CommandLineOption;
import org.apache.maven.surefire.api.report.ReporterConfiguration;
import org.apache.maven.surefire.api.report.ReporterFactoryOptions;
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
import org.apache.maven.surefire.shared.utils.StringUtils;
import org.apache.maven.surefire.shared.utils.io.FileUtils;
import org.apache.maven.toolchain.DefaultToolchain;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.apache.maven.toolchain.java.DefaultJavaToolChain;
import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor;
import org.codehaus.plexus.languages.java.jpms.LocationManager;
import org.codehaus.plexus.languages.java.jpms.ResolvePathRequest;
import org.codehaus.plexus.languages.java.jpms.ResolvePathResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.JUNIT4_ONLY_DETECTED;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.JUNIT_VINTAGE_DETECTED;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.RUN_ORDER_PROP;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.RUN_ORDER_RANDOM_SEED_PROP;
import static org.apache.maven.surefire.api.suite.RunResult.failure;
import static org.apache.maven.surefire.api.suite.RunResult.noTestsRun;
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
 */
public abstract class AbstractSurefireMojo extends AbstractMojo implements SurefireExecutionParameters {
    private static final Map<String, String> JAVA_9_MATCHER_OLD_NOTATION = singletonMap("version", "[1.9,)");
    private static final Map<String, String> JAVA_9_MATCHER = singletonMap("version", "[9,)");
    private static final Platform PLATFORM = new Platform();

    private final ClasspathCache classpathCache = new ClasspathCache();

    /**
     * Configures the XML report, i.e. the {@code TEST-*.xml} files written to the reports directory.
     * The nested element {@code disable} turns the report off and {@code version} selects the XSD schema
     * version of the generated files.
     * <br>
     * Note: the deprecated parameter <em>disableXmlReport</em> takes precedence over {@code disable}
     * whenever it is explicitly set, no matter which value: {@code true} disables the report and
     * {@code false} keeps it enabled even if {@code disable} is {@code true}.
     * <br>
     * The XML attribute {@code implementation} may select another extension, e.g.
     * {@link org.apache.maven.plugin.surefire.extensions.junit5.JUnit5Xml30StatelessReporter
     * JUnit5Xml30StatelessReporter} which phrases names after the JUnit 5 annotation <em>DisplayName</em>.
     * For the complete list of nested elements, including those added by the JUnit 5
     * variants, see <a href="https://maven.apache.org/surefire/maven-surefire-plugin/examples/junit-platform.html#Surefire_Extensions_and_Reports_Configuration_for_.40DisplayName">Surefire Extensions and Reports Configuration for @DisplayName</a>.
     *
     * @see SurefireStatelessReporter
     * @since 3.0.0-M4
     */
    @Parameter
    private SurefireStatelessReporter statelessTestsetReporter;

    /**
     * Configures the reporter of the standard output and error streams captured from the tests. The streams
     * are written to the {@code *-output.txt} files in the reports directory if
     * {@code redirectTestOutputToFile} is enabled, and printed to the console otherwise. The nested element
     * {@code disable} drops the captured output altogether and {@code encoding} selects the charset of the
     * output files.
     * <br>
     * The XML attribute {@code implementation} may select another extension, e.g.
     * {@link org.apache.maven.plugin.surefire.extensions.junit5.JUnit5ConsoleOutputReporter
     * JUnit5ConsoleOutputReporter} which phrases the file names after the JUnit 5 annotation
     * <em>DisplayName</em>. For the complete list of nested elements, including those added by the JUnit 5
     * variants, see <a href="https://maven.apache.org/surefire/maven-surefire-plugin/examples/junit-platform.html#Surefire_Extensions_and_Reports_Configuration_for_.40DisplayName">Surefire Extensions and Reports Configuration for @DisplayName</a>.
     *
     * @see SurefireConsoleOutputReporter
     * @since 3.0.0-M4
     */
    @Parameter
    private SurefireConsoleOutputReporter consoleOutputReporter;

    /**
     * Configures the reporter of the per test-set summary, i.e. the <em>Running</em> and <em>Tests run</em>
     * lines printed to the console and written to the plain text {@code *.txt} files in the reports
     * directory. The nested element {@code disable} turns off both the console and the file summary.
     * <br>
     * The XML attribute {@code implementation} may select another extension, e.g.
     * {@link org.apache.maven.plugin.surefire.extensions.junit5.JUnit5StatelessTestsetInfoReporter
     * JUnit5StatelessTestsetInfoReporter} which phrases names after the JUnit 5 annotation
     * <em>DisplayName</em>. For the complete list of nested elements, including those added by the JUnit 5
     * variants, see <a href="https://maven.apache.org/surefire/maven-surefire-plugin/examples/junit-platform.html#Surefire_Extensions_and_Reports_Configuration_for_.40DisplayName">Surefire Extensions and Reports Configuration for @DisplayName</a>.
     *
     * @see SurefireStatelessTestsetInfoReporter
     * @since 3.0.0-M4
     */
    @Parameter
    private SurefireStatelessTestsetInfoReporter statelessTestsetInfoReporter;

    /**
     * Information about this plugin, mainly used to lookup this plugin's configuration from the currently executing
     * project.
     *
     * @since 2.12
     */
    @Parameter(defaultValue = "${plugin}", readonly = true, required = true)
    private PluginDescriptor pluginDescriptor;

    /**
     * Set this to "true" to skip running tests, but still compile them. Its use is NOT RECOMMENDED, but quite
     * convenient on occasion.<br>
     * <p>
     * As of Failsafe 3.6.0, the Failsafe plugin no longer binds its own {@code skipTests} parameter to the
     * {@code skipTests} user property, so {@code -DskipTests=true} now skips only Surefire (unit tests) while
     * leaving Failsafe (integration tests) untouched. Use {@code -DskipITs} to skip integration tests, or
     * {@code -Dmaven.test.skip} to skip both.
     *
     * @since 2.4
     */
    @Parameter(property = "skipTests", defaultValue = "false")
    protected boolean skipTests;

    /**
     * This old parameter is just like {@code skipTests}, but bound to the old property "maven.test.skip.exec".
     *
     * @since 2.3
     * @deprecated use skipTests instead
     */
    @Deprecated
    @Parameter(property = "maven.test.skip.exec")
    protected boolean skipExec;

    /**
     * Set this to "true" to bypass unit tests entirely. Its use is NOT RECOMMENDED, especially if you enable it using
     * the "maven.test.skip" property, because maven.test.skip disables both running the tests and compiling the tests.
     * Consider using the {@code skipTests} parameter instead.
     */
    @Parameter(property = "maven.test.skip", defaultValue = "false")
    protected boolean skip;

    /**
     * The Maven Project Object.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * The base directory of the project being tested. This can be obtained in your integration test via
     * System.getProperty("basedir").
     */
    @Parameter(defaultValue = "${basedir}", readonly = true, required = true)
    protected File basedir;

    /**
     * The directory containing generated test classes of the project being tested. This will be included at the
     * beginning of the test classpath.
     */
    @Parameter(defaultValue = "${project.build.testOutputDirectory}")
    protected File testClassesDirectory;

    /**
     * List of dependencies to exclude from the test classpath at runtime.
     * Each item is passed as pattern to {@link PatternIncludesArtifactFilter}.
     * The pattern is matched against the following artifact ids:
     * <ul>
     * <li>{@code groupId:artifactId} (Short ID)</li>
     * <li>{@code groupId:artifactId:type:classifier} (Dependency Conflict ID)</li>
     * <li>{@code groupId:artifactId:type:classifier:version} (Full ID)</li>
     * </ul>
     * The matching algorithm is described in detail in <a href="https://maven.apache.org/plugins/maven-assembly-plugin/advanced-descriptor-topics.html#advanced-artifact-matching-in-includes-and-excludes">Advanced Artifact-Matching</a> for the maven-assembly-plugin. This parameter behaves the same as the {@code excludes} pattern described there.
     * The dependency matching is applied to the project dependency IDs (including transitive ones) <i>after resolving</i>, i.e. excluding
     * one dependency will not exclude its transitive dependencies!
     *
     * @since 2.6
     */
    @Parameter(property = "maven.test.dependency.excludes")
    private String[] classpathDependencyExcludes;

    /**
     * A dependency scope to exclude from the test classpath at runtime. The scope should be one of the scopes defined by
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
    @Parameter(defaultValue = "")
    private String classpathDependencyScopeExclude;

    /**
     * Additional elements to be appended to the test classpath at runtime.
     * Each element must be a file system path to a JAR file or a directory containing classes.
     * No wildcards are allowed here.
     *
     * @since 2.4
     */
    @Parameter(property = "maven.test.additionalClasspath")
    private String[] additionalClasspathElements;

    /**
     * Additional Maven dependencies to be added to the test classpath at runtime.
     * Each element supports the parametrization like documented in <a href="https://maven.apache.org/pom.html#dependencies">POM Reference: Dependencies</a>.
     * <p>
     * Those dependencies are automatically collected (i.e. have their full dependency tree calculated) and then all underlying artifacts are resolved from the repository (including their transitive dependencies).
     * Afterwards the resolved artifacts are filtered to only contain {@code compile} and {@code runtime} scoped ones and appended to the test classpath at runtime
     * (after the ones from {@link #additionalClasspathElements}).
     * <p>
     * The following differences to regular project dependency resolving apply:
     * <ul>
     * <li>The dependency management from the project is not taken into account.</li>
     * <li>Conflicts between the different items and the project dependencies are not resolved.</li>
     * <li>Only external dependencies (outside the current Maven reactor) are supported.</li>
     * </ul>
     *
     * @since 3.2
     */
    @Parameter(property = "maven.test.additionalClasspathDependencies")
    private List<Dependency> additionalClasspathDependencies;

    /**
     * The test source directory containing test class sources.
     * Important <b>only</b> for TestNG HTML reports.
     *
     * @since 2.2
     */
    @Parameter(defaultValue = "${project.build.testSourceDirectory}")
    private File testSourceDirectory;

    /**
     * List of System properties to pass to a provider.
     *
     * @see #systemPropertyVariables {@code systemPropertyVariables} for how the effective provider properties are calculated
     * @deprecated use {@link #systemPropertyVariables} instead
     */
    @Deprecated
    @Parameter
    Properties systemProperties;

    /**
     * List of System properties to pass to a provider.
     * The effective system properties given to a provider are contributed from several sources:
     * <ol>
     * <li>properties set via {@link #argLine} with {@code -D} (only for forked executions)</li>
     * <li>{@link #systemProperties}</li>
     * <li>{@link AbstractSurefireMojo#getSystemPropertiesFile()} (set via parameter {@code systemPropertiesFile} on some goals)</li>
     * <li>{@link #systemPropertyVariables}</li>
     * <li>User properties from {@link MavenSession#getUserProperties()}, usually set via CLI options given with {@code -D} on the current Maven process (only used as source if {@link #promoteUserPropertiesToSystemProperties} is {@code true})</li>
     * </ol>
     * Later sources may overwrite same named properties from earlier sources, that means for example that one cannot overwrite user properties with either
     * {@link #systemProperties}, {@link #getSystemPropertiesFile()} or {@link #systemPropertyVariables}.
     * <p>
     * Certain properties may only be overwritten via {@link #argLine} (applicable only for forked executions) because their values are cached and only evaluated at the start of the JRE.
     * Those include:
     * <ul>
     * <li>{@code java.library.path}</li>
     * <li>{@code file.encoding}</li>
     * <li>{@code jdk.map.althashing.threshold}</li>
     * <li>{@code line.separator}</li>
     * </ul>
     *
     * @see #systemProperties
     * @since 2.5
     */
    @Parameter
    Map<String, String> systemPropertyVariables;

    /**
     * If set to {@code true} will also pass all user properties exposed via {@link MavenSession#getUserProperties()} as system properties to a provider.
     * Those always take precedence over same named system properties set via any other means.
     *
     * @see #systemPropertyVariables
     * @since 3.4.0
     */
    @Parameter(defaultValue = "true")
    boolean promoteUserPropertiesToSystemProperties;

    /**
     * List of properties for configuring the testing provider. This is the preferred method of
     * configuring TestNG and JUnit platform providers.<br><br>
     * JUnit platform properties may be defined in a {@code configurationParameters} element:
     * <pre><code>{@literal <properties>}
     *     {@literal <configurationParameters>}
     *         junit.jupiter.execution.parallel.enabled = true
     *         junit.jupiter.execution.parallel.mode.default = concurrent
     *     {@literal </configurationParameters>}
     * {@literal </properties>}</code></pre>
     * <br>
     * TestNG properties may be defined as distinct element blocks:
     * <pre><code>{@literal <properties>}
     *     {@literal <property>}
     *         {@literal <name>parallel</name>}
     *         {@literal <value>methods</value>}
     *     {@literal </property>}
     * {@literal </properties>}</code></pre>
     *
     * @since 2.4
     */
    @Parameter
    private Properties properties;

    /**
     * Package prefixes to filter from stack traces when capturing test output.
     * When specified, this REPLACES the default prefixes (java., javax., sun., jdk.).
     * <br>
     * <ul>
     *   <li>Not specified: uses defaults (java., javax., sun., jdk.)</li>
     *   <li>Custom list: only those prefixes are filtered (replaces defaults)</li>
     * </ul>
     * <br>
     * Example - filter only specific packages:
     * <pre><code>{@literal <stackTraceFilterPrefixes>}
     *     {@literal <prefix>org.springframework.</prefix>}
     *     {@literal <prefix>org.junit.</prefix>}
     * {@literal </stackTraceFilterPrefixes>}</code></pre>
     *
     * @since 3.6.0
     */
    @Parameter(property = "surefire.stackTraceFilterPrefixes")
    private List<String> stackTraceFilterPrefixes;

    /**
     * Maximum number of stack trace frames to capture when associating console output with test classes.
     * Default is 15. For performance reason this could be set to 0 or a negative value to disable stack trace capture entirely.
     * In the case of low value, Surefire will capture only the top frames of the stack trace, which might miss the test class
     * and so confuse stack trace in case of parallel test execution.
     * In the case of high value, Surefire will capture more frames, which might have a negative impact on performance and memory usage.
     * @since 3.6.0
     */
    @Parameter(property = "surefire.stackTraceMaxFrames", defaultValue = "15")
    private int stackTraceMaxFrames;

    /**
     * Map of plugin artifacts.
     */
    @Parameter(property = "plugin.artifactMap", required = true, readonly = true)
    private Map<String, Artifact> pluginArtifactMap;

    /**
     * Map of project artifacts.
     */
    @Parameter(property = "project.artifactMap", readonly = true, required = true)
    private Map<String, Artifact> projectArtifactMap;

    /**
     * Add custom text into report filename: TEST-testClassName-reportNameSuffix.xml,
     * testClassName-reportNameSuffix.txt and testClassName-reportNameSuffix-output.txt.
     * File TEST-testClassName-reportNameSuffix.xml has changed attributes 'testsuite'--'name'
     * and 'testcase'--'classname' - reportNameSuffix is added to the attribute value.
     */
    @Parameter(property = "surefire.reportNameSuffix", defaultValue = "")
    private String reportNameSuffix;

    /**
     * Set this to "true" to redirect the unit test standard output to a file (found in
     * reportsDirectory/testName-output.txt).
     *
     * @since 2.3
     */
    @Parameter(property = "maven.test.redirectTestOutputToFile", defaultValue = "false")
    private boolean redirectTestOutputToFile;

    /**
     * Set this to "true" to cause a failure if there are no tests to run. Defaults to "false".
     *
     * @since 2.4
     */
    @Parameter(property = "failIfNoTests", defaultValue = "false")
    private boolean failIfNoTests;

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
    @Parameter(property = "tempDir", defaultValue = "surefire")
    private String tempDir;

    /**
     * Option to specify the jvm (or path to the java executable) to use with the forking options. For the default, the
     * jvm will be a new instance of the same VM as the one used to run Maven. JVM settings are not inherited from
     * MAVEN_OPTS.
     *
     * @since 2.1
     */
    @Parameter(property = "jvm")
    private String jvm;

    /**
     * Arbitrary JVM options to set on the command line. Only effective for forked executions.
     * <br>
     * <br>
     * Using an alternate syntax for placeholders in {@code argLine}, namely <code>@{...}</code> allows late replacement
     * of properties when the plugin is executed, so properties that have been <i>modified</i> by other plugins will be picked
     * up correctly.
     * In contrast to the standard Maven property replacement the <code>@{...}</code> placeholder is replaced by the empty string if the referenced
     * propery cannot be found.
     * <br>
     * <i>This mechanism is only necessary for properties which are modified i.e. also available with a different value at initialization of the
     * POM model. Otherwise standard Maven property replacement kicks in properly directly before this goal is being executed (instead of when the POM model is being resolved)</i>.
     * <br>
     * See the Frequently Asked Questions page with more details:<br>
     * <a href="http://maven.apache.org/surefire/maven-surefire-plugin/faq.html">
     *     http://maven.apache.org/surefire/maven-surefire-plugin/faq.html</a>
     * <br>
     * <a href="http://maven.apache.org/surefire/maven-failsafe-plugin/faq.html">
     *     http://maven.apache.org/surefire/maven-failsafe-plugin/faq.html</a>
     *
     * @see #forkCount
     * @since 2.1
     */
    @Parameter(property = "argLine")
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
    @Parameter(property = "basedir")
    private File workingDirectory;

    /**
     * When false it makes tests run using the standard classloader delegation instead of the default Maven isolated
     * classloader. Only used when forking ({@code forkCount} is greater than zero).<br>
     * Setting it to false helps with some problems caused by conflicts between xml parsers in the classpath and the
     * Java 5 provider parser.
     *
     * @since 2.1
     */
    @Parameter(property = "childDelegation", defaultValue = "false")
    private boolean childDelegation;

    /**
     * (TestNG/JUnit47 provider with JUnit4.8+ only and JUnit5+ provider since 2.22.0) Groups/categories/tags for this
     * test. Only classes/methods/etc decorated with one of the groups/categories/tags specified here will be included
     * in test run, if specified.<br>
     * For JUnit4 tests, this parameter forces the use of the 4.7 provider. For JUnit5 tests, this parameter forces
     * the use of the JUnit platform provider.<br>
     * Since version 2.18.1 and JUnit 4.12, the {@code @Category} annotation type is automatically inherited from
     * superclasses, see {@code @java.lang.annotation.Inherited}. Make sure that test class inheritance still makes
     * sense together with {@code @Category} annotation of the JUnit 4.12 or higher appeared in superclass.
     *
     * @since 2.2
     */
    @Parameter(property = "groups")
    private String groups;

    /**
     * (TestNG/JUnit47 provider with JUnit4.8+ only and JUnit5+ provider since 2.22.0) Excluded groups/categories/tags.
     * Any methods/classes/etc with one of the groups/categories/tags specified in this list will specifically not be
     * run.<br>
     * For JUnit4, this parameter forces the use of the 4.7 provider. For JUnit5, this parameter forces the use of the
     * JUnit platform provider.<br>
     * Since version 2.18.1 and JUnit 4.12, the {@code @Category} annotation type is automatically inherited from
     * superclasses, see {@code @java.lang.annotation.Inherited}. Make sure that test class inheritance still makes
     * sense together with {@code @Category} annotation of the JUnit 4.12 or higher appeared in superclass.
     *
     * @since 2.2
     */
    @Parameter(property = "excludedGroups")
    private String excludedGroups;

    /**
     * Allows you to specify the name of the JUnit artifact. If not set, {@code junit:junit} will be used.
     *
     * @since 2.3.1
     */
    @Parameter(property = "junitArtifactName", defaultValue = "junit:junit")
    private String junitArtifactName;

    /**
     * Allows you to specify the name of the TestNG artifact. If not set, {@code org.testng:testng} will be used.
     *
     * @since 2.3.1
     */
    @Parameter(property = "testNGArtifactName", defaultValue = "org.testng:testng")
    private String testNGArtifactName;

    /**
     * (TestNG/JUnit 4.7 provider) The attribute thread-count allows you to specify how many threads should be
     * allocated for this execution. Only makes sense to use in conjunction with the {@code parallel} parameter.
     *
     * @since 2.2
     */
    @Parameter(property = "threadCount")
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
    @Parameter(property = "forkCount", defaultValue = "1")
    String forkCount;

    /**
     * Indicates if forked VMs can be reused. If set to "false", a new VM is forked for each test class to be executed.
     * If set to "true", up to {@code forkCount} VMs will be forked and then reused to execute all tests.
     *
     * @since 2.13
     */
    @Parameter(property = "reuseForks", defaultValue = "true")
    private boolean reuseForks;

    /**
     * (JUnit 4.7 provider) Indicates that threadCount, threadCountSuites, threadCountClasses, threadCountMethods
     * are per cpu core.
     *
     * @since 2.5
     * @deprecated not used on the JUnit Platform since 3.6.0, {@code threadCount} is no longer multiplied by the
     * number of CPU cores
     */
    @Deprecated
    @Parameter(property = "perCoreThreadCount", defaultValue = "true")
    private boolean perCoreThreadCount;

    /**
     * (JUnit 4.7 provider) Indicates that the thread pool will be unlimited. The {@code parallel} parameter and
     * the actual number of classes/methods will decide. Setting this to "true" effectively disables
     * {@code perCoreThreadCount} and {@code threadCount}. Defaults to "false".
     *
     * @since 2.5
     * @deprecated not used on the JUnit Platform since 3.6.0, use {@code threadCount}
     */
    @Deprecated
    @Parameter(property = "useUnlimitedThreads", defaultValue = "false")
    private boolean useUnlimitedThreads;

    /**
     * (TestNG provider) When you use the parameter {@code parallel}, TestNG will try to run all your test methods
     * in separate threads, except for methods that depend on each other, which will be run in the same thread in order
     * to respect their order of execution. Supports {@code methods}, {@code classes}, {@code tests} and
     * {@code instances}.
     * <br>
     * (JUnit 4.7 provider) Supports values {@code classes}, {@code methods}, {@code both} to run
     * in separate threads been controlled by {@code threadCount}.
     * <br>
     * <br>
     * Since version 2.16 (JUnit 4.7 provider), the value {@code both} is <strong>DEPRECATED</strong>.
     * Use {@code classesAndMethods} instead.
     * <br>
     * <br>
     * By default, Surefire does not execute tests in parallel. You can set the parameter {@code parallel} to
     * {@code none} to explicitly disable parallel execution (e.g. when disabling parallel execution in special Maven
     * profiles when executing coverage analysis).
     *
     * @since 2.2
     */
    @Parameter(property = "parallel")
    private String parallel;

    /**
     * (JUnit 4.7 / provider only) The thread counts do not exceed the number of parallel suite, class runners and
     * average number of methods per class if set to <strong>true</strong>.
     * <br>
     * True by default.
     *
     * @since 2.17
     * @deprecated not used on the JUnit Platform since 3.6.0
     */
    @Deprecated
    @Parameter(property = "parallelOptimized", defaultValue = "true")
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
     * @deprecated not used on the JUnit Platform since 3.6.0, it has no suite level parallelism
     */
    @Deprecated
    @Parameter(property = "threadCountSuites", defaultValue = "0")
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
    @Parameter(property = "threadCountClasses", defaultValue = "0")
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
    @Parameter(property = "threadCountMethods", defaultValue = "0")
    private int threadCountMethods;

    /**
     * Whether to trim the stack trace in the reports to just the lines within the test, or show the full trace.
     *
     * @since 2.2
     */
    @Parameter(property = "trimStackT…23332 tokens truncated…nRepositories(), surefireArtifact);
            for (Artifact artifact : booterArtifacts) {
                items.add(artifact.getFile().getAbsolutePath());
            }
            existing = new Classpath(items);
            classpathCache.setCachedClasspath(surefireArtifact.getArtifactId(), existing);
        }
        return existing;
    }

    Properties getUserProperties() {
        return getSession().getUserProperties();
    }

    private void ensureWorkingDirectoryExists() throws MojoFailureException {
        if (getWorkingDirectory() == null) {
            throw new MojoFailureException("workingDirectory cannot be null");
        }

        if (isForking()) {
            // Postpone directory creation till forked JVM creation
            // see ForkConfiguration.createCommandLine
            return;
        }

        if (!getWorkingDirectory().exists()) {
            if (!getWorkingDirectory().mkdirs()) {
                throw new MojoFailureException("Cannot create workingDirectory " + getWorkingDirectory());
            }
        }

        if (!getWorkingDirectory().isDirectory()) {
            throw new MojoFailureException(
                    "workingDirectory " + getWorkingDirectory() + " exists and is not a directory");
        }
    }

    private void ensureParallelRunningCompatibility() throws MojoFailureException {
        if (isMavenParallel() && isNotForking()) {
            throw new MojoFailureException("parallel maven execution is not compatible with surefire forkCount 0");
        }
    }

    private void warnIfUselessUseSystemClassLoaderParameter() {
        if (isUseSystemClassLoader() && isNotForking()) {
            getConsoleLogger().warning("useSystemClassLoader setting has no effect when not forking");
        }
    }

    private boolean isNotForking() {
        return !isForking();
    }

    private List<CommandLineOption> commandLineOptions() {
        return SurefireHelper.commandLineOptions(getSession(), getConsoleLogger());
    }

    private void warnIfDefunctGroupsCombinations() throws MojoFailureException, MojoExecutionException {
        Artifact junitArtifact = getJunitArtifact();
        if (isAnyGroupsSelected()) {
            if (getTestNgArtifact() == null) {
                boolean junit47Compatible = isJunit47Compatible(junitArtifact);
                boolean junit5PlatformCompatible = getJUnit5Artifact() != null;
                if (!junit47Compatible && !junit5PlatformCompatible) {
                    throw new MojoFailureException("groups/excludedGroups are specified but JUnit version on "
                            + "classpath is too old to support groups. "
                            + "Check your dependency:tree to see if your project "
                            + "is picking up an old junit version");
                }
            }
        }
    }

    private void warnIfRerunClashes() throws MojoFailureException {
        if (getRerunFailingTestsCount() < 0) {
            throw new MojoFailureException("Parameter \"rerunFailingTestsCount\" should not be negative.");
        }

        if (getSkipAfterFailureCount() < 0) {
            throw new MojoFailureException("Parameter \"skipAfterFailureCount\" should not be negative.");
        }
    }

    private void warnIfWrongShutdownValue() throws MojoFailureException {
        if (!Shutdown.isKnown(getShutdown())) {
            throw new MojoFailureException("Parameter \"shutdown\" should have values " + Shutdown.listParameters());
        }
    }

    private void warnIfIllegalTempDir() throws MojoFailureException {
        if (isEmpty(getTempDir())) {
            throw new MojoFailureException("Parameter 'tempDir' should not be blank string.");
        }
    }

    protected void warnIfIllegalFailOnFlakeCount() throws MojoFailureException {}

    /**
     * TestNG suite XML files from the configuration.
     * We only read them to fail the build, they're not supported anymore.
     */
    protected File[] getSuiteXmlFiles() {
        return null;
    }

    void ensureNoSuiteXmlFiles() throws MojoFailureException {
        File[] suiteXmlFiles = getSuiteXmlFiles();
        if (suiteXmlFiles != null && suiteXmlFiles.length != 0) {
            throw new MojoFailureException("Parameter \"suiteXmlFiles\" is not supported since 3.6.0: "
                    + "TestNG tests now run on the JUnit Platform, which can't read TestNG suite XML files. "
                    + "Remove it and select tests with \"groups\", \"excludedGroups\", \"includes\" or \"excludes\" "
                    + "(or a JUnit Platform @Suite class), or keep using maven-" + getPluginName()
                    + "-plugin 3.5.x.");
        }
    }

    /**
     * Warns when a parameter the JUnit Platform doesn't use is set.
     * Legacy providers still use them, so this only runs on the JUnit Platform path.
     */
    void warnIfIgnoredByJUnitPlatform() {
        List<String> ignored = new ArrayList<>();
        if (getUseUnlimitedThreads()) {
            ignored.add("useUnlimitedThreads");
        }
        if (getThreadCountSuites() > 0) {
            ignored.add("threadCountSuites");
        }
        if (!isParallelOptimized()) {
            ignored.add("parallelOptimized");
        }
        if (getParallelTestsTimeoutInSeconds() > 0) {
            ignored.add("parallelTestsTimeoutInSeconds");
        }
        if (getParallelTestsTimeoutForcedInSeconds() > 0) {
            ignored.add("parallelTestsTimeoutForcedInSeconds");
        }
        if (getObjectFactory() != null) {
            ignored.add("objectFactory");
        }
        if (!ignored.isEmpty()) {
            getConsoleLogger()
                    .warning("These parameters are ignored when tests run on the JUnit Platform and are deprecated: "
                            + String.join(", ", ignored) + ".");
        }
    }

    private void printDefaultSeedIfNecessary() {
        if (getRunOrder().equals(RunOrder.RANDOM.name())) {
            if (getRunOrderRandomSeed() == null) {
                setRunOrderRandomSeed(System.nanoTime());
            }
            getConsoleLogger()
                    .info("Tests will run in random order. To reproduce ordering use flag -D" + getPluginName()
                            + ".runOrder.random.seed=" + getRunOrderRandomSeed());
        }
    }

    /**
     * Provides the Provider information for manually configured providers.
     */
    final class DynamicProviderInfo implements ConfigurableProviderInfo {
        final String providerName;

        DynamicProviderInfo(String providerName) {
            this.providerName = providerName;
        }

        @Override
        public ProviderInfo instantiate(String providerName) {
            return new DynamicProviderInfo(providerName);
        }

        @Override
        @Nonnull
        public String getProviderName() {
            return providerName;
        }

        @Override
        public boolean isApplicable() {
            return true;
        }

        @Override
        public void addProviderProperties() throws MojoExecutionException {
            // Ok this is a bit lazy.
            try {
                convertJunitCoreParameters();
                convertTestNGParameters();
            } catch (MojoFailureException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }

        @Nonnull
        @Override
        public List<String[]> getJpmsArguments(@Nonnull ProviderRequirements forkRequirements) {
            return emptyList();
        }

        @Override
        @Nonnull
        public Set<Artifact> getProviderClasspath() throws MojoExecutionException {
            Plugin plugin = getPluginDescriptor().getPlugin();
            Map<String, Artifact> providerArtifacts = surefireDependencyResolver.resolvePluginDependencies(
                    session.getRepositorySession(),
                    project.getRemotePluginRepositories(),
                    plugin,
                    getPluginArtifactMap());
            return new LinkedHashSet<>(providerArtifacts.values());
        }
    }

    class JUnitPlatformProviderInfo implements ProviderInfo {

        private static final String PROVIDER_DEP_GID = "org.junit.platform";
        private static final String PROVIDER_DEP_AID = "junit-platform-launcher";

        private final Artifact junitPlatformRunnerArtifact;
        private final Artifact junitPlatformArtifact;
        private final TestClassPath testClasspath;

        private final Artifact junitArtifact;
        private final Artifact booterArtifact;
        private final Artifact testNgArtifact;
        private final SurefireDependencyResolver surefireDependencyResolver;
        private final MavenSession session;
        private final MavenProject project;
        private final PluginDescriptor pluginDescriptor;
        private final Map<String, Artifact> pluginArtifactMap;
        private final ConsoleLogger consoleLogger;
        private ProviderInfo.Engine engine = ProviderInfo.Engine.JUNIT_PLATFORM;

        @SuppressWarnings("checkstyle:parameternumber")
        JUnitPlatformProviderInfo(
                Artifact junitPlatformRunnerArtifact,
                Artifact junitPlatformArtifact,
                @Nonnull TestClassPath testClasspath,
                Artifact junitArtifact,
                Artifact booterArtifact,
                SurefireDependencyResolver surefireDependencyResolver,
                MavenSession session,
                MavenProject project,
                PluginDescriptor pluginDescriptor,
                Map<String, Artifact> pluginArtifactMap,
                ConsoleLogger consoleLogger,
                Artifact testNgArtifact) {
            this.session = session;
            this.project = project;
            this.pluginDescriptor = pluginDescriptor;
            this.pluginArtifactMap = pluginArtifactMap;
            this.consoleLogger = consoleLogger;
            this.junitPlatformRunnerArtifact = junitPlatformRunnerArtifact;
            this.junitPlatformArtifact = junitPlatformArtifact;
            this.testClasspath = testClasspath;
            this.junitArtifact = junitArtifact;
            this.booterArtifact = booterArtifact;
            this.testNgArtifact = testNgArtifact;
            // JUnit 4.12+ is required by JUnit 5.3+
            // JUnit 4.13+ is required by JUnit 5.8+
            // JUnit 4.13.2 is required by JUnit 5.9+
            // JUnit 4.13.3 is required by JUnit 5.10+
            // JUnit 4.13.4 is required by JUnit 5.12+
            // https://junit.org/junit5/docs/current/user-guide/#dependency-metadata

            if (isAnyJunit4(junitArtifact)) {
                this.engine = Engine.JUNIT4;
            }
            this.surefireDependencyResolver = surefireDependencyResolver;
        }

        @Override
        @Nonnull
        public String getProviderName() {
            return "org.apache.maven.surefire.junitplatform.JUnitPlatformProvider";
        }

        protected String getProviderArtifactName() {
            return "surefire-junit-platform";
        }

        @Override
        public boolean isApplicable() {
            return (junitPlatformRunnerArtifact == null && junitPlatformArtifact != null)
                    || (isAnyJunit4(junitArtifact))
                    || testNgArtifact != null;
        }

        @Override
        public void addProviderProperties() throws MojoExecutionException {
            try {
                convertJunitEngineParameters();
                convertGroupParameters();
                convertJunitCoreParameters();
                warnIfIgnoredByJUnitPlatform();
            } catch (MojoFailureException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
            // FIXME look at duplicate parameters to remove
            if (testNgArtifact != null) {
                convertTestNGParameters();
            }
        }

        @Nonnull
        @Override
        public List<String[]> getJpmsArguments(@Nonnull ProviderRequirements forkRequirements) {
            boolean commonsDependency = hasCommonsOnTestClasspath();
            return commonsDependency || hasCommonsOnAdditionalClasspath(forkRequirements.getJavaHome())
                    ? ModulePathJpmsArguments.junitPlatformArguments(forkRequirements)
                    : emptyList();
        }

        private boolean hasCommonsOnTestClasspath() {
            for (Artifact artifact : testClasspath.getArtifacts()) {
                if ("org.junit.platform".equals(artifact.getGroupId())
                        && "junit-platform-commons".equals(artifact.getArtifactId())
                        && artifact.getArtifactHandler().isAddedToClasspath()
                        && artifact.getFile() != null) {
                    return true;
                }
            }
            return false;
        }

        private boolean hasCommonsOnAdditionalClasspath(String javaHome) {
            Iterable<String> additionalElements = testClasspath.getAdditionalClasspathElements();
            if (additionalElements == null) {
                return false;
            }
            for (String additionalElement : additionalElements) {
                if (additionalElement == null) {
                    continue;
                }
                for (String path : StringUtils.split(additionalElement, ",")) {
                    if (path == null || path.trim().isEmpty()) {
                        continue;
                    }
                    try {
                        ResolvePathRequest<String> request = ResolvePathRequest.ofString(path.trim());
                        if (javaHome != null) {
                            request.setJdkHome(javaHome);
                        }
                        ResolvePathResult result = getLocationManager().resolvePath(request);
                        JavaModuleDescriptor descriptor = result == null ? null : result.getModuleDescriptor();
                        if (descriptor != null && "org.junit.platform.commons".equals(descriptor.name())) {
                            return true;
                        }
                    } catch (IOException e) {
                        logger.debug("Could not inspect additional classpath element '{}'.", path, e);
                    }
                }
            }
            return false;
        }

        @Override
        @Nonnull
        public Set<Artifact> getProviderClasspath() throws MojoExecutionException {
            String surefireVersion = booterArtifact.getBaseVersion();
            Map<String, Artifact> providerArtifacts = surefireDependencyResolver.getProviderClasspathAsMap(
                    session.getRepositorySession(),
                    project.getRemotePluginRepositories(),
                    getProviderArtifactName(),
                    surefireVersion);
            Map<String, Artifact> testDeps = testClasspath.getTestDependencies();

            Plugin plugin = pluginDescriptor.getPlugin();
            Map<String, Artifact> pluginDeps = surefireDependencyResolver.resolvePluginDependencies(
                    session.getRepositorySession(), project.getRemotePluginRepositories(), plugin, pluginArtifactMap);

            String defaultJunitVersion = getDefaultJunitVersion();

            String engineGroupId = "org.junit.jupiter";
            String engineArtifactId = "junit-jupiter-engine";
            String engineCoordinates = engineGroupId + ":" + engineArtifactId;
            String api = "org.junit.jupiter:junit-jupiter-api";
            // FIXME this default version should be aligned with the junit-platform-version property from pom
            String engineVersion = testDeps.get(api) == null
                    ? defaultJunitVersion
                    : testDeps.get(api).getBaseVersion();
            consoleLogger.debug(
                    "Test dependencies contain " + api + ". Resolving " + engineCoordinates + ":" + engineVersion);

            if (hasDependencyPlatformEngine(pluginDeps)) {
                providerArtifacts.putAll(pluginDeps);
            } else {
                if (hasDependencyJupiterAPI(testDeps)
                        && engineVersion != null
                        && !testDeps.containsKey(engineGroupId + ":" + engineArtifactId)) {
                    addEngineByApi(engineGroupId, engineArtifactId, engineVersion, providerArtifacts);
                }

                if (isWithinVersionSpec(junitArtifact, "[4.0,4.11]")) {
                    String message = "Surefire is not compatible with JUnit 4.11 or older. Project version is "
                            + junitArtifact.getBaseVersion();
                    consoleLogger.error(message);
                    throw new MojoExecutionException(message);
                }
            }

            // need to check there is at least one test engine in the classpath
            String junit = (String) getProperties().get("junit");
            boolean runJunit = Boolean.parseBoolean(StringUtils.isEmpty(junit) ? "true" : junit);
            if (StringUtils.isNotEmpty(junit) && !runJunit) {
                consoleLogger.debug("System property 'junit' is explicity set to '" + junit + "'");
            } else {
                if (testDeps.containsKey("junit:junit")
                        && !testDeps.containsKey("org.junit.vintage:junit-vintage-engine")
                        && !pluginDeps.containsKey("org.junit.vintage:junit-vintage-engine")) {
                    getProperties().setProperty(JUNIT_VINTAGE_DETECTED, "true");
                    if (testNgArtifact == null
                            && !hasDependencyJupiterAPI(testDeps)
                            && !hasDependencyPlatformEngine(testDeps)
                            && !hasDependencyPlatformEngine(pluginDeps)) {
                        getProperties().setProperty(JUNIT4_ONLY_DETECTED, "true");
                    }
                    if (!hasDependencyPlatformEngine(providerArtifacts)) {
                        // TODO exclude transitive deps (hamcrest etc...)
                        consoleLogger.debug("Test dependencies contain JUnit4. Resolving " + engineCoordinates + ":"
                                + engineVersion);
                        addEngineByApi(engineGroupId, engineArtifactId, engineVersion, providerArtifacts);
                    }
                    // add org.junit.vintage:junit-vintage-engine
                    addEngineByApi("org.junit.vintage", "junit-vintage-engine", engineVersion, providerArtifacts);
                }
            }
            if (testNgArtifact != null) {
                // minimum testng version tested via getTestNgArtifact
                String junitSupportGroupId = "org.junit.support";
                String testNgEngineArtifactId = "testng-engine";
                String testNgEngineCoordinates = junitSupportGroupId + ":" + testNgEngineArtifactId;
                String version = "1.1.0";
                if (!testDeps.containsKey(testNgEngineCoordinates)
                        && !pluginDeps.containsKey(testNgEngineCoordinates)) {
                    addEngineByApi(junitSupportGroupId, testNgEngineArtifactId, version, providerArtifacts);
                    consoleLogger.info(
                            "TestNG is present. Adding per default " + testNgEngineCoordinates + ":" + version);
                }
            }

            if (!hasDependencyPlatformEngine(providerArtifacts)) {
                addEngineByApi(engineGroupId, engineArtifactId, engineVersion, providerArtifacts);
            }

            if (!hasDependencyPlatformEngine(providerArtifacts)) {
                addEngineByApi(engineGroupId, engineArtifactId, engineVersion, providerArtifacts);
            }

            narrowDependencies(providerArtifacts, testDeps);
            alignProviderVersions(providerArtifacts, engineVersion, testDeps.get(api));

            return new LinkedHashSet<>(providerArtifacts.values());
        }

        private void addEngineByApi(
                String engineGroupId,
                String engineArtifactId,
                String engineVersion,
                Map<String, Artifact> providerArtifacts)
                throws MojoExecutionException {
            for (Artifact dep : resolve(engineGroupId, engineArtifactId, engineVersion, null, "jar")) {
                String key = dep.getGroupId() + ":" + dep.getArtifactId();
                Artifact removed = providerArtifacts.put(key, dep);
                if (removed != null) {
                    consoleLogger.debug("Replaced provider dependency " + key + " " + removed + " with " + dep);
                    ComparableVersion removedVersion = new ComparableVersion(removed.getVersion());
                    ComparableVersion depVersion = new ComparableVersion(dep.getVersion());
                    // keep the highest version
                    if (removedVersion.compareTo(depVersion) > 0) {
                        providerArtifacts.put(key, removed);
                        consoleLogger.debug("Kept higher version " + key + " " + removed);
                    }
                }
            }
        }

        private String getDefaultJunitVersion() {
            try (InputStream inputStream = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream("surefire.junit.version.properties")) {
                Properties junitProperties = new Properties();
                junitProperties.load(inputStream);
                return junitProperties.getProperty("junit.version");
            } catch (Exception e) {
                consoleLogger.warning(
                        "Could not load default JUnit version from junit.version.properties use default 5.14.1");
                return "5.14.1";
            }
        }

        private void narrowDependencies(
                Map<String, Artifact> providerArtifacts, Map<String, Artifact> testDependencies) {
            providerArtifacts.keySet().removeAll(testDependencies.keySet());
        }

        protected void alignProviderVersions(
                Map<String, Artifact> providerArtifacts, String engineVersion, Artifact engineApi)
                throws MojoExecutionException {
            if (junitPlatformArtifact == null) {
                return;
            }

            String version = junitPlatformArtifact.getBaseVersion();
            // this might be dangerous for version before 6.x where versions were different between launcher, platform,,
            boolean is6x = false;
            if (engineApi != null) {
                is6x = isWithinVersionSpec(engineApi, "[6.0.0,)");
            }

            if (!version.equals(engineVersion) && is6x) {
                version = engineVersion;
            }
            for (Artifact launcherArtifact : resolve(PROVIDER_DEP_GID, PROVIDER_DEP_AID, version, null, "jar")) {
                String key = launcherArtifact.getGroupId() + ":" + launcherArtifact.getArtifactId();
                if (providerArtifacts.containsKey(key)) {
                    providerArtifacts.put(key, launcherArtifact);
                }
            }
        }

        private Set<Artifact> resolve(String groupId, String artifactId, String version, String classifier, String type)
                throws MojoExecutionException {
            // FIXME will be different with 3 and testng
            ArtifactHandler handler = junitPlatformArtifact != null
                    ? junitPlatformArtifact.getArtifactHandler()
                    : junitArtifact != null
                            ? junitArtifact.getArtifactHandler()
                            : testNgArtifact != null ? testNgArtifact.getArtifactHandler() : null;
            Artifact artifact = new DefaultArtifact(groupId, artifactId, version, null, type, classifier, handler);
            consoleLogger.debug("Resolving artifact " + groupId + ":" + artifactId + ":" + version);
            Set<Artifact> r = surefireDependencyResolver.resolveArtifacts(
                    session.getRepositorySession(), project.getRemoteProjectRepositories(), artifact);
            consoleLogger.debug("Resolved artifact " + groupId + ":" + artifactId + ":" + version + " to " + r);
            return r;
        }

        private boolean hasDependencyJupiterAPI(Map<String, Artifact> dependencies) {
            return dependencies.containsKey("org.junit.jupiter:junit-jupiter-api");
        }

        private boolean hasDependencyPlatformEngine(Map<String, Artifact> dependencies) {
            return dependencies.entrySet().stream()
                    .anyMatch(stringArtifactEntry ->
                            stringArtifactEntry.getKey().equals("org.junit.platform:junit-platform-engine"));
        }
    }

    // TODO why an enclosing class on the top do we real need this shading??
    @SuppressWarnings("checkstyle:parameternumber")
    final class JUnitPlatformProviderShadefireInfo extends JUnitPlatformProviderInfo {

        JUnitPlatformProviderShadefireInfo(
                Artifact junitPlatformRunnerArtifact,
                Artifact junitPlatformArtifact,
                @Nonnull TestClassPath testClasspath,
                Artifact junitArtifact,
                Artifact booterArtifact,
                SurefireDependencyResolver surefireDependencyResolver,
                MavenSession session,
                MavenProject project,
                PluginDescriptor pluginDescriptor,
                Map<String, Artifact> pluginArtifactMap,
                ConsoleLogger consoleLogger,
                Artifact testNgArtifact) {
            super(
                    junitPlatformRunnerArtifact,
                    junitPlatformArtifact,
                    testClasspath,
                    junitArtifact,
                    booterArtifact,
                    surefireDependencyResolver,
                    session,
                    project,
                    pluginDescriptor,
                    pluginArtifactMap,
                    consoleLogger,
                    testNgArtifact);
        }

        @Override
        public boolean isApplicable() {
            // newer discover this provider automatically
            return false;
        }

        @Override
        @Nonnull
        public String getProviderName() {
            return "org.apache.maven.shadefire.surefire.junitplatform.JUnitPlatformProvider";
        }

        @Override
        protected String getProviderArtifactName() {
            return "surefire-shadefire";
        }

        @Override
        protected void alignProviderVersions(
                Map<String, Artifact> providerArtifacts, String engineVersion, Artifact engineApi)
                throws MojoExecutionException {
            // shadefire is used as booter we can not provide additional dependencies,
            // so we need add a launcher here
            providerArtifacts.put("org.junit.platform:junit-platform-launcher", null);
            super.alignProviderVersions(providerArtifacts, engineVersion, engineApi);
        }
    }

    File createSurefireBootDirectoryInBuild() {
        File tmp = new File(getProjectBuildDirectory(), getTempDir());
        //noinspection ResultOfMethodCallIgnored
        tmp.mkdirs();
        return tmp;
    }

    File createSurefireBootDirectoryInTemp() {
        try {
            return Files.createTempDirectory(getTempDir()).toFile();
        } catch (IOException e) {
            return createSurefireBootDirectoryInBuild();
        }
    }

    @Override
    public String getLocalRepositoryPath() {
        return Optional.ofNullable(
                        session.getRepositorySession().getLocalRepository().getBasedir())
                .map(File::getAbsolutePath)
                .orElse(".");
    }

    public Properties getSystemProperties() {
        return systemProperties;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setSystemProperties(Properties systemProperties) {
        this.systemProperties = systemProperties;
    }

    public Map<String, String> getSystemPropertyVariables() {
        return systemPropertyVariables;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setSystemPropertyVariables(Map<String, String> systemPropertyVariables) {
        this.systemPropertyVariables = systemPropertyVariables;
    }

    /**
     * List of System properties, loaded from a file, to pass to the JUnit tests.
     *
     * @since 2.8.2
     */
    public abstract File getSystemPropertiesFile();

    @SuppressWarnings("UnusedDeclaration")
    public abstract void setSystemPropertiesFile(File systemPropertiesFile);

    private Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public Map<String, Artifact> getPluginArtifactMap() {
        return pluginArtifactMap;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setPluginArtifactMap(Map<String, Artifact> pluginArtifactMap) {
        this.pluginArtifactMap = pluginArtifactMap;
    }

    public Map<String, Artifact> getProjectArtifactMap() {
        return projectArtifactMap;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setProjectArtifactMap(Map<String, Artifact> projectArtifactMap) {
        this.projectArtifactMap = projectArtifactMap;
    }

    public String getReportNameSuffix() {
        return reportNameSuffix;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setReportNameSuffix(String reportNameSuffix) {
        this.reportNameSuffix = reportNameSuffix;
    }

    public boolean isRedirectTestOutputToFile() {
        return redirectTestOutputToFile;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setRedirectTestOutputToFile(boolean redirectTestOutputToFile) {
        this.redirectTestOutputToFile = redirectTestOutputToFile;
    }

    public boolean getFailIfNoTests() {
        return failIfNoTests;
    }

    public void setFailIfNoTests(boolean failIfNoTests) {
        this.failIfNoTests = failIfNoTests;
    }

    public String getJvm() {
        return jvm;
    }

    public String getArgLine() {
        return argLine;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setArgLine(String argLine) {
        this.argLine = argLine;
    }

    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public boolean isChildDelegation() {
        return childDelegation;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setChildDelegation(boolean childDelegation) {
        this.childDelegation = childDelegation;
    }

    public String getGroups() {
        return groups;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setGroups(String groups) {
        this.groups = groups;
    }

    public String getExcludedGroups() {
        return excludedGroups;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setExcludedGroups(String excludedGroups) {
        this.excludedGroups = excludedGroups;
    }

    public String getJunitArtifactName() {
        return junitArtifactName;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setJunitArtifactName(String junitArtifactName) {
        this.junitArtifactName = junitArtifactName;
    }

    public String getTestNGArtifactName() {
        return testNGArtifactName;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setTestNGArtifactName(String testNGArtifactName) {
        this.testNGArtifactName = testNGArtifactName;
    }

    public int getThreadCount() {
        return threadCount;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public boolean getPerCoreThreadCount() {
        return perCoreThreadCount;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setPerCoreThreadCount(boolean perCoreThreadCount) {
        this.perCoreThreadCount = perCoreThreadCount;
    }

    public boolean getUseUnlimitedThreads() {
        return useUnlimitedThreads;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setUseUnlimitedThreads(boolean useUnlimitedThreads) {
        this.useUnlimitedThreads = useUnlimitedThreads;
    }

    public String getParallel() {
        return parallel;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setParallel(String parallel) {
        this.parallel = parallel;
    }

    public boolean isParallelOptimized() {
        return parallelOptimized;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setParallelOptimized(boolean parallelOptimized) {
        this.parallelOptimized = parallelOptimized;
    }

    public int getThreadCountSuites() {
        return threadCountSuites;
    }

    public void setThreadCountSuites(int threadCountSuites) {
        this.threadCountSuites = threadCountSuites;
    }

    public int getThreadCountClasses() {
        return threadCountClasses;
    }

    public void setThreadCountClasses(int threadCountClasses) {
        this.threadCountClasses = threadCountClasses;
    }

    public int getThreadCountMethods() {
        return threadCountMethods;
    }

    public void setThreadCountMethods(int threadCountMethods) {
        this.threadCountMethods = threadCountMethods;
    }

    public boolean isTrimStackTrace() {
        return trimStackTrace;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setTrimStackTrace(boolean trimStackTrace) {
        this.trimStackTrace = trimStackTrace;
    }

    public boolean isEnableAssertions() {
        return enableAssertions;
    }

    public boolean effectiveIsEnableAssertions() {
        if (getArgLine() != null) {
            List<String> args = asList(getArgLine().split(" "));
            if (args.contains("-da") || args.contains("-disableassertions")) {
                return false;
            }
        }
        return isEnableAssertions();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setEnableAssertions(boolean enableAssertions) {
        this.enableAssertions = enableAssertions;
    }

    public boolean isEnableOutErrElements() {
        return enableOutErrElements;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setEnableOutErrElements(boolean enableOutErrElements) {
        this.enableOutErrElements = enableOutErrElements;
    }

    public boolean isEnablePropertiesElement() {
        return enablePropertiesElement;
    }

    public boolean isReportTestTimestamp() {
        return reportTestTimestamp;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setEnablePropertiesElement(boolean enablePropertiesElement) {
        this.enablePropertiesElement = enablePropertiesElement;
    }

    public MavenSession getSession() {
        return session;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setSession(MavenSession session) {
        this.session = session;
    }

    public String getObjectFactory() {
        return objectFactory;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setObjectFactory(String objectFactory) {
        this.objectFactory = objectFactory;
    }

    public ToolchainManager getToolchainManager() {
        return toolchainManager;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setToolchainManager(ToolchainManager toolchainManager) {
        this.toolchainManager = toolchainManager;
    }

    public boolean isMavenParallel() {
        return parallelMavenExecution != null && parallelMavenExecution;
    }

    public String[] getDependenciesToScan() {
        return dependenciesToScan;
    }

    public void setDependenciesToScan(String[] dependenciesToScan) {
        this.dependenciesToScan = dependenciesToScan;
    }

    @SuppressWarnings("UnusedDeclaration")
    void setPluginDescriptor(PluginDescriptor pluginDescriptor) {
        this.pluginDescriptor = pluginDescriptor;
    }

    public PluginDescriptor getPluginDescriptor() {
        return pluginDescriptor;
    }

    public MavenProject getProject() {
        return project;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setProject(MavenProject project) {
        this.project = project;
    }

    @Override
    public File getTestSourceDirectory() {
        return testSourceDirectory;
    }

    @Override
    public void setTestSourceDirectory(File testSourceDirectory) {
        this.testSourceDirectory = testSourceDirectory;
    }

    public String getForkCount() {
        return forkCount;
    }

    public boolean isReuseForks() {
        return reuseForks;
    }

    public String[] getAdditionalClasspathElements() {
        return additionalClasspathElements;
    }

    public void setAdditionalClasspathElements(String[] additionalClasspathElements) {
        this.additionalClasspathElements = additionalClasspathElements;
    }

    public String[] getClasspathDependencyExcludes() {
        return classpathDependencyExcludes;
    }

    public void setClasspathDependencyExcludes(String[] classpathDependencyExcludes) {
        this.classpathDependencyExcludes = classpathDependencyExcludes;
    }

    public String getClasspathDependencyScopeExclude() {
        return classpathDependencyScopeExclude;
    }

    public void setClasspathDependencyScopeExclude(String classpathDependencyScopeExclude) {
        this.classpathDependencyScopeExclude = classpathDependencyScopeExclude;
    }

    public File getProjectBuildDirectory() {
        return projectBuildDirectory;
    }

    public void setProjectBuildDirectory(File projectBuildDirectory) {
        this.projectBuildDirectory = projectBuildDirectory;
    }

    protected void logDebugOrCliShowErrors(String s) {
        SurefireHelper.logDebugOrCliShowErrors(s, getConsoleLogger(), cli);
    }

    public Map<String, String> getJdkToolchain() {
        return jdkToolchain;
    }

    public void setJdkToolchain(Map<String, String> jdkToolchain) {
        this.jdkToolchain = jdkToolchain;
    }

    public String getTempDir() {
        return tempDir;
    }

    public void setTempDir(String tempDir) {
        this.tempDir = tempDir;
    }

    private static final class ClasspathCache {
        private final Map<String, Classpath> classpaths = new HashMap<>(4);

        private Classpath getCachedClassPath(@Nonnull String artifactId) {
            return classpaths.get(artifactId);
        }

        private void setCachedClasspath(@Nonnull String key, @Nonnull Classpath classpath) {
            classpaths.put(key, classpath);
        }

        private Classpath setCachedClasspath(@Nonnull String key, @Nonnull Set<Artifact> artifacts) {
            Collection<String> files = new ArrayList<>();
            for (Artifact artifact : artifacts) {
                files.add(artifact.getFile().getAbsolutePath());
            }
            Classpath classpath = new Classpath(files);
            setCachedClasspath(key, classpath);
            return classpath;
        }
    }

    /**
     * Determines whether the plugin should fail if no tests found to run.
     */
    enum PluginFailureReason {
        NONE,
        COULD_NOT_RUN_SPECIFIED_TESTS,
        COULD_NOT_RUN_DEFAULT_TESTS,
    }
}
