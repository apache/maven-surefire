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

import org.apache.maven.plugin.surefire.JdkAttributes;
import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.Commandline;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.booter.AbstractPathConfiguration;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.SurefireBooterForkException;
import org.apache.maven.surefire.extensions.ForkNodeFactory;
import org.apache.maven.surefire.api.util.internal.ImmutableMap;
import org.apache.maven.surefire.shared.utils.cli.CommandLineException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import static org.apache.maven.plugin.surefire.SurefireHelper.replaceForkThreadsInPath;
import static org.apache.maven.plugin.surefire.util.Relocator.relocate;
import static org.apache.maven.plugin.surefire.SurefireHelper.replaceThreadNumberPlaceholders;
import static org.apache.maven.surefire.booter.Classpath.join;

/**
 * Basic framework which constructs CLI.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.21.0.Jigsaw
 */
public abstract class DefaultForkConfiguration
        extends ForkConfiguration
{
    @Nonnull private final Classpath booterClasspath;
    @Nonnull private final File tempDirectory;
    @Nullable
    private final String debugLine;
    @Nonnull private final File workingDirectory;
    @Nonnull private final Properties modelProperties;
    @Nullable private final String argLine;
    @Nonnull private final Map<String, String> environmentVariables;
    @Nonnull private final String[] excludedEnvironmentVariables;
    private final boolean debug;
    private final int forkCount;
    private final boolean reuseForks;
    @Nonnull private final Platform pluginPlatform;
    @Nonnull private final ConsoleLogger log;
    @Nonnull private final ForkNodeFactory forkNodeFactory;

    @SuppressWarnings( "checkstyle:parameternumber" )
    protected DefaultForkConfiguration( @Nonnull Classpath booterClasspath,
                                     @Nonnull File tempDirectory,
                                     @Nullable String debugLine,
                                     @Nonnull File workingDirectory,
                                     @Nonnull Properties modelProperties,
                                     @Nullable String argLine,
                                     @Nonnull Map<String, String> environmentVariables,
                                     @Nonnull String[] excludedEnvironmentVariables,
                                     boolean debug,
                                     int forkCount,
                                     boolean reuseForks,
                                     @Nonnull Platform pluginPlatform,
                                     @Nonnull ConsoleLogger log,
                                     @Nonnull ForkNodeFactory forkNodeFactory )
    {
        this.booterClasspath = booterClasspath;
        this.tempDirectory = tempDirectory;
        this.debugLine = debugLine;
        this.workingDirectory = workingDirectory;
        this.modelProperties = modelProperties;
        this.argLine = argLine;
        this.environmentVariables = toImmutable( environmentVariables );
        this.excludedEnvironmentVariables = excludedEnvironmentVariables;
        this.debug = debug;
        this.forkCount = forkCount;
        this.reuseForks = reuseForks;
        this.pluginPlatform = pluginPlatform;
        this.log = log;
        this.forkNodeFactory = forkNodeFactory;
    }

    protected abstract void resolveClasspath( @Nonnull Commandline cli,
                                              @Nonnull String booterThatHasMainMethod,
                                              @Nonnull StartupConfiguration config,
                                              @Nonnull File dumpLogDirectory )
            throws SurefireBooterForkException;

    @Nonnull
    protected String extendJvmArgLine( @Nonnull String jvmArgLine )
    {
        return jvmArgLine;
    }

    @Nonnull
    @Override
    public final ForkNodeFactory getForkNodeFactory()
    {
        return forkNodeFactory;
    }

    /**
     * @param config       The startup configuration
     * @param forkNumber   index of forked JVM, to be the replacement in the argLine
     * @param dumpLogDirectory     directory for dump log file
     * @return CommandLine able to flush entire command going to be sent to forked JVM
     * @throws org.apache.maven.surefire.booter.SurefireBooterForkException when unable to perform the fork
     */
    @Nonnull
    @Override
    public Commandline createCommandLine( @Nonnull StartupConfiguration config,
                                          int forkNumber,
                                          @Nonnull File dumpLogDirectory )
            throws SurefireBooterForkException
    {
        try
        {
            Commandline cli =
                    new Commandline( getExcludedEnvironmentVariables() );

            cli.setWorkingDirectory( getWorkingDirectory( forkNumber ).getAbsolutePath() );

            for ( Entry<String, String> entry : getEnvironmentVariables().entrySet() )
            {
                String value = entry.getValue();
                cli.addEnvironment( entry.getKey(), value == null ? "" : value );
            }

            cli.setExecutable( getJdkForTests().getJvmExecutable().getAbsolutePath() );

            String jvmArgLine = newJvmArgLine( forkNumber );
            if ( !jvmArgLine.isEmpty() )
            {
                cli.createArg()
                        .setLine( jvmArgLine );
            }

            if ( getDebugLine() != null && !getDebugLine().isEmpty() )
            {
                cli.createArg()
                        .setLine( getDebugLine() );
            }

            resolveClasspath( cli, findStartClass( config ), config, dumpLogDirectory );

            return cli;
        }
        catch ( CommandLineException e )
        {
            throw new SurefireBooterForkException( e.getLocalizedMessage(), e );
        }
    }

    protected ConsoleLogger getLogger()
    {
        return log;
    }

    @Nonnull
    protected List<String> toCompleteClasspath( @Nonnull StartupConfiguration conf ) throws SurefireBooterForkException
    {
        AbstractPathConfiguration pathConfig = conf.getClasspathConfiguration();
        if ( pathConfig.isClassPathConfig() == pathConfig.isModularPathConfig() )
        {
            throw new SurefireBooterForkException( "Could not find class-path config nor modular class-path either." );
        }

        Classpath bootClasspath = getBooterClasspath();
        Classpath testClasspath = pathConfig.getTestClasspath();
        Classpath providerClasspath = pathConfig.getProviderClasspath();
        Classpath completeClasspath = join( join( bootClasspath, testClasspath ), providerClasspath );

        getLogger().debug( completeClasspath.getLogMessage( "boot classpath:" ) );
        getLogger().debug( completeClasspath.getCompactLogMessage( "boot(compact) classpath:" ) );

        return completeClasspath.getClassPath();
    }

    @Nonnull
    private File getWorkingDirectory( int forkNumber )
            throws SurefireBooterForkException
    {
        File cwd = replaceForkThreadsInPath( getWorkingDirectory(), forkNumber );

        if ( !cwd.exists() && !cwd.mkdirs() )
        {
            throw new SurefireBooterForkException( "Cannot create workingDirectory " + cwd.getAbsolutePath() );
        }

        if ( !cwd.isDirectory() )
        {
            throw new SurefireBooterForkException(
                    "WorkingDirectory " + cwd.getAbsolutePath() + " exists and is not a directory" );
        }
        return cwd;
    }

    /**
     * Replaces expressions <pre>@{property-name}</pre> with the corresponding properties
     * from the model. This allows late evaluation of property values when the plugin is executed (as compared
     * to evaluation when the pom is parsed as is done with <pre>${property-name}</pre> expressions).
     *
     * This allows other plugins to modify or set properties with the changes getting picked up by surefire.
     */
    @Nonnull
    private String interpolateArgLineWithPropertyExpressions()
    {
        if ( getArgLine() == null )
        {
            return "";
        }

        String resolvedArgLine = getArgLine().trim();

        if ( resolvedArgLine.isEmpty() )
        {
            return "";
        }

        for ( final String key : getModelProperties().stringPropertyNames() )
        {
            String field = "@{" + key + "}";
            if ( getArgLine().contains( field ) )
            {
                resolvedArgLine = resolvedArgLine.replace( field, getModelProperties().getProperty( key, "" ) );
            }
        }

        return resolvedArgLine;
    }

    @Nonnull
    private static String stripNewLines( @Nonnull String argLine )
    {
        return argLine.replace( "\n", " " ).replace( "\r", " " );
    }

    /**
     * Immutable map.
     *
     * @param map    immutable map copies elements from <code>map</code>
     * @param <K>    key type
     * @param <V>    value type
     * @return never returns null
     */
    @Nonnull
    private static <K, V> Map<K, V> toImmutable( @Nullable Map<K, V> map )
    {
        return map == null ? Collections.<K, V>emptyMap() : new ImmutableMap<>( map );
    }

    @Override
    @Nonnull
    public File getTempDirectory()
    {
        return tempDirectory;
    }

    @Override
    @Nullable
    protected String getDebugLine()
    {
        return debugLine;
    }

    @Override
    @Nonnull
    protected File getWorkingDirectory()
    {
        return workingDirectory;
    }

    @Override
    @Nonnull
    protected Properties getModelProperties()
    {
        return modelProperties;
    }

    @Override
    @Nullable
    protected String getArgLine()
    {
        return argLine;
    }

    @Override
    @Nonnull
    protected Map<String, String> getEnvironmentVariables()
    {
        return environmentVariables;
    }

    @Nonnull
    @Override
    protected String[] getExcludedEnvironmentVariables()
    {
        return excludedEnvironmentVariables;
    }

    @Override
    protected boolean isDebug()
    {
        return debug;
    }

    @Override
    protected int getForkCount()
    {
        return forkCount;
    }

    @Override
    protected boolean isReuseForks()
    {
        return reuseForks;
    }

    @Override
    @Nonnull
    protected Platform getPluginPlatform()
    {
        return pluginPlatform;
    }

    @Override
    @Nonnull
    protected JdkAttributes getJdkForTests()
    {
        return getPluginPlatform().getJdkExecAttributesForTests();
    }

    @Override
    @Nonnull
    protected Classpath getBooterClasspath()
    {
        return booterClasspath;
    }

    @Nonnull
    private String newJvmArgLine( int forks )
    {
        String interpolatedArgs = stripNewLines( interpolateArgLineWithPropertyExpressions() );
        String argsWithReplacedForkNumbers = replaceThreadNumberPlaceholders( interpolatedArgs, forks );
        return extendJvmArgLine( argsWithReplacedForkNumbers );
    }

    @Nonnull
    private static String findStartClass( StartupConfiguration config )
    {
        return config.isShadefire() ? relocate( DEFAULT_PROVIDER_CLASS ) : DEFAULT_PROVIDER_CLASS;
    }
}
