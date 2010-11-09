package org.apache.maven.surefire.booter;

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

import org.apache.maven.surefire.Surefire;
import org.apache.maven.surefire.booter.output.FileOutputConsumerProxy;
import org.apache.maven.surefire.booter.output.ForkingStreamConsumer;
import org.apache.maven.surefire.booter.output.OutputConsumer;
import org.apache.maven.surefire.booter.output.StandardOutputConsumer;
import org.apache.maven.surefire.booter.output.SupressFooterOutputConsumerProxy;
import org.apache.maven.surefire.booter.output.SupressHeaderOutputConsumerProxy;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Jason van Zyl
 * @author Emmanuel Venisse
 * @version $Id$
 */
public class SurefireBooter
{

    private int forkedProcessTimeoutInSeconds = 0;

    private final BooterConfiguration booterConfiguration;

    private File reportsDirectory;

    public SurefireBooter( BooterConfiguration booterConfiguration, File reportsDirectory )
    {
        this.booterConfiguration = booterConfiguration;
        this.reportsDirectory = reportsDirectory;
    }

    protected SurefireBooter( BooterConfiguration booterConfiguration )
    {
        this.booterConfiguration = booterConfiguration;
    }

    public int run()
        throws SurefireBooterForkException, SurefireExecutionException
    {
        int result;

        final String requestedForkMode = booterConfiguration.getForkConfiguration().getForkMode();
        if ( ForkConfiguration.FORK_NEVER.equals( requestedForkMode ) )
        {
            result = runSuitesInProcess();
        }
        else if ( ForkConfiguration.FORK_ONCE.equals( requestedForkMode ) )
        {
            result = runSuitesForkOnce();
        }
        else if ( ForkConfiguration.FORK_ALWAYS.equals( requestedForkMode ) )
        {
            result = runSuitesForkPerTestSet();
        }
        else
        {
            throw new SurefireExecutionException( "Unknown forkmode: " + requestedForkMode, null );
        }
        return result;
    }

