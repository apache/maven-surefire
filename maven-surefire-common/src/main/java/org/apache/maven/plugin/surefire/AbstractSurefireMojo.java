package org.apache.maven.plugin.surefire;

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
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.artifact.filter.PatternIncludesArtifactFilter;
import org.apache.maven.surefire.booter.ForkConfiguration;
import org.apache.maven.surefire.booter.SurefireBooter;
import org.apache.maven.surefire.report.BriefConsoleReporter;
import org.apache.maven.surefire.report.BriefFileReporter;
import org.apache.maven.surefire.report.ConsoleReporter;
import org.apache.maven.surefire.report.DetailedConsoleReporter;
import org.apache.maven.surefire.report.FileReporter;
import org.apache.maven.surefire.report.ForkingConsoleReporter;
import org.apache.maven.surefire.report.XMLReporter;
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
    extends AbstractMojo implements SurefireExecutionParameters
{

    private static final String BRIEF_REPORT_FORMAT = "brief";

    private static final String PLAIN_REPORT_FORMAT = "plain";

    // common field getters/setters

    // common code

    protected abstract String getPluginName();

    protected abstract boolean verifyParameters()
        throws MojoFailureException;

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
        if ( getProperties() == null )
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
        Artifact configurableParallelComputer =
            (Artifact) getProjectArtifactMap().get( "org.jdogma.junit:configurable-parallel-computer" );
        getProperties().setProperty( "configurableParallelComputerPresent",
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

    protected boolean isForkModeNever()
    {
        return ForkConfiguration.FORK_NEVER.equals( getForkMode()); 
    }

    protected SurefireBooter constructSurefireBooter()
        throws MojoExecutionException, MojoFailureException
    {
        SurefireBooter surefireBooter = new SurefireBooter();

        Artifact surefireArtifact = (Artifact) getPluginArtifactMap().get( "org.apache.maven.surefire:surefire-booter" );
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

            junitArtifact = (Artifact) getProjectArtifactMap().get( getJunitArtifactName() );
            // SUREFIRE-378, junit can have an alternate artifact name
            if ( junitArtifact == null && "junit:junit".equals( getJunitArtifactName() ) )
            {
                junitArtifact = (Artifact) getProjectArtifactMap().get( "junit:junit-dep" );
            }

            // TODO: this is pretty manual, but I'd rather not require the plugin > dependencies section right now
            testNgArtifact = (Artifact) getProjectArtifactMap().get( getTestNGArtifactName() );

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

                if ( this.getTestClassesDirectory() != null )
                {
                    getProperties().setProperty( "testng.test.classpath", getTestClassesDirectory().getAbsolutePath() );
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

        if ( getSuiteXmlFiles() != null && getSuiteXmlFiles().length > 0 && getTest() == null )
        {
            if ( testNgArtifact == null )
            {
                throw new MojoExecutionException( "suiteXmlFiles is configured, but there is no TestNG dependency" );
            }

            // TODO: properties should be passed in here too
            surefireBooter.addTestSuite( "org.apache.maven.surefire.testng.TestNGXmlTestSuite",
                                         new Object[]{ getSuiteXmlFiles(), getTestSourceDirectory().getAbsolutePath(),
                                             testNgArtifact.getVersion(), testNgArtifact.getClassifier(),
                                             getProperties(), getReportsDirectory() } );
        }
        else
        {
            List includes;
            List excludes;

            if ( getTest() != null )
            {
                // Check to see if we are running a single test. The raw parameter will
                // come through if it has not been set.

                // FooTest -> **/FooTest.java

                includes = new ArrayList();

                excludes = new ArrayList();

                if ( getFailIfNoTests() == null )
                {
                    setFailIfNoTests( Boolean.TRUE );
                }

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

                excludes = this.getExcludes();

                // defaults here, qdox doesn't like the end javadoc value
                // Have to wrap in an ArrayList as surefire expects an ArrayList instead of a List for some reason
                if ( includes == null || includes.size() == 0 )
                {
                    includes = new ArrayList( Arrays.asList( getDefaultIncludes() ) );
                }
                if ( excludes == null || excludes.size() == 0 )
                {
                    excludes = new ArrayList( Arrays.asList( new String[]{"**/*$*"} ) );
                }
            }

            if ( testNgArtifact != null )
            {
                surefireBooter.addTestSuite( "org.apache.maven.surefire.testng.TestNGDirectoryTestSuite",
                                             new Object[]{ getTestClassesDirectory(), includes, excludes,
                                                 getTestSourceDirectory().getAbsolutePath(), testNgArtifact.getVersion(),
                                                 testNgArtifact.getClassifier(), getProperties(),
                                                 getReportsDirectory() } );
            }
            else
            {
                String junitDirectoryTestSuite;
                if ( isAnyConcurrencySelected() && isJunit47Compatible( junitArtifact ) )
                {
                    junitDirectoryTestSuite = "org.apache.maven.surefire.junitcore.JUnitCoreDirectoryTestSuite";
                    getLog().info( "Concurrency config is " + getProperties().toString() );
                    surefireBooter.addTestSuite( junitDirectoryTestSuite,
                                                 new Object[]{ getTestClassesDirectory(), includes, excludes,
                                                     getProperties() } );
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
                                                 new Object[]{ getTestClassesDirectory(), includes, excludes} );
                }
            }
        }

        List classpathElements = null;
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

            surefireBooter.addClassPathUrl( classpathElement );
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

        // ----------------------------------------------------------------------
        // Forking
        // ----------------------------------------------------------------------

        ForkConfiguration fork = new ForkConfiguration();

        fork.setForkMode( getForkMode() );

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

        surefireBooter.setFailIfNoTests( getFailIfNoTests() == null ? false : getFailIfNoTests().booleanValue() );

        surefireBooter.setForkedProcessTimeoutInSeconds( getForkedProcessTimeoutInSeconds() );

        surefireBooter.setRedirectTestOutputToFile( isRedirectTestOutputToFile() );

        surefireBooter.setForkConfiguration( fork );

        surefireBooter.setChildDelegation( isChildDelegation() );

        surefireBooter.setEnableAssertions( isEnableAssertions() );

        surefireBooter.setReportsDirectory( getReportsDirectory() );

        addReporters( surefireBooter, fork.isForking() );

        return surefireBooter;
    }

    protected abstract String[] getDefaultIncludes();

    /**
     * Generate the test classpath.
     *
     * @return List containing the classpath elements
     * @throws org.apache.maven.artifact.DependencyResolutionRequiredException
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
     * @param artifacts
     * @param filter
     */
    private Set filterArtifacts( Set artifacts, ArtifactFilter filter )
    {
        Set filteredArtifacts = new LinkedHashSet();

        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            if ( ! filter.include( artifact ) )
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

    private void addProvider( SurefireBooter surefireBooter, String provider, String version,
                              Artifact filteredArtifact )
        throws ArtifactNotFoundException, ArtifactResolutionException
    {
        Artifact providerArtifact = getArtifactFactory().createDependencyArtifact( "org.apache.maven.surefire", provider,
                                                                              VersionRange.createFromVersion( version ),
                                                                              "jar", null, Artifact.SCOPE_TEST );
        ArtifactResolutionResult result = resolveArtifact( filteredArtifact, providerArtifact );

        for ( Iterator i = result.getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            getLog().debug( "Adding to " + getPluginName() + " test classpath: " + artifact.getFile().getAbsolutePath() + " Scope: " + artifact.getScope() );

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

        Artifact originatingArtifact = getArtifactFactory().createBuildArtifact( "dummy", "dummy", "1.0", "jar" );

        return getArtifactResolver().resolveTransitively( Collections.singleton( providerArtifact ), originatingArtifact,
                                                          getLocalRepository(), getRemoteRepositories(),
                                                          getMetadataSource(), filter );
    }

    private void addArtifact( SurefireBooter surefireBooter, Artifact surefireArtifact )
        throws ArtifactNotFoundException, ArtifactResolutionException
    {
        ArtifactResolutionResult result = resolveArtifact( null, surefireArtifact );

        for ( Iterator i = result.getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            getLog().debug( "Adding to " + getPluginName() + " booter test classpath: " + artifact.getFile().getAbsolutePath() + " Scope: " + artifact.getScope() );

            surefireBooter.addSurefireBootClassPathUrl( artifact.getFile().getAbsolutePath() );
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
     * @param surefireBooter The surefire booter that will run tests.
     * @param forking
     */
    private void addReporters( SurefireBooter surefireBooter, boolean forking )
    {
        Boolean trimStackTrace = Boolean.valueOf( this.isTrimStackTrace() );
        if ( isUseFile() )
        {
            if ( isPrintSummary() )
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

            if ( BRIEF_REPORT_FORMAT.equals( getReportFormat() ) )
            {
                surefireBooter.addReport( BriefFileReporter.class.getName(),
                                          new Object[]{ getReportsDirectory(), trimStackTrace} );
            }
            else if ( PLAIN_REPORT_FORMAT.equals( getReportFormat() ) )
            {
                surefireBooter.addReport( FileReporter.class.getName(),
                                          new Object[]{ getReportsDirectory(), trimStackTrace} );
            }
        }
        else
        {
            if ( BRIEF_REPORT_FORMAT.equals( getReportFormat() ) )
            {
                surefireBooter.addReport( BriefConsoleReporter.class.getName(), new Object[]{trimStackTrace} );
            }
            else if ( PLAIN_REPORT_FORMAT.equals( getReportFormat() ) )
            {
                surefireBooter.addReport( DetailedConsoleReporter.class.getName(), new Object[]{trimStackTrace} );
            }
        }

        if ( !isDisableXmlReport() )
        {
            surefireBooter.addReport( XMLReporter.class.getName(), new Object[]{ getReportsDirectory(), trimStackTrace} );
        }
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
            throw new MojoFailureException( "workingDirectory " + getWorkingDirectory() + " exists and is not a directory" );
        }
    }

    protected void ensureParallelRunningCompatibility()
        throws MojoFailureException
    {
        if (isMavenParallel() && isForkModeNever())
        {
            throw new MojoFailureException( "parallel maven execution is not compatible with surefire forkmode NEVER");
        }
    }

    protected void warnIfUselessUseSystemClassLoaderParameter()
    {
        if ( getUseSystemClassLoader() != null && isForkModeNever() )
        {
            getLog().warn( "useSystemClassloader setting has no effect when not forking" );
        }
    }
}
