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
import org.apache.maven.plugin.surefire.booterclient.ForkConfiguration;
import org.apache.maven.plugin.surefire.booterclient.ForkStarter;
import org.apache.maven.shared.artifact.filter.PatternIncludesArtifactFilter;
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

    protected SurefireDependencyResolver dependencyResolver;

    protected abstract boolean verifyParameters()
        throws MojoFailureException;

    private Artifact surefireArtifact;


    ProviderList wellKnownProviders;

    protected List initialize()
        throws MojoFailureException
    {
        dependencyResolver =
            new SurefireDependencyResolver( getArtifactResolver(), getArtifactFactory(), getLog(), getLocalRepository(),
                                            getRemoteRepositories(), getMetadataSource(), getPluginName() );

        try
        {
            wellKnownProviders = new ProviderList(new ProviderInfo[]{ new TestNgProviderInfo( getTestNgArtifact() ),
                new JUnitCoreProviderInfo( getJunitArtifact() ), new JUnit4ProviderInfo( getJunitArtifact() ),
                new JUnit3ProviderInfo() }, new DynamicProviderInfo( null ) );

            return wellKnownProviders.resolve(getLog());
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new RuntimeException( e );
        }
    }

    protected void logReportsDirectory()
    {
        getLog().info(
            StringUtils.capitalizeFirstLetter( getPluginName() ) + " report directory: " + getReportsDirectory() );
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
     * Converts old TestNG configuration parameters over to new properties based configuration
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
            getProperties().setProperty( "parallel", this.getParallel() );
        }
        if ( this.getThreadCount() > 0 )
        {
            getProperties().setProperty( "threadCount", Integer.toString( this.getThreadCount() ) );
        }
        if ( this.getPerCoreThreadCount() != null )
        {
            getProperties().setProperty( "perCoreThreadCount", getPerCoreThreadCount() );
        }
        if ( this.getUseUnlimitedThreads() != null )
        {
            getProperties().setProperty( "useUnlimitedThreads", getUseUnlimitedThreads() );
        }
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

    protected ProviderConfiguration createProviderConfiguration( ForkConfiguration forkConfiguration )
        throws MojoExecutionException, MojoFailureException
    {

        List reports = getReporters( forkConfiguration.isForking() );
        ReporterConfiguration reporterConfiguration =
            new ReporterConfiguration( reports, getReportsDirectory(), Boolean.valueOf( isTrimStackTrace() ) );

        surefireArtifact = (Artifact) getPluginArtifactMap().get( "org.apache.maven.surefire:surefire-booter" );
        if ( surefireArtifact == null )
        {
            throw new MojoExecutionException( "Unable to locate surefire-booter in the list of plugin artifacts" );
        }

        surefireArtifact.isSnapshot(); // MNG-2961: before Maven 2.0.8, fixes getBaseVersion to be -SNAPSHOT if needed

        Artifact testNgArtifact;
        try
        {
            addArtifact( forkConfiguration.getBootClasspath(), surefireArtifact );

            testNgArtifact = getTestNgArtifact();
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

        DirectoryScannerParameters directoryScannerParameters = null;
        final boolean isTestNg = testNgArtifact != null;
        TestArtifactInfo testNg =
            isTestNg ? new TestArtifactInfo( testNgArtifact.getVersion(), testNgArtifact.getClassifier() ) : null;
        TestRequest testSuiteDefinition = new TestRequest( getSuiteXmlFiles(), getTestSourceDirectory(), getTest() );
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
            directoryScannerParameters = new DirectoryScannerParameters( getTestClassesDirectory(), includes, excludes,
                                                                         Boolean.valueOf( failIfNoTests ) );
        }

        Properties providerProperties = getProperties();
        if ( providerProperties == null )
        {
            providerProperties = new Properties();
        }

        ProviderConfiguration providerConfiguration1 =
            new ProviderConfiguration( reports, directoryScannerParameters, failIfNoTests, reporterConfiguration,
                                       testNg, testSuiteDefinition, providerProperties, null );

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

        return providerConfiguration1;
    }

    protected StartupConfiguration createStartupConfiguration( ForkConfiguration forkConfiguration,
                                                               ProviderInfo provider )
        throws MojoExecutionException, MojoFailureException
    {
        final ClasspathConfiguration classpathConfiguration =
            new ClasspathConfiguration( isEnableAssertions(), isChildDelegation() );

        surefireArtifact = (Artifact) getPluginArtifactMap().get( "org.apache.maven.surefire:surefire-booter" );
        if ( surefireArtifact == null )
        {
            throw new MojoExecutionException( "Unable to locate surefire-booter in the list of plugin artifacts" );
        }

        surefireArtifact.isSnapshot(); // MNG-2961: before Maven 2.0.8, fixes getBaseVersion to be -SNAPSHOT if needed

        String providerName;
        try
        {
            addArtifact( forkConfiguration.getBootClasspath(), surefireArtifact );

            provider.addProviderProperties();
            provider.addProviderArtifactToBootClasspath( forkConfiguration.getBootClasspath() );
            provider.addProviderArtifactToSurefireClasspath( classpathConfiguration );
            providerName = provider.getProviderName();
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new MojoExecutionException(
                "Unable to locate required surefire provider dependency: " + e.getMessage(), e );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException( "Error to resolving surefire provider dependency: " + e.getMessage(), e );
        }

        List classpathElements;
        try
        {
            classpathElements = generateTestClasspath();
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MojoExecutionException( "Unable to generate test classpath: " + e, e );
        }

        getLog().debug( "Test Classpath :" );

        for ( Iterator i = classpathElements.iterator(); i.hasNext(); )
        {
            String classpathElement = (String) i.next();

            getLog().debug( "  " + classpathElement );

            classpathConfiguration.addClasspathUrl( classpathElement );
        }
        return new StartupConfiguration( providerName, classpathConfiguration,
                                         forkConfiguration.getClassLoaderConfiguration(), forkConfiguration.isForking(),
                                         false, isRedirectTestOutputToFile() );
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
                excludes = new ArrayList( Arrays.asList( new String[]{ "**/*$*" } ) );
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
                    "TestNG support requires version 4.7 or above. You have declared version " +
                        artifact.getVersion() );
            }
        }
        return artifact;

    }

    private Artifact getJunitArtifact()
    {
        Artifact junitArtifact;
        junitArtifact = (Artifact) getProjectArtifactMap().get( getJunitArtifactName() );
        // SUREFIRE-378, junit can have an alternate artifact name
        if ( junitArtifact == null && "junit:junit".equals( getJunitArtifactName() ) )
        {
            junitArtifact = (Artifact) getProjectArtifactMap().get( "junit:junit-dep" );
        }
        return junitArtifact;
    }

    protected ForkStarter createForkStarter( ProviderInfo provider, ForkConfiguration forkConfiguration )
        throws MojoExecutionException, MojoFailureException
    {
        StartupConfiguration startupConfiguration = createStartupConfiguration( forkConfiguration, provider );
        ProviderConfiguration providerConfiguration =
            createProviderConfiguration( forkConfiguration );
        return new ForkStarter( providerConfiguration, startupConfiguration, getReportsDirectory(), forkConfiguration,
                                getForkedProcessTimeoutInSeconds() );
    }

    protected ForkConfiguration getForkConfiguration()
    {
        final Classpath bootClasspathConfiguration = new Classpath();

        ForkConfiguration fork = new ForkConfiguration( bootClasspathConfiguration );

        fork.setForkMode( getForkMode() );

        File tmpDir = new File( getReportsDirectory().getParentFile(), "surefire" );
        //noinspection ResultOfMethodCallIgnored
        tmpDir.mkdirs();
        fork.setTempDirectory( tmpDir );

        processSystemProperties( !fork.isForking() );

        if ( getLog().isDebugEnabled() )
        {
            showMap( getInternalSystemProperties(), "system property" );
        }

        if ( fork.isForking() )
        {
            setUseSystemClassLoader( getUseSystemClassLoader() == null ? Boolean.TRUE : getUseSystemClassLoader() );
            fork.setUseSystemClassLoader( getUseSystemClassLoader().booleanValue() );
            fork.setUseManifestOnlyJar( isUseManifestOnlyJar() );

            fork.setSystemProperties( getInternalSystemProperties() );

            if ( "true".equals( getDebugForkedProcess() ) )
            {
                setDebugForkedProcess(
                    "-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005" );
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

    protected abstract String[] getDefaultIncludes();

    /**
     * Generate the test classpath.
     *
     * @return List containing the classpath elements
     * @throws org.apache.maven.artifact.DependencyResolutionRequiredException
     *          when dependency resolution fails
     * @throws org.apache.maven.plugin.MojoExecutionException
     *          upon other problems
     */
    public List generateTestClasspath()
        throws DependencyResolutionRequiredException, MojoExecutionException
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
                classpath.add( classpathElement );
            }
        }

        return classpath;
    }

    /**
     * Return a new set containing only the artifacts accepted by the given filter.
     *
     * @param artifacts The unfiltered artifacts
     * @param filter    The filter to apply
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
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        ArtifactFilter filter = null;
        if ( filteredArtifact != null )
        {
            filter = new ExcludesArtifactFilter(
                Collections.singletonList( filteredArtifact.getGroupId() + ":" + filteredArtifact.getArtifactId() ) );
        }

        Artifact originatingArtifact = getArtifactFactory().createBuildArtifact( "dummy", "dummy", "1.0", "jar" );

        return getArtifactResolver().resolveTransitively( Collections.singleton( providerArtifact ),
                                                          originatingArtifact, getLocalRepository(),
                                                          getRemoteRepositories(), getMetadataSource(), filter );
    }

    private void addArtifact( Classpath bootClasspath, Artifact surefireArtifact )
        throws ArtifactNotFoundException, ArtifactResolutionException
    {
        ArtifactResolutionResult result = resolveArtifact( null, surefireArtifact );

        for ( Iterator i = result.getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            getLog().debug(
                "Adding to " + getPluginName() + " booter test classpath: " + artifact.getFile().getAbsolutePath() +
                    " Scope: " + artifact.getScope() );

            bootClasspath.addClassPathElementUrl( artifact.getFile().getAbsolutePath() );
        }
    }

    protected void processSystemProperties( boolean setInSystem )
    {
        if ( this.getSystemProperties() != null )
        {
            for ( Iterator i = getSystemProperties().keySet().iterator(); i.hasNext(); )
            {
                String key = (String) i.next();
                String value = (String) getSystemProperties().get( key );
                getInternalSystemProperties().setProperty( key, value );
            }
        }

        if ( this.getSystemPropertyVariables() != null )
        {
            for ( Iterator i = getSystemPropertyVariables().keySet().iterator(); i.hasNext(); )
            {
                String key = (String) i.next();
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

        Properties userProperties = getUserProperties();
        for ( Iterator it = userProperties.keySet().iterator(); it.hasNext(); )
        {
            String key = (String) it.next();
            String value = userProperties.getProperty( key );
            getInternalSystemProperties().setProperty( key, value );
        }

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
            String msg = "Build uses Maven 2.0.x, cannot propagate system properties" +
                " from command line to tests (cf. SUREFIRE-121)";
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
        if ( isUseFile() )
        {
            if ( isPrintSummary() )
            {
                if ( forking )
                {
                    reports.add( ForkingConsoleReporter.class.getName() );
                }
                else
                {
                    reports.add( ConsoleReporter.class.getName() );
                }
            }

            if ( BRIEF_REPORT_FORMAT.equals( getReportFormat() ) )
            {
                reports.add( BriefFileReporter.class.getName() );
            }
            else if ( PLAIN_REPORT_FORMAT.equals( getReportFormat() ) )
            {
                reports.add( FileReporter.class.getName() );
            }
        }
        else
        {
            if ( BRIEF_REPORT_FORMAT.equals( getReportFormat() ) )
            {
                reports.add( BriefConsoleReporter.class.getName() );
            }
            else if ( PLAIN_REPORT_FORMAT.equals( getReportFormat() ) )
            {
                reports.add( DetailedConsoleReporter.class.getName() );
            }
        }

        if ( !isDisableXmlReport() )
        {
            reports.add( XMLReporter.class.getName() );
        }
        return reports;
    }

    protected void ensureWorkingDirectoryExists()
        throws MojoFailureException
    {
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
        if ( getUseSystemClassLoader() != null && isForkModeNever() )
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

        public void addProviderArtifactToBootClasspath( Classpath bootclasspath )
            throws ArtifactResolutionException, ArtifactNotFoundException
        {

            dependencyResolver.addResolvedArtifactToClasspath( bootclasspath, testNgArtifact );
        }

        public void addProviderArtifactToSurefireClasspath( ClasspathConfiguration bootclasspath )
            throws ArtifactResolutionException, ArtifactNotFoundException
        {
            Artifact surefireArtifact =
                (Artifact) getPluginArtifactMap().get( "org.apache.maven.surefire:surefire-booter" );
            dependencyResolver.addProviderToClasspath( bootclasspath, "surefire-testng",
                                                       surefireArtifact.getBaseVersion(), testNgArtifact );
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

        public void addProviderArtifactToBootClasspath( Classpath bootclasspath )
        {
        }

        public void addProviderArtifactToSurefireClasspath( ClasspathConfiguration classpathConfiguration )
            throws ArtifactResolutionException, ArtifactNotFoundException
        {
            // add the JUnit provider as default - it doesn't require JUnit to be present,
            // since it supports POJO tests.
            dependencyResolver.addProviderToClasspath( classpathConfiguration, "surefire-junit3",
                                                       surefireArtifact.getBaseVersion(), null );

        }

    }

    class JUnit4ProviderInfo
        implements ProviderInfo
    {
        private final Artifact junitArtifact;

        JUnit4ProviderInfo( Artifact junitArtifact )
        {
            this.junitArtifact = junitArtifact;
        }

        public String getProviderName()
        {
            return "org.apache.maven.surefire.junit4.JUnit4Provider";
        }

        public boolean isApplicable()
        {
            return isAnyJunit4( junitArtifact );
        }

        public void addProviderProperties()
        {
        }

        public void addProviderArtifactToBootClasspath( Classpath bootclasspath )
        {
        }

        public void addProviderArtifactToSurefireClasspath( ClasspathConfiguration classpathConfiguration )
            throws ArtifactResolutionException, ArtifactNotFoundException
        {
            dependencyResolver.addProviderToClasspath( classpathConfiguration, "surefire-junit4",
                                                       surefireArtifact.getBaseVersion(), null );

        }

    }

    class JUnitCoreProviderInfo
        implements ProviderInfo
    {
        private final Artifact junitArtifact;

        JUnitCoreProviderInfo( Artifact junitArtifact )
        {
            this.junitArtifact = junitArtifact;
        }

        public String getProviderName()
        {
            return "org.apache.maven.surefire.junitcore.JUnitCoreProvider";
        }

        public boolean isApplicable()
        {
            return isAnyJunit4( junitArtifact ) && isAnyConcurrencySelected() && isJunit47Compatible( junitArtifact );
        }

        public void addProviderProperties()
        {
            convertJunitCoreParameters();
        }

        public void addProviderArtifactToBootClasspath( Classpath bootclasspath )
        {
        }

        public void addProviderArtifactToSurefireClasspath( ClasspathConfiguration classpathConfiguration )
            throws ArtifactResolutionException, ArtifactNotFoundException
        {
            dependencyResolver.addProviderToClasspath( classpathConfiguration, "surefire-junit47",
                                                       surefireArtifact.getBaseVersion(), null );
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

        public ProviderInfo instantiate(String providerName){
            return new DynamicProviderInfo(providerName);
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

        public void addProviderArtifactToBootClasspath( Classpath bootclasspath )
        {
        }

        public void addProviderArtifactToSurefireClasspath( ClasspathConfiguration classpathConfiguration )
            throws ArtifactResolutionException, ArtifactNotFoundException
        {
            final Map pluginArtifactMap = getPluginArtifactMap();
            Artifact plugin = (Artifact) pluginArtifactMap.get( "org.apache.maven.plugins:maven-surefire-plugin");
            dependencyResolver.addProviderToClasspath( classpathConfiguration, pluginArtifactMap, plugin );
        }

    }

}