    private int runSuitesInProcess( String testSet, Properties results )
        throws SurefireExecutionException
    {
        if ( booterConfiguration.getTestSuites().size() != 1 )
        {
            throw new IllegalArgumentException( "Cannot only specify testSet for single test suites" );
        }

        // TODO: replace with plexus

        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            ClassLoader testsClassLoader = getClasspathConfiguration().createTestClassLoaderConditionallySystem(
                booterConfiguration.useSystemClassLoader() );

            // TODO: assertions = true shouldn't be required for this CL if we had proper separation (see TestNG)
            ClassLoader surefireClassLoader = getClasspathConfiguration().createSurefireClassLoader( testsClassLoader );

            SurefireReflector reflector = new SurefireReflector( surefireClassLoader );

            Thread.currentThread().setContextClassLoader( testsClassLoader );
            return reflector.run( booterConfiguration.getReports(),
                                  (Object[]) booterConfiguration.getTestSuites().get( 0 ), testSet, surefireClassLoader,
                                  testsClassLoader, results, booterConfiguration.isFailIfNoTests() );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( oldContextClassLoader );
        }
    }

    private int runSuitesInProcess()
        throws SurefireExecutionException
    {
        // TODO: replace with plexus

        // noinspection CatchGenericClass,OverlyBroadCatchBlock
        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();

        try
        {
            // The test classloader must be constructed first to avoid issues with commons-logging until we properly
            // separate the TestNG classloader
            ClassLoader testsClassLoader;
            String testClassPath = getClasspathConfiguration().getTestClassPathAsString();
            System.setProperty( "surefire.test.class.path", testClassPath );
            if ( booterConfiguration.getForkConfiguration().isManifestOnlyJarRequestedAndUsable() )
            {
                testsClassLoader = getClass().getClassLoader(); // ClassLoader.getSystemClassLoader()
                // SUREFIRE-459, trick the app under test into thinking its classpath was conventional
                // (instead of a single manifest-only jar) 
                System.setProperty( "surefire.real.class.path", System.getProperty( "java.class.path" ) );
                System.setProperty( "java.class.path", testClassPath );
            }
            else
            {
                testsClassLoader = getClasspathConfiguration().createTestClassLoader(  );
            }

            ClassLoader surefireClassLoader = getClasspathConfiguration().createSurefireClassLoader( testsClassLoader );

            SurefireReflector reflector = new SurefireReflector( surefireClassLoader );

            Thread.currentThread().setContextClassLoader( testsClassLoader );

            return reflector.run( booterConfiguration.getReports(), booterConfiguration.getTestSuites(),
                                  surefireClassLoader, testsClassLoader, booterConfiguration.isFailIfNoTests() );

        }
        finally
        {
            Thread.currentThread().setContextClassLoader( oldContextClassLoader );
        }
    }


    private int runSuitesForkOnce()
        throws SurefireBooterForkException
    {
        return forkSuites( booterConfiguration.getTestSuites(), true, true );
    }

    private int runSuitesForkPerTestSet()
        throws SurefireBooterForkException
    {
        ClassLoader testsClassLoader;
        ClassLoader surefireClassLoader;
        try
        {
            testsClassLoader = getClasspathConfiguration().createTestClassLoader( false );
            // TODO: assertions = true shouldn't be required if we had proper separation (see TestNG)
            surefireClassLoader = getClasspathConfiguration().createSurefireClassLoader( testsClassLoader );
        }
        catch ( SurefireExecutionException e )
        {
            throw new SurefireBooterForkException( "Unable to create classloader to find test suites", e );
        }

        int globalResult = 0;

        boolean showHeading = true;
        Properties properties = new Properties();
        for ( Iterator i = booterConfiguration.getTestSuites().iterator(); i.hasNext(); )
        {
            Object[] testSuite = (Object[]) i.next();

            Map testSets = getTestSets( testSuite, testsClassLoader, surefireClassLoader );

            for ( Iterator j = testSets.keySet().iterator(); j.hasNext(); )
            {
                Object testSet = j.next();
                boolean showFooter = !j.hasNext() && !i.hasNext();
                int result = forkSuite( testSuite, testSet, showHeading, showFooter, properties );
                if ( result > globalResult )
                {
                    globalResult = result;
                }
                showHeading = false;
            }
        }

        return globalResult;
    }

    private ClasspathConfiguration getClasspathConfiguration()
    {
        return booterConfiguration.getClasspathConfiguration();
    }

    private Map getTestSets( Object[] testSuite, ClassLoader testsClassLoader, ClassLoader surefireClassLoader )
        throws SurefireBooterForkException
    {
        String className = (String) testSuite[0];

        Object[] params = (Object[]) testSuite[1];

        Object suite;
        try
        {
            suite = Surefire.instantiateObject( className, params, surefireClassLoader );
        }
        catch ( TestSetFailedException e )
        {
            throw new SurefireBooterForkException( e.getMessage(), e.getCause() );
        }
        catch ( ClassNotFoundException e )
        {
            throw new SurefireBooterForkException( "Unable to find class for test suite '" + className + "'", e );
        }
        catch ( NoSuchMethodException e )
        {
            throw new SurefireBooterForkException(
                "Unable to find appropriate constructor for test suite '" + className + "': " + e.getMessage(), e );
        }

        Map testSets;
        try
        {
            Method m = suite.getClass().getMethod( "locateTestSets", new Class[]{ ClassLoader.class } );

            testSets = (Map) m.invoke( suite, new Object[]{ testsClassLoader } );
        }
        catch ( IllegalAccessException e )
        {
            throw new SurefireBooterForkException( "Error obtaining test sets", e );
        }
        catch ( NoSuchMethodException e )
        {
            throw new SurefireBooterForkException( "Error obtaining test sets", e );
        }
        catch ( InvocationTargetException e )
        {
            throw new SurefireBooterForkException( e.getTargetException().getMessage(), e.getTargetException() );
        }
        return testSets;
    }

    private int forkSuites( List testSuites, boolean showHeading, boolean showFooter )
        throws SurefireBooterForkException
    {
        Properties properties = new Properties();

        booterConfiguration.setForkProperties( testSuites, properties );

        return fork( properties, showHeading, showFooter );
    }

    private int forkSuite( Object[] testSuite, Object testSet, boolean showHeading, boolean showFooter,
                           Properties properties )
        throws SurefireBooterForkException
    {
        booterConfiguration.setForkProperties( Collections.singletonList( testSuite ), properties );

        if ( testSet instanceof String )
        {
            properties.setProperty( "testSet", (String) testSet );
        }

        return fork( properties, showHeading, showFooter );
    }

    private int fork( Properties properties, boolean showHeading, boolean showFooter )
        throws SurefireBooterForkException
    {
        File surefireProperties;
        File systemProperties = null;
        try
        {
            surefireProperties = booterConfiguration.writePropertiesFile( "surefire", properties );
            if ( booterConfiguration.getForkConfiguration().getSystemProperties() != null )
            {
                systemProperties = booterConfiguration.writePropertiesFile( "surefire",
                                                                            booterConfiguration.getForkConfiguration().getSystemProperties() );
            }
        }
        catch ( IOException e )
        {
            throw new SurefireBooterForkException( "Error creating properties files for forking", e );
        }

        List bootClasspath = getClasspathConfiguration().getBootClasspath( booterConfiguration.useSystemClassLoader() );

        Commandline cli = booterConfiguration.getForkConfiguration().createCommandLine( bootClasspath );

        cli.createArg().setFile( surefireProperties );

        if ( systemProperties != null )
        {
            cli.createArg().setFile( systemProperties );
        }

        ForkingStreamConsumer out =
            getForkingStreamConsumer( showHeading, showFooter, booterConfiguration.isRedirectTestOutputToFile() );

        StreamConsumer err;

        if ( booterConfiguration.isRedirectTestOutputToFile() )
        {
            err = out;
        }
        else
        {
            err = getForkingStreamConsumer( showHeading, showFooter, booterConfiguration.isRedirectTestOutputToFile() );
        }

        if ( booterConfiguration.getForkConfiguration().isDebug() )
        {
            System.out.println( "Forking command line: " + cli );
        }

        int returnCode;

        try
        {
            returnCode = CommandLineUtils.executeCommandLine( cli, out, err, forkedProcessTimeoutInSeconds );
        }
        catch ( CommandLineException e )
        {
            throw new SurefireBooterForkException( "Error while executing forked tests.", e );
        }

        if ( booterConfiguration.isRedirectTestOutputToFile() )
        {
            // ensure the FileOutputConsumerProxy flushes/closes the output file
            try
            {
                out.getOutputConsumer().testSetCompleted();
            }
            catch ( Exception e )
            {
                // the FileOutputConsumerProxy might throw an IllegalStateException but that's not of interest now
            }
        }

        if ( surefireProperties != null && surefireProperties.exists() )
        {
            FileInputStream inStream = null;
            try
            {
                inStream = new FileInputStream( surefireProperties );

                properties.load( inStream );
            }
            catch ( FileNotFoundException e )
            {
                throw new SurefireBooterForkException( "Unable to reload properties file from forked process", e );
            }
            catch ( IOException e )
            {
                throw new SurefireBooterForkException( "Unable to reload properties file from forked process", e );
            }
            finally
            {
                IOUtil.close( inStream );
            }
        }

        return returnCode;
    }

    private static Properties loadProperties( File file )
        throws IOException
    {
        Properties p = new Properties();

        if ( file != null && file.exists() )
        {
            FileInputStream inStream = new FileInputStream( file );
            try
            {
                p.load( inStream );
            }
            finally
            {
                IOUtil.close( inStream );
            }
        }

        return p;
    }

    private static void setSystemProperties( File file )
        throws IOException
    {
        Properties p = loadProperties( file );

        for ( Iterator i = p.keySet().iterator(); i.hasNext(); )
        {
            String key = (String) i.next();

            System.setProperty( key, p.getProperty( key ) );
        }
    }

    /**
     * This method is invoked when Surefire is forked - this method parses and organizes the arguments passed to it and
     * then calls the Surefire class' run method. <p/> The system exit code will be 1 if an exception is thrown.
     *
     * @param args Commandline arguments
     * @throws Throwable Upon throwables
     */
    public static void main( String[] args )
        throws Throwable
    {
        try
        {
            if ( args.length > 1 )
            {
                setSystemProperties( new File( args[1] ) );
            }

            File surefirePropertiesFile = new File( args[0] );
            InputStream stream = surefirePropertiesFile.exists() ? new FileInputStream( surefirePropertiesFile ) : null;
            BooterConfiguration booterConfiguration = new BooterConfiguration( stream );
            Properties p = booterConfiguration.getProperties();

            SurefireBooter booter = new SurefireBooter( booterConfiguration );

            String testSet = p.getProperty( "testSet" );
            int result;
            if ( testSet != null )
            {
                result = booter.runSuitesInProcess( testSet, p );
            }
            else
            {
                result = booter.runSuitesInProcess();
            }

            booterConfiguration.writePropertiesFile( surefirePropertiesFile, "surefire", p );

            // noinspection CallToSystemExit
            System.exit( result );
        }
        catch ( Throwable t )
        {
            // Just throwing does getMessage() and a local trace - we want to call printStackTrace for a full trace
            // noinspection UseOfSystemOutOrSystemErr
            t.printStackTrace( System.err );
            // noinspection ProhibitedExceptionThrown,CallToSystemExit
            System.exit( 1 );
        }
    }

    private ForkingStreamConsumer getForkingStreamConsumer( boolean showHeading, boolean showFooter,
                                                            boolean redirectTestOutputToFile )
    {
        OutputConsumer outputConsumer = new StandardOutputConsumer();

        if ( redirectTestOutputToFile )
        {
            outputConsumer = new FileOutputConsumerProxy( outputConsumer, reportsDirectory );
        }

        if ( !showHeading )
        {
            outputConsumer = new SupressHeaderOutputConsumerProxy( outputConsumer );
        }
        if ( !showFooter )
        {
            outputConsumer = new SupressFooterOutputConsumerProxy( outputConsumer );
        }

        return new ForkingStreamConsumer( outputConsumer );
    }

    public void setForkedProcessTimeoutInSeconds( int forkedProcessTimeoutInSeconds )
    {
        this.forkedProcessTimeoutInSeconds = forkedProcessTimeoutInSeconds;
    }
}
