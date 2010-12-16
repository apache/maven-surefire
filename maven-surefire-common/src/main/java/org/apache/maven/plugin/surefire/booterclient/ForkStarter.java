package org.apache.maven.plugin.surefire.booterclient;

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

import org.apache.maven.plugin.surefire.booterclient.output.FileOutputConsumerProxy;
import org.apache.maven.plugin.surefire.booterclient.output.OutputConsumer;
import org.apache.maven.plugin.surefire.booterclient.output.StandardOutputConsumer;
import org.apache.maven.plugin.surefire.booterclient.output.SupressFooterOutputConsumerProxy;
import org.apache.maven.plugin.surefire.booterclient.output.SupressHeaderOutputConsumerProxy;
import org.apache.maven.plugin.surefire.booterclient.output.SynchronizedOutputConsumer;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.ProviderConfiguration;
import org.apache.maven.surefire.booter.ProviderFactory;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.SurefireBooterForkException;
import org.apache.maven.surefire.booter.SurefireExecutionException;
import org.apache.maven.surefire.booter.SurefireStarter;
import org.apache.maven.surefire.booter.SystemPropertyManager;
import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.apache.maven.surefire.suite.RunResult;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;


/**
 * Starts the fork or runs in-process.
 * <p/>
 * Lives only on the plugin-side (not present in remote vms)
 * <p/>
 * Knows how to fork new vms and also how to delegate non-forking invocation to SurefireStarter directly
 *
 * @author Jason van Zyl
 * @author Emmanuel Venisse
 * @author Brett Porter
 * @author Dan Fabulich
 * @author Carlos Sanchez
 * @author Kristian Rosenvold
 * @version $Id$
 */
public class ForkStarter
{
    private final int forkedProcessTimeoutInSeconds;

    private final ProviderConfiguration providerConfiguration;

    private final StartupConfiguration startupConfiguration;

    private final ForkConfiguration forkConfiguration;

    private final File reportsDirectory;

    public ForkStarter( ProviderConfiguration providerConfiguration, StartupConfiguration startupConfiguration,
                        File reportsDirectory, ForkConfiguration forkConfiguration, int forkedProcessTimeoutInSeconds )
    {
        this.forkConfiguration = forkConfiguration;
        this.providerConfiguration = providerConfiguration;
        this.reportsDirectory = reportsDirectory;
        this.forkedProcessTimeoutInSeconds = forkedProcessTimeoutInSeconds;
        this.startupConfiguration = startupConfiguration;
    }

    public int run()
        throws SurefireBooterForkException, SurefireExecutionException
    {
        final int result;

        final String requestedForkMode = forkConfiguration.getForkMode();
        if ( ForkConfiguration.FORK_NEVER.equals( requestedForkMode ) )
        {
            SurefireStarter surefireStarter = new SurefireStarter( startupConfiguration, providerConfiguration );
            RunResult runResult = surefireStarter.runSuitesInProcess();
            result = surefireStarter.processRunCount( runResult );
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
        return fork( null, providerConfiguration.getProviderProperties(), true, true );
    }

    private int runSuitesForkPerTestSet()
        throws SurefireBooterForkException
    {
        int globalResult = 0;

        ClassLoader testsClassLoader;
        ClassLoader surefireClassLoader;
        try
        {
            testsClassLoader = startupConfiguration.getClasspathConfiguration().createTestClassLoader( false );
            // TODO: assertions = true shouldn't be required if we had proper separation (see TestNG)
            surefireClassLoader =
                startupConfiguration.getClasspathConfiguration().createSurefireClassLoader( testsClassLoader );
        }
        catch ( SurefireExecutionException e )
        {
            throw new SurefireBooterForkException( "Unable to create classloader to find test suites", e );
        }

        boolean showHeading = true;
        final ProviderFactory providerFactory =
            new ProviderFactory( startupConfiguration, providerConfiguration, surefireClassLoader );
        SurefireProvider surefireProvider = providerFactory.createProvider( testsClassLoader );

        Properties properties = new Properties();

        final Iterator suites = surefireProvider.getSuites();
        while ( suites.hasNext() )
        {
            Object testSet = suites.next();
            boolean showFooter = !suites.hasNext();
            int result = fork( testSet, properties, showHeading, showFooter );

            if ( result > globalResult )
            {
                globalResult = result;
            }
            showHeading = false;
        }
        // At this place, show aggregated results ?
        return globalResult;
    }

    private int fork( Object testSet, Properties properties, boolean showHeading, boolean showFooter )
        throws SurefireBooterForkException
    {
        File surefireProperties;
        File systemProperties = null;
        try
        {
            BooterSerializer booterSerializer = new BooterSerializer( forkConfiguration, properties );

            surefireProperties = booterSerializer.serialize( providerConfiguration, startupConfiguration, testSet );

            if ( forkConfiguration.getSystemProperties() != null )
            {
                systemProperties = SystemPropertyManager.writePropertiesFile( forkConfiguration.getSystemProperties(),
                                                                              forkConfiguration.getTempDirectory(),
                                                                              "surefire", forkConfiguration.isDebug() );
            }
        }
        catch ( IOException e )
        {
            throw new SurefireBooterForkException( "Error creating properties files for forking", e );
        }

        final Classpath bootClasspathConfiguration = forkConfiguration.getBootClasspath();
        final Classpath additionlClassPathUrls = startupConfiguration.useSystemClassLoader()
            ? startupConfiguration.getClasspathConfiguration().getTestClasspath()
            : null;

        Classpath bootClasspath = bootClasspathConfiguration.append( additionlClassPathUrls );

        Commandline cli = forkConfiguration.createCommandLine( bootClasspath.getClassPath(),
                                                               startupConfiguration.getClassLoaderConfiguration() );

        cli.createArg().setFile( surefireProperties );

        if ( systemProperties != null )
        {
            cli.createArg().setFile( systemProperties );
        }

        final boolean willBeSharingConsumer = startupConfiguration.isRedirectTestOutputToFile();

        ForkingStreamConsumer out =
            getForkingStreamConsumer( showHeading, showFooter, startupConfiguration.isRedirectTestOutputToFile(),
                                      willBeSharingConsumer );

        StreamConsumer err = willBeSharingConsumer
            ? out
            : getForkingStreamConsumer( showHeading, showFooter, startupConfiguration.isRedirectTestOutputToFile(),
                                        false );

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
            throw new SurefireBooterForkException( "Error while executing forked tests.", e.getCause() );
        }

        if ( startupConfiguration.isRedirectTestOutputToFile() )
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
                                                            boolean redirectTestOutputToFile, boolean mustBeThreadSafe )
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

        if ( mustBeThreadSafe )
        {
            outputConsumer = new SynchronizedOutputConsumer( outputConsumer );
        }

        return new ForkingStreamConsumer( outputConsumer );
    }
}
