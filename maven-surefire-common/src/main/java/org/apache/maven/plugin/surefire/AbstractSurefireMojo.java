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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.surefire.booterclient.ChecksumCalculator;
import org.apache.maven.plugin.surefire.booterclient.ForkConfiguration;
import org.apache.maven.plugin.surefire.booterclient.ForkStarter;
import org.apache.maven.shared.artifact.filter.PatternIncludesArtifactFilter;
import org.apache.maven.surefire.booter.ClassLoaderConfiguration;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.ClasspathConfiguration;
import org.apache.maven.surefire.booter.ProviderConfiguration;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.report.BriefConsoleReporter;
import org.apache.maven.surefire.report.BriefFileReporter;
import org.apache.maven.surefire.report.ConsoleReporter;
import org.apache.maven.surefire.report.DetailedConsoleReporter;
import org.apache.maven.surefire.report.FileReporter;
import org.apache.maven.surefire.report.ForkingConsoleReporter;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.report.XMLReporter;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestRequest;
import org.apache.maven.surefire.util.NestedRuntimeException;
import org.apache.maven.surefire.util.Relocator;
import org.apache.maven.toolchain.Toolchain;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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

    private static final String BRIEF_REPORT_FORMAT = "brief";

    private static final String PLAIN_REPORT_FORMAT = "plain";

    // common field getters/setters

    // common code

    protected abstract String getPluginName();

    private SurefireDependencyResolver dependencyResolver;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {

        if ( verifyParameters() && !hasExecutedBefore() )
        {
            logReportsDirectory();
            executeAfterPreconditionsChecked();
        }
    }

    protected boolean verifyParameters()
        throws MojoFailureException
    {
        if ( isSkipExecution() )
        {
            getLog().info( "Tests are skipped." );
            return false;
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

    protected abstract void executeAfterPreconditionsChecked()
        throws MojoExecutionException, MojoFailureException;

    private Artifact surefireArtifact;

    protected List initialize()
        throws MojoFailureException
    {
        dependencyResolver =
            new SurefireDependencyResolver( getArtifactResolver(), getArtifactFactory(), getLog(),
                                            getLocalRepository(), getRemoteRepositories(), getMetadataSource(),
                                            getPluginName() );

        try
        {
            final Artifact junitDepArtifact = getJunitDepArtifact();
            ProviderList wellKnownProviders =
                new ProviderList( new ProviderInfo[] { new TestNgProviderInfo( getTestNgArtifact() ),
                    new JUnitCoreProviderInfo( getJunitArtifact(), junitDepArtifact ),
                    new JUnit4ProviderInfo( getJunitArtifact(), junitDepArtifact ), new JUnit3ProviderInfo() },
                                  new DynamicProviderInfo( null ) );

            return wellKnownProviders.resolve( getLog() );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new NestedRuntimeException( e );
        }
    }

    protected void logReportsDirectory()
    {
        getLog().info( StringUtils.capitalizeFirstLetter( getPluginName() ) + " report directory: "
                           + getReportsDirectory() );
    }

    protected final Toolchain getToolchain()
    {
        Toolchain tc = null;

        if ( getToolchainManager() != null )
        {
            tc = getToolchainManager().getToolchainFromBuildContext( "jdk", getSession() );
        }

        return tc;
    }

    /**
     * Converts old TestNG configuration parameters over to new properties based configuration method. (if any are
     * defined the old way)
     */
    private void convertTestNGParameters()
    {
        if ( getProperties() == null ) // May be predefined from plugin paramaters
        {
            setProperties( new Properties() );
        }

        if ( this.getParallel() != null )
        {
            getProperties().setProperty( "parallel", this.getParallel() );
        }
        if ( this.getExcludedGroups() != null )
        {
            getProperties().setProperty( "excludegroups", this.getExcludedGroups() );
        }
        if ( this.getGroups() != null )
        {
            getProperties().setProperty( "groups", this.getGroups() );
        }

        if ( this.getThreadCount() > 0 )
        {
            getProperties().setProperty( "threadcount", Integer.toString( this.getThreadCount() ) );
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

    protected boolean isAnyConcurrencySelected()
    {
        return this.getParallel() != null && this.getParallel().trim().length() > 0;
    }

    /**
     * Converts old JUnit configuration parameters over to new properties based configuration method. (if any are
     * defined the old way)
     */
    private void convertJunitCoreParameters()
    {
        if ( getProperties() == null )
        {
            setProperties( new Properties() );
        }

        if ( this.getParallel() != null )
        {
            getProperties().setProperty( "parallel", this.getParallel() );
        }
        if ( this.getThreadCount() > 0 )
        {
            getProperties().setProperty( "threadCount", Integer.toString( this.getThreadCount() ) );
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

    protected boolean isForkModeNever()
    {
        return ForkConfiguration.FORK_NEVER.equals( getForkMode() );
    }

    protected ProviderConfiguration createProviderConfiguration( ForkConfiguration forkConfiguration, boolean shadefire )
        throws MojoExecutionException, MojoFailureException
    {

        List reports = getReporters( forkConfiguration.isForking() );
        reports = shadefire ? new Relocator().relocateReports( reports ) : reports;
        Integer timeoutSet =
            getForkedProcessTimeoutInSeconds() > 0 ? Integer.valueOf( getForkedProcessTimeoutInSeconds() ) : null;
        ReporterConfiguration reporterConfiguration =
            new ReporterConfiguration( reports, getReportsDirectory(), Boolean.valueOf( isTrimStackTrace() ),
                                       timeoutSet );

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
        List testXml = getSuiteXmlFiles() != null ? Arrays.asList( getSuiteXmlFiles() ) : null;
        TestRequest testSuiteDefinition =
            new TestRequest( testXml, getTestSourceDirectory(), getTest(), getTestMethod() );
        final boolean failIfNoTests;

        if ( isValidSuiteXmlFileConfig() && getTest() == null )
        {
            failIfNoTests = getFailIfNoTests() != null && getFailIfNoTests().booleanValue();
            if ( !isTestNg )
            {
                throw new MojoExecutionException( "suiteXmlFiles is configured, but there is no TestNG dependency" );
            }
        }
        else
        {
            if ( isSpecificTestSpecified() && getFailIfNoTests() == null )
            {
                setFailIfNoTests( Boolean.TRUE );
            }

            failIfNoTests = getFailIfNoTests() != null && getFailIfNoTests().booleanValue();

            List includes = getIncludeList();
            List excludes = getExcludeList();
            directoryScannerParameters =
                new DirectoryScannerParameters( getTestClassesDirectory(), includes, excludes,
                                                Boolean.valueOf( failIfNoTests ), getRunOrder() );
        }

        Properties providerProperties = getProperties();
        if ( providerProperties == null )
        {
            providerProperties = new Properties();
        }

        ProviderConfiguration providerConfiguration1 =
            new ProviderConfiguration( directoryScannerParameters, failIfNoTests, reporterConfiguration, testNg,
                                       testSuiteDefinition, providerProperties, null );

        Toolchain tc = getToolchain();

        if ( tc != null )
        {
            getLog().info( "Toolchain in " + getPluginName() + "-plugin: " + tc );
            if ( isForkModeNever() )
            {
                setForkMode( ForkConfiguration.FORK_ONCE );
            }
            if ( getJvm() != null )
            {
                getLog().warn( "Toolchains are ignored, 'executable' parameter is set to " + getJvm() );
            }
            else
            {
                setJvm( tc.findTool( "java" ) ); // NOI18N
            }
        }

        return providerConfiguration1;
    }

    protected StartupConfiguration createStartupConfiguration( ForkConfiguration forkConfiguration,
                                                               ProviderInfo provider,
                                                               ClassLoaderConfiguration classLoaderConfiguration )
        throws MojoExecutionException, MojoFailureException
    {

        try
        {
            provider.addProviderProperties();
            String providerName = provider.getProviderName();
            final Classpath providerClasspath = provider.getProviderClasspath();
            final Classpath testClasspath = generateTestClasspath();

            logClasspath( testClasspath, "test classpath" );
            logClasspath( testClasspath, "provider classpath" );
            final ClasspathConfiguration classpathConfiguration =
                new ClasspathConfiguration( testClasspath, providerClasspath, isEnableAssertions(), isChildDelegation() );

            return new StartupConfiguration( providerName, classpathConfiguration, classLoaderConfiguration,
                                             forkConfiguration.isForking(), false, isRedirectTestOutputToFile() );
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MojoExecutionException( "Unable to generate classpath: " + e, e );
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

    public void logClasspath( Classpath classpath, String descriptor )
    {
        getLog().debug( descriptor + " classpath:" );
        for ( Iterator i = classpath.getClassPath().iterator(); i.hasNext(); )
        {
            String classpathElement = (String) i.next();
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

    private List getExcludeList()
    {
        List excludes;
        if ( isSpecificTestSpecified() )
        {
            // Check to see if we are running a single test. The raw parameter will
            // come through if it has not been set.
            // FooTest -> **/FooTest.java

            excludes = new ArrayList();
        }
        else
        {

            excludes = this.getExcludes();

            // defaults here, qdox doesn't like the end javadoc value
            // Have to wrap in an ArrayList as surefire expects an ArrayList instead of a List for some reason
            if ( excludes == null || excludes.size() == 0 )
            {
                excludes = new ArrayList( Arrays.asList( new String[] { "**/*$*" } ) );
            }
        }
        return excludes;
    }

    private List getIncludeList()
    {
        List includes;
        if ( isSpecificTestSpecified() )
        {
            // Check to see if we are running a single test. The raw parameter will
            // come through if it has not been set.

            // FooTest -> **/FooTest.java

            includes = new ArrayList();

            String[] testRegexes = StringUtils.split( getTest(), "," );

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
            includes = this.getIncludes();

            // defaults here, qdox doesn't like the end javadoc value
            // Have to wrap in an ArrayList as surefire expects an ArrayList instead of a List for some reason
            if ( includes == null || includes.size() == 0 )
            {
                includes = new ArrayList( Arrays.asList( getDefaultIncludes() ) );
            }
        }
        return includes;
    }

    private Artifact getTestNgArtifact()
        throws MojoFailureException, InvalidVersionSpecificationException
    {
        // TODO: this is pretty manual, but I'd rather not require the plugin > dependencies section right now
        Artifact artifact = (Artifact) getProjectArtifactMap().get( getTestNGArtifactName() );

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
        return (Artifact) getProjectArtifactMap().get( getJunitArtifactName() );
    }

    private Artifact getJunitDepArtifact()
    {
        return (Artifact) getProjectArtifactMap().get( "junit:junit-dep" );
    }

    protected ForkStarter createForkStarter( ProviderInfo provider, ForkConfiguration forkConfiguration,
                                             ClassLoaderConfiguration classLoaderConfiguration )
        throws MojoExecutionException, MojoFailureException
    {
        StartupConfiguration startupConfiguration =
            createStartupConfiguration( forkConfiguration, provider, classLoaderConfiguration );
        ProviderConfiguration providerConfiguration =
            createProviderConfiguration( forkConfiguration, startupConfiguration.isShadefire() );
        return new ForkStarter( providerConfiguration, startupConfiguration, getReportsDirectory(), forkConfiguration,
                                getForkedProcessTimeoutInSeconds(), isPrintSummary() );
    }

    protected ForkConfiguration getForkConfiguration()
    {
        File tmpDir = getSurefireTempDir();
        // noinspection ResultOfMethodCallIgnored
        tmpDir.mkdirs();

        Artifact shadeFire = (Artifact) getPluginArtifactMap().get( "org.apache.maven.surefire:surefire-shadefire" );

        surefireArtifact = (Artifact) getPluginArtifactMap().get( "org.apache.maven.surefire:surefire-booter" );
        if ( surefireArtifact == null )
        {
            throw new RuntimeException( "Unable to locate surefire-booter in the list of plugin artifacts" );
        }

        surefireArtifact.isSnapshot(); // MNG-2961: before Maven 2.0.8, fixes getBaseVersion to be -SNAPSHOT if needed

        final Classpath bootClasspathConfiguration =
            getArtifactClasspath( shadeFire != null ? shadeFire : surefireArtifact );

        ForkConfiguration fork = new ForkConfiguration( bootClasspathConfiguration, getForkMode(), tmpDir );

        fork.setTempDirectory( tmpDir );

        processSystemProperties( !fork.isForking() );

        verifyLegalSystemProperties();

        if ( getLog().isDebugEnabled() )
        {
            showMap( getInternalSystemProperties(), "system property" );
        }

        if ( fork.isForking() )
        {
            setUseSystemClassLoader( isUseSystemClassLoader() );

            fork.setSystemProperties( getInternalSystemProperties() );

            if ( "true".equals( getDebugForkedProcess() ) )
            {
                setDebugForkedProcess( "-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005" );
            }

            fork.setDebugLine( getDebugForkedProcess() );

            if ( getJvm() == null || "".equals( getJvm() ) )
            {
                // use the same JVM as the one used to run Maven (the "java.home" one)
                setJvm( System.getProperty( "java.home" ) + File.separator + "bin" + File.separator + "java" );
                getLog().debug( "Using JVM: " + getJvm() );
            }

            fork.setJvmExecutable( getJvm() );

            if ( getWorkingDirectory() != null )
            {
                fork.setWorkingDirectory( getWorkingDirectory() );
            }
            else
            {
                fork.setWorkingDirectory( getBasedir() );
            }

            fork.setArgLine( getArgLine() );

            fork.setEnvironmentVariables( getEnvironmentVariables() );

            if ( getLog().isDebugEnabled() )
            {
                showMap( getEnvironmentVariables(), "environment variable" );

                fork.setDebug( true );
            }

            if ( getArgLine() != null )
            {
                List args = Arrays.asList( getArgLine().split( " " ) );
                if ( args.contains( "-da" ) || args.contains( "-disableassertions" ) )
                {
                    setEnableAssertions( false );
                }
            }
        }
        return fork;
    }

    private void verifyLegalSystemProperties()
    {
        final Properties properties = getInternalSystemProperties();
        Iterator iter = properties.keySet().iterator();

        while ( iter.hasNext() )
        {
            String key = (String) iter.next();

            if ( "java.library.path".equals( key ) )
            {
                getLog().warn( "java.library.path cannot be set as system property, use <argLine>-Djava.library.path=...<argLine> instead" );
            }
        }
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
        checksum.add( getProperties() );
        checksum.add( isPrintSummary() );
        checksum.add( getReportFormat() );
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

    protected abstract void addPluginSpecificChecksumItems( ChecksumCalculator checksum );

    protected boolean hasExecutedBefore()
    {
        // A tribute to Linus Torvalds
        String configChecksum = getConfigChecksum();
        Map pluginContext = getPluginContext();
        if ( pluginContext.containsKey( configChecksum ) )
        {
            getLog().info( "Skipping execution of surefire because it has already been run for this configuration" );
            return true;
        }
        pluginContext.put( configChecksum, configChecksum );

        return false;
    }

    protected ClassLoaderConfiguration getClassLoaderConfiguration( ForkConfiguration fork )
    {
        return fork.isForking() ? new ClassLoaderConfiguration( isUseSystemClassLoader(), isUseManifestOnlyJar() )
                        : new ClassLoaderConfiguration( false, false );
    }

    protected abstract String[] getDefaultIncludes();

    /**
     * Generate the test classpath.
     * 
     * @return List containing the classpath elements
     * @throws org.apache.maven.artifact.DependencyResolutionRequiredException when dependency resolution fails
     * @throws org.apache.maven.plugin.MojoExecutionException upon other problems
     * @throws InvalidVersionSpecificationException when it happens
     * @throws MojoFailureException when it happens
     * @throws ArtifactNotFoundException when it happens
     * @throws ArtifactResolutionException when it happens
     */
    public Classpath generateTestClasspath()
        throws DependencyResolutionRequiredException, MojoExecutionException, InvalidVersionSpecificationException,
        MojoFailureException, ArtifactResolutionException, ArtifactNotFoundException
    {
        List classpath = new ArrayList( 2 + getProject().getArtifacts().size() );

        classpath.add( getTestClassesDirectory().getAbsolutePath() );

        classpath.add( getClassesDirectory().getAbsolutePath() );

        Set classpathArtifacts = getProject().getArtifacts();

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

        for ( Iterator iter = classpathArtifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
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
            for ( Iterator iter = getAdditionalClasspathElements().iterator(); iter.hasNext(); )
            {
                String classpathElement = (String) iter.next();
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
            Artifact testNgUtils = getTestNgUtilsArtifact();
            String path = testNgUtils.getFile().getPath();
            classpath.add( path );

        }

        return new Classpath( classpath );
    }

    protected Artifact getTestNgUtilsArtifact()
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        Artifact surefireArtifact = (Artifact) getPluginArtifactMap().get( "org.apache.maven.surefire:surefire-booter" );
        String surefireVersion = surefireArtifact.getBaseVersion();
        Artifact testNgUtils =
            getArtifactFactory().createArtifact( "org.apache.maven.surefire", "surefire-testng-utils", surefireVersion,
                                                 "runtime", "jar" );

        getArtifactResolver().resolve( testNgUtils, getRemoteRepositories(), getLocalRepository() );
        return testNgUtils;
    }

    /**
     * Return a new set containing only the artifacts accepted by the given filter.
     * 
     * @param artifacts The unfiltered artifacts
     * @param filter The filter to apply
     * @return The filtered result
     */
    private Set filterArtifacts( Set artifacts, ArtifactFilter filter )
    {
        Set filteredArtifacts = new LinkedHashSet();

        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            if ( !filter.include( artifact ) )
            {
                filteredArtifacts.add( artifact );
            }
        }

        return filteredArtifacts;
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

    private ArtifactResolutionResult resolveArtifact( Artifact filteredArtifact, Artifact providerArtifact )
    {
        ArtifactFilter filter = null;
        if ( filteredArtifact != null )
        {
            filter =
                new ExcludesArtifactFilter( Collections.singletonList( filteredArtifact.getGroupId() + ":"
                    + filteredArtifact.getArtifactId() ) );
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

        List items = new ArrayList();
        for ( Iterator i = result.getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            getLog().debug( "Adding to " + getPluginName() + " booter test classpath: "
                                + artifact.getFile().getAbsolutePath() + " Scope: " + artifact.getScope() );

            items.add( artifact.getFile().getAbsolutePath() );
        }
        return new Classpath( items );
    }

    protected void processSystemProperties( boolean setInSystem )
    {
        copyPropertiesToInternalSystemProperties( getSystemProperties() );

        if ( this.getSystemPropertyVariables() != null )
        {
            for ( Iterator i = getSystemPropertyVariables().keySet().iterator(); i.hasNext(); )
            {
                String key = (String) i.next();
                String value = (String) getSystemPropertyVariables().get( key );
                // java Properties does not accept null value
                if ( value != null )
                {
                    getInternalSystemProperties().setProperty( key, value );
                }
            }
        }

        setOriginalSystemProperties( (Properties) System.getProperties().clone() );

        // We used to take all of our system properties and dump them in with the
        // user specified properties for SUREFIRE-121, causing SUREFIRE-491.
        // Not gonna do THAT any more... instead, we only propagate those system properties
        // that have been explicitly specified by the user via -Dkey=value on the CLI

        copyPropertiesToInternalSystemProperties( getUserProperties() );

        getInternalSystemProperties().setProperty( "basedir", getBasedir().getAbsolutePath() );
        getInternalSystemProperties().setProperty( "user.dir", getWorkingDirectory().getAbsolutePath() );
        getInternalSystemProperties().setProperty( "localRepository", getLocalRepository().getBasedir() );

        if ( setInSystem )
        {
            // Add all system properties configured by the user
            Iterator iter = getInternalSystemProperties().keySet().iterator();

            while ( iter.hasNext() )
            {
                String key = (String) iter.next();

                String value = getInternalSystemProperties().getProperty( key );

                System.setProperty( key, value );
            }
        }
    }

    private void copyPropertiesToInternalSystemProperties( Properties properties )
    {
        if ( properties != null )
        {
            for ( Iterator i = properties.keySet().iterator(); i.hasNext(); )
            {
                String key = (String) i.next();
                String value = properties.getProperty( key );
                getInternalSystemProperties().setProperty( key, value );
            }
        }
    }

    private Properties getUserProperties()
    {
        Properties props = null;
        try
        {
            // try calling MavenSession.getUserProperties() from Maven 2.1.0-M1+
            Method getUserProperties = getSession().getClass().getMethod( "getUserProperties", null );
            props = (Properties) getUserProperties.invoke( getSession(), null );
        }
        catch ( Exception e )
        {
            String msg =
                "Build uses Maven 2.0.x, cannot propagate system properties"
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

    /**
     * <p/>
     * Adds Reporters that will generate reports with different formatting.
     * <p/>
     * The Reporter that will be added will be based on the value of the parameter useFile, reportFormat, and
     * printSummary.
     * 
     * @param forking forking
     * @return a list of reporters
     */
    private List getReporters( boolean forking )
    {
        List reports = new ArrayList();
        final String consoleReporter = getConsoleReporter( forking );
        if ( consoleReporter != null )
        {
            reports.add( consoleReporter );
        }

        if ( isUseFile() )
        {
            if ( BRIEF_REPORT_FORMAT.equals( getReportFormat() ) )
            {
                reports.add( BriefFileReporter.class.getName() );
            }
            else if ( PLAIN_REPORT_FORMAT.equals( getReportFormat() ) )
            {
                reports.add( FileReporter.class.getName() );
            }
        }

        if ( !isDisableXmlReport() )
        {
            reports.add( XMLReporter.class.getName() );
        }
        return reports;
    }

    /**
     * Returns the reporter that will write to the console
     * 
     * @param forking forking
     * @return a console reporter of null if no console reporting
     */
    private String getConsoleReporter( boolean forking )
    {
        if ( isUseFile() )
        {
            if ( forking )
            {
                return ForkingConsoleReporter.class.getName();
            }
            else
            {
                return isPrintSummary() ? ConsoleReporter.class.getName() : null;
            }
        }
        else if ( BRIEF_REPORT_FORMAT.equals( getReportFormat() ) )
        {
            return BriefConsoleReporter.class.getName();
        }
        else if ( PLAIN_REPORT_FORMAT.equals( getReportFormat() ) )
        {
            return DetailedConsoleReporter.class.getName();
        }
        return null;
    }

    protected void ensureWorkingDirectoryExists()
        throws MojoFailureException
    {
        assertWorkingDirectoryNotNull();
        createWorkingDirectoryIfItNotExists();
        assertWorkingDirectoryIsDirectory();
    }

    private void assertWorkingDirectoryNotNull()
        throws MojoFailureException
    {
        if ( getWorkingDirectory() == null )
        {
            throw new MojoFailureException( "The property workingDirectory is null." );
        }
    }

    private void assertWorkingDirectoryIsDirectory()
        throws MojoFailureException
    {
        File workingDirectory = getWorkingDirectory();
        if ( !workingDirectory.isDirectory() )
        {
            throw new MojoFailureException( "workingDirectory " + workingDirectory + " exists and is not a directory" );
        }
    }

    private void createWorkingDirectoryIfItNotExists()
        throws MojoFailureException
    {
        File workingDirectory = getWorkingDirectory();
        if ( !workingDirectory.exists() )
        {
            if ( !workingDirectory.mkdirs() )
            {
                throw new MojoFailureException( "Cannot create workingDirectory " + workingDirectory );
            }
        }
    }

    protected void ensureParallelRunningCompatibility()
        throws MojoFailureException
    {
        if ( isMavenParallel() && isForkModeNever() )
        {
            throw new MojoFailureException( "parallel maven execution is not compatible with surefire forkmode NEVER" );
        }
    }

    protected void warnIfUselessUseSystemClassLoaderParameter()
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
            Artifact surefireArtifact =
                (Artifact) getPluginArtifactMap().get( "org.apache.maven.surefire:surefire-booter" );
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
            return dependencyResolver.getProviderClasspath( "surefire-junit3", surefireArtifact.getBaseVersion(), null );

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
            return dependencyResolver.getProviderClasspath( "surefire-junit4", surefireArtifact.getBaseVersion(), null );

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
            return isAnyConcurrencySelected() && ( isJunitArtifact47 || is47CompatibleJunitDep() );
        }

        public void addProviderProperties()
        {
            convertJunitCoreParameters();
        }

        public Classpath getProviderClasspath()
            throws ArtifactResolutionException, ArtifactNotFoundException
        {
            return dependencyResolver.getProviderClasspath( "surefire-junit47", surefireArtifact.getBaseVersion(), null );
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
            final Map pluginArtifactMap = getPluginArtifactMap();
            Artifact plugin = (Artifact) pluginArtifactMap.get( "org.apache.maven.plugins:maven-surefire-plugin" );
            return dependencyResolver.addProviderToClasspath( pluginArtifactMap, plugin );
        }

    }

}
