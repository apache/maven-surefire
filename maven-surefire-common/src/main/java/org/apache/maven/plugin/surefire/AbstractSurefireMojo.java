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
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
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
import org.apache.maven.surefire.booter.ProviderParameterNames;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.StartupReportConfiguration;
import org.apache.maven.surefire.booter.SurefireBooterForkException;
import org.apache.maven.surefire.booter.SurefireExecutionException;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.RunOrderParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestRequest;
import org.apache.maven.surefire.util.NestedRuntimeException;
import org.apache.maven.surefire.util.RunOrder;
import org.apache.maven.toolchain.Toolchain;
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

    boolean verifyParameters()
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

    protected void executeAfterPreconditionsChecked()
        throws MojoExecutionException, MojoFailureException
    {
        createDependencyResolver();
        Summary summary = executeAllProviders();
        restoreOriginalSystemPropertiesWhenNotForking( summary );
        handleSummary( summary );
    }

    private Artifact surefireBooterArtifact;

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
            ProviderList wellKnownProviders = new ProviderList(
                new ProviderInfo[]{ new TestNgProviderInfo( getTestNgArtifact() ),
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

    private Summary executeAllProviders()
        throws MojoExecutionException, MojoFailureException
    {
        List<ProviderInfo> providers = createProviders();
        Summary summary = new Summary();
        for ( ProviderInfo provider : providers )
        {
            executeProvider( provider, summary );
        }
        return summary;
    }

    private void executeProvider( ProviderInfo provider, Summary summary )
        throws MojoExecutionException, MojoFailureException
    {
        ForkConfiguration forkConfiguration = getForkConfiguration();
        summary.reportForkConfiguration( forkConfiguration );
        ClassLoaderConfiguration classLoaderConfiguration = getClassLoaderConfiguration( forkConfiguration );
        try
        {
            final RunResult result;
            if ( ForkConfiguration.FORK_NEVER.equals( forkConfiguration.getForkMode() ) )
            {
                InPluginVMSurefireStarter surefireStarter =
                    createInprocessStarter( provider, forkConfiguration, classLoaderConfiguration );
                result = surefireStarter.runSuitesInProcess();
            }
            else
            {
                ForkStarter forkStarter = createForkStarter( provider, forkConfiguration, classLoaderConfiguration );
                result = forkStarter.run();
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

    protected abstract void handleSummary( Summary summary )
        throws MojoExecutionException, MojoFailureException;

    protected void restoreOriginalSystemPropertiesWhenNotForking( Summary summary )
    {
        if ( ( getOriginalSystemProperties() != null ) && ( summary.isForking() ) )
        {
            System.setProperties( getOriginalSystemProperties() );
        }
    }

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
        if ( getProperties() == null ) // May be predefined from plugin paramaters
        {
            setProperties( new Properties() );
        }

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
        if ( getProperties() == null ) // May be predefined from plugin paramaters
        {
            setProperties( new Properties() );
        }
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
        return this.getGroups() != null && this.getExcludedGroups() != null;
    }

    /**
     * Converts old JUnit configuration parameters over to new properties based configuration
     * method. (if any are defined the old way)
     */
    private void convertJunitCoreParameters()
    {
        if ( getProperties() == null )
        {
            setProperties( new Properties() );
        }

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
        return ForkConfiguration.FORK_NEVER.equals( getForkMode() );
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

    protected ProviderConfiguration createProviderConfiguration( String configurationHash )
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
                if ( getFailIfNoSpecifiedTests() != null )
                {
                    failIfNoTests = getFailIfNoSpecifiedTests().booleanValue();
                }
                else if ( getFailIfNoTests() != null )
                {
                    failIfNoTests = getFailIfNoTests().booleanValue();
                }
                else
                {
                    failIfNoTests = true;
                }

                setFailIfNoTests( Boolean.valueOf( failIfNoTests ) );
            }
            else
            {
                failIfNoTests = getFailIfNoTests() != null && getFailIfNoTests().booleanValue();
            }

            List<String> includes = getIncludeList();
            List<String> excludes = getExcludeList();
            List<String> specificTests = getSpecificTests();
            directoryScannerParameters =
                new DirectoryScannerParameters( getTestClassesDirectory(), includes, excludes, specificTests,
                                                failIfNoTests, getRunOrder() );
        }

        Properties providerProperties = getProperties();
        if ( providerProperties == null )
        {
            providerProperties = new Properties();
        }

        RunOrderParameters runOrderParameters =
            new RunOrderParameters( getRunOrder(), getStatisticsFileName( configurationHash ) );

        return new ProviderConfiguration( directoryScannerParameters, runOrderParameters, failIfNoTests,
                                          reporterConfiguration, testNg, testSuiteDefinition, providerProperties,
                                          null );
    }

    public String getStatisticsFileName( String configurationHash )
    {
        return getReportsDirectory().getParentFile().getParentFile() + File.separator + ".surefire-"
            + configurationHash;
    }


    StartupConfiguration createStartupConfiguration( ForkConfiguration forkConfiguration, ProviderInfo provider,
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
                new ClasspathConfiguration( testClasspath, providerClasspath, inprocClassPath, isEnableAssertions(),
                                            isChildDelegation() );

            return new StartupConfiguration( providerName, classpathConfiguration, classLoaderConfiguration,
                                             forkConfiguration.getForkMode(), false );
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
        List<String> includes = this.getIncludes();

        // defaults here, qdox doesn't like the end javadoc value
        // Have to wrap in an ArrayList as surefire expects an ArrayList instead of a List for some reason
        if ( includes == null || includes.size() == 0 )
        {
            includes = new ArrayList<String>( Arrays.asList( getDefaultIncludes() ) );
        }

        return includes;
    }

    private List<String> getSpecificTests()
    {
        if ( !isSpecificTestSpecified() )
        {
            return Collections.emptyList();
        }

        List<String> specificTests = new ArrayList<String>();
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
            specificTests.add( "**/" + testRegex + ".java" );
        }

        return specificTests;
    }

    private Artifact getTestNgArtifact()
        throws MojoFailureException, InvalidVersionSpecificationException
    {
        // TODO: this is pretty manual, but I'd rather not require the plugin > dependencies section right now
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
                                             ClassLoaderConfiguration classLoaderConfiguration )
        throws MojoExecutionException, MojoFailureException
    {
        StartupConfiguration startupConfiguration =
            createStartupConfiguration( forkConfiguration, provider, classLoaderConfiguration );
        String configChecksum = getConfigChecksum();
        StartupReportConfiguration startupReportConfiguration = getStartupReportConfiguration( configChecksum );
        ProviderConfiguration providerConfiguration = createProviderConfiguration( configChecksum );
        return new ForkStarter( providerConfiguration, startupConfiguration, forkConfiguration,
                                getForkedProcessTimeoutInSeconds(), startupReportConfiguration );
    }

    protected InPluginVMSurefireStarter createInprocessStarter( ProviderInfo provider,
                                                                ForkConfiguration forkConfiguration,
                                                                ClassLoaderConfiguration classLoaderConfiguration )
        throws MojoExecutionException, MojoFailureException
    {
        StartupConfiguration startupConfiguration =
            createStartupConfiguration( forkConfiguration, provider, classLoaderConfiguration );
        String configChecksum = getConfigChecksum();
        StartupReportConfiguration startupReportConfiguration = getStartupReportConfiguration( configChecksum );
        ProviderConfiguration providerConfiguration = createProviderConfiguration( configChecksum );
        return new InPluginVMSurefireStarter( startupConfiguration, providerConfiguration, startupReportConfiguration );

    }

    protected ForkConfiguration getForkConfiguration()
    {
        File tmpDir = getSurefireTempDir();
        //noinspection ResultOfMethodCallIgnored
        tmpDir.mkdirs();

        Artifact shadeFire = getPluginArtifactMap().get( "org.apache.maven.surefire:surefire-shadefire" );

        surefireBooterArtifact = getPluginArtifactMap().get( "org.apache.maven.surefire:surefire-booter" );
        if ( surefireBooterArtifact == null )
        {
            throw new RuntimeException( "Unable to locate surefire-booter in the list of plugin artifacts" );
        }

        surefireBooterArtifact.isSnapshot(); // MNG-2961: before Maven 2.0.8, fixes getBaseVersion to be -SNAPSHOT if needed

        final Classpath bootClasspathConfiguration =
            getArtifactClasspath( shadeFire != null ? shadeFire : surefireBooterArtifact );

        ForkConfiguration fork = new ForkConfiguration( bootClasspathConfiguration, getForkMode(), tmpDir );

        fork.setTempDirectory( tmpDir );

        processSystemProperties( !fork.isForking() );

        verifyLegalSystemProperties();

        if ( getLog().isDebugEnabled() )
        {
            showMap( getInternalSystemProperties(), "system property" );
        }

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
                setJvm( tc.findTool( "java" ) ); //NOI18N
            }
        }

        if ( fork.isForking() )
        {
            setUseSystemClassLoader( isUseSystemClassLoader() );

            fork.setSystemProperties( getInternalSystemProperties() );

            if ( "true".equals( getDebugForkedProcess() ) )
            {
                setDebugForkedProcess(
                    "-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005" );
            }

            fork.setDebugLine( getDebugForkedProcess() );

            if ( ( getJvm() == null || "".equals( getJvm() ) ) )
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
                List<String> args = Arrays.asList( getArgLine().split( " " ) );
                if ( args.contains( "-da" ) || args.contains( "-disableassertions" ) )
                {
                    setEnableAssertions( false );
                }
            }

            if ( fork.getForkMode().equals( ForkConfiguration.FORK_PERTHREAD ) )
            {
                fork.setThreadCount( getThreadCount() );
            }
            else
            {
                fork.setThreadCount( 1 );
            }
        }
        return fork;
    }

    private void verifyLegalSystemProperties()
    {
        final Properties properties = getInternalSystemProperties();

        for ( Object o : properties.keySet() )
        {
            String key = (String) o;

            if ( "java.library.path".equals( key ) )
            {
                getLog().warn(
                    "java.library.path cannot be set as system property, use <argLine>-Djava.library.path=...<argLine> instead" );
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

    protected abstract void addPluginSpecificChecksumItems( ChecksumCalculator checksum );

    protected boolean hasExecutedBefore()
    {
        // A tribute to Linus Torvalds
        String configChecksum = getConfigChecksum();
        @SuppressWarnings( "unchecked" ) Map<String,String> pluginContext = getPluginContext();
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
        return fork.isForking()
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

        @SuppressWarnings( "unchecked" )
        Set<Artifact> classpathArtifacts = getProject().getArtifacts();

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
            Artifact testNgUtils = getTestNgUtilsArtifact();
            String path = testNgUtils.getFile().getPath();
            classpath.add( path );

        }

        return new Classpath( classpath );
    }

    Artifact getTestNgUtilsArtifact()
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        Artifact surefireArtifact = getPluginArtifactMap().get( "org.apache.maven.surefire:surefire-booter" );
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

    private void showMap( Map<?,?> map, String setting )
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

    void processSystemProperties( boolean setInSystem )
    {
        copyPropertiesToInternalSystemProperties( getSystemProperties() );

        if ( this.getSystemPropertiesFile() != null )
        {
            Properties props = new Properties();
            try
            {
                FileInputStream fis = new FileInputStream( getSystemPropertiesFile() );
                props.load( fis );
                fis.close();
            }
            catch ( IOException e )
            {
                String msg =
                    "The system property file '" + getSystemPropertiesFile().getAbsolutePath() + "' can't be read.";
                if ( getLog().isDebugEnabled() )
                {
                    getLog().warn( msg, e );
                }
                else
                {
                    getLog().warn( msg );
                }
            }

            Enumeration<?> keys = props.propertyNames();
            //loop through all properties
            while ( keys.hasMoreElements() )
            {
                String key = (String) keys.nextElement();
                String value = props.getProperty( key );
                getInternalSystemProperties().setProperty( key, value );
            }
        }

        if ( this.getSystemPropertyVariables() != null )
        {
            for ( Object o : getSystemPropertyVariables().keySet() )
            {
                String key = (String) o;
                String value = (String) getSystemPropertyVariables().get( key );
                //java Properties does not accept null value
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

            for ( Object o : getInternalSystemProperties().keySet() )
            {
                String key = (String) o;

                String value = getInternalSystemProperties().getProperty( key );

                System.setProperty( key, value );
            }
        }
    }

    private void copyPropertiesToInternalSystemProperties( Properties properties )
    {
        if ( properties != null )
        {
            for ( Object o : properties.keySet() )
            {
                String key = (String) o;
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
            Artifact surefireArtifact =
                getPluginArtifactMap().get( "org.apache.maven.surefire:surefire-booter" );
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
            final Map<String,Artifact> pluginArtifactMap = getPluginArtifactMap();
            Artifact plugin = pluginArtifactMap.get( "org.apache.maven.plugins:maven-surefire-plugin" );
            return dependencyResolver.addProviderToClasspath( pluginArtifactMap, plugin );
        }

    }

}
