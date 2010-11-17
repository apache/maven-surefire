package org.apache.maven.plugin.surefire.booter;

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

import org.apache.maven.surefire.booter.BooterConfiguration;
import org.apache.maven.surefire.booter.BooterSerializer;
import org.apache.maven.surefire.booter.ClasspathConfiguration;
import org.apache.maven.surefire.booter.ForkConfiguration;
import org.apache.maven.surefire.booter.SurefireBooterForkException;
import org.apache.maven.surefire.booter.SurefireExecutionException;
import org.apache.maven.surefire.booter.SurefireReflector;
import org.apache.maven.surefire.booter.TestVmBooter;
import org.apache.maven.surefire.booter.output.FileOutputConsumerProxy;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * The part of the booter that lives only on the plugin-side (not present in remote vms)
 * <p/>
 * Knows how to fork new vms and also how to delegate non-forking invocation to TestVmBooter directly
 *
 * @author Jason van Zyl
 * @author Emmanuel Venisse
 * @author Kristian Rosenvold
 * @version $Id$
 */
public class PluginSideBooter
{

    private int forkedProcessTimeoutInSeconds = 0;

    private final BooterConfiguration booterConfiguration;

    private final PluginsideForkConfiguration forkConfiguration;

    private File reportsDirectory;


    public PluginSideBooter( BooterConfiguration booterConfiguration, File reportsDirectory,
                             PluginsideForkConfiguration forkConfiguration )
    {
        this.forkConfiguration = forkConfiguration;
        this.booterConfiguration = booterConfiguration;
        this.reportsDirectory = reportsDirectory;
    }

    public int run()
        throws SurefireBooterForkException, SurefireExecutionException
    {
        int result;

        final String requestedForkMode = forkConfiguration.getForkMode();
        if ( ForkConfiguration.FORK_NEVER.equals( requestedForkMode ) )
        {
            TestVmBooter testVmBooter = new TestVmBooter( booterConfiguration );
            result = testVmBooter.runSuitesInProcess();
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
            suite = SurefireReflector.instantiateObject( className, params, surefireClassLoader );
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

        BooterSerializer booterSerializer = new BooterSerializer();
        booterSerializer.setForkProperties( properties, testSuites, booterConfiguration,
                                            forkConfiguration.getBooterForkConfiguration() );

        return fork( properties, showHeading, showFooter );
    }

    private int forkSuite( Object[] testSuite, Object testSet, boolean showHeading, boolean showFooter,
                           Properties properties )
        throws SurefireBooterForkException
    {
        BooterSerializer booterSerializer = new BooterSerializer();
        booterSerializer.setForkProperties( properties, Collections.singletonList( testSuite ), booterConfiguration,
                                            forkConfiguration.getBooterForkConfiguration() );

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
            BooterSerializer booterSerializer = new BooterSerializer();
            surefireProperties = booterSerializer.writePropertiesFile( "surefire", properties,
                                                                       forkConfiguration.getBooterForkConfiguration() );
            if ( forkConfiguration.getSystemProperties() != null )
            {
                systemProperties =
                    booterSerializer.writePropertiesFile( "surefire", forkConfiguration.getSystemProperties(),
                                                          forkConfiguration.getBooterForkConfiguration() );
            }
        }
        catch ( IOException e )
        {
            throw new SurefireBooterForkException( "Error creating properties files for forking", e );
        }

        List bootClasspath = getClasspathConfiguration().getBootClasspath( booterConfiguration.useSystemClassLoader() );

        Commandline cli = forkConfiguration.createCommandLine( bootClasspath );

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

        if ( forkConfiguration.isDebug() )
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
