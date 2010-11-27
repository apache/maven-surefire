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

import org.apache.maven.surefire.booter.BooterConfiguration;
import org.apache.maven.surefire.booter.ClasspathConfiguration;
import org.apache.maven.surefire.booter.SurefireBooterForkException;
import org.apache.maven.surefire.booter.SurefireExecutionException;
import org.apache.maven.surefire.booter.SurefireReflector;
import org.apache.maven.surefire.booter.SurefireStarter;
import org.apache.maven.plugin.surefire.booterclient.output.FileOutputConsumerProxy;
import org.apache.maven.plugin.surefire.booterclient.output.OutputConsumer;
import org.apache.maven.plugin.surefire.booterclient.output.StandardOutputConsumer;
import org.apache.maven.plugin.surefire.booterclient.output.SupressFooterOutputConsumerProxy;
import org.apache.maven.plugin.surefire.booterclient.output.SupressHeaderOutputConsumerProxy;
import org.apache.maven.surefire.booter.*;
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
import java.util.List;
import java.util.Properties;


/**
 * Starts the fork or runs in-process.
 * <p/>
 * Lives only on the plugin-side (not present in remote vms)
 * <p/>
 * Knows how to fork new vms and also how to delegate non-forking invocation to TestVmBooter directly
 *
 * @author Jason van Zyl
 * @author Emmanuel Venisse
 * @author Kristian Rosenvold
 * @version $Id$
 */
public class ForkStarter
{
    private int forkedProcessTimeoutInSeconds = 0;

    private final BooterConfiguration booterConfiguration;


    private final ForkConfiguration forkConfiguration;

    private File reportsDirectory;

    public ForkStarter( BooterConfiguration booterConfiguration, File reportsDirectory,
                        ForkConfiguration forkConfiguration )
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
            SurefireStarter testVmBooter = new SurefireStarter( booterConfiguration );
            result = testVmBooter.runSuitesInProcess(booterConfiguration.getProviderProperties());
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
        return forkSuites( true, true );
    }

    private int runSuitesForkPerTestSet()
        throws SurefireBooterForkException
    {
        int globalResult = 0;

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


        boolean showHeading = true;
        final ProviderFactory providerFactory = new ProviderFactory( booterConfiguration, surefireClassLoader);
        Object surefireProvider = providerFactory.createProvider(testsClassLoader);

        Properties properties = new Properties();

        final Iterator suites = (Iterator) SurefireReflector.getSuites(surefireProvider);
        while ( suites.hasNext() )
        {
            Object testSet = suites.next();
            boolean showFooter = !suites.hasNext();
            int result = forkSuite( testSet, showHeading, showFooter, properties );
            if ( result > globalResult )
            {
                globalResult = result;
            }
            showHeading = false;
        }

        return globalResult;
    }

    private ClasspathConfiguration getClasspathConfiguration()
    {
        return booterConfiguration.getClasspathConfiguration();
    }

    private int forkSuites( boolean showHeading, boolean showFooter )
        throws SurefireBooterForkException
    {
        Properties properties = booterConfiguration.getProviderProperties();

        BooterSerializer booterSerializer = new BooterSerializer();
        booterSerializer.setForkProperties( properties, booterConfiguration,
                                            forkConfiguration.getClassLoaderConfiguration() );

        return fork( properties, showHeading, showFooter );
    }

    private int forkSuite( Object testSet, boolean showHeading, boolean showFooter, Properties properties )
        throws SurefireBooterForkException
    {
        BooterSerializer booterSerializer = new BooterSerializer();
        booterSerializer.setForkProperties( properties, booterConfiguration,
                                            forkConfiguration.getClassLoaderConfiguration() );

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
            surefireProperties =
                booterSerializer.writePropertiesFile( "surefire", properties, forkConfiguration.isDebug(),
                                                      forkConfiguration.getTempDirectory() );
            if ( forkConfiguration.getSystemProperties() != null )
            {
                systemProperties =
                    booterSerializer.writePropertiesFile( "surefire", forkConfiguration.getSystemProperties(),
                                                          forkConfiguration.isDebug(),
                                                          forkConfiguration.getTempDirectory() );
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
