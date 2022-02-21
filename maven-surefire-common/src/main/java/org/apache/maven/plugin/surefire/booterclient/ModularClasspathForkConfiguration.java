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

import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.Commandline;
import org.apache.maven.plugin.surefire.booterclient.output.InPluginProcessDumpSingleton;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.booter.AbstractPathConfiguration;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.ModularClasspath;
import org.apache.maven.surefire.booter.ModularClasspathConfiguration;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.SurefireBooterForkException;
import org.apache.maven.surefire.extensions.ForkNodeFactory;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.io.File.createTempFile;
import static java.io.File.pathSeparatorChar;
import static org.apache.maven.plugin.surefire.SurefireHelper.escapeToPlatformPath;
import static org.apache.maven.surefire.api.util.internal.StringUtils.NL;
import static org.apache.maven.surefire.shared.utils.StringUtils.replace;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.21.0.Jigsaw
 */
public class ModularClasspathForkConfiguration
        extends DefaultForkConfiguration
{
    @SuppressWarnings( "checkstyle:parameternumber" )
    public ModularClasspathForkConfiguration( @Nonnull Classpath bootClasspath,
                                              @Nonnull File tempDirectory,
                                              @Nullable String debugLine,
                                              @Nonnull File workingDirectory,
                                              @Nonnull Properties modelProperties,
                                              @Nullable String argLine,
                                              @Nonnull Map<String, String> environmentVariables,
                                              @Nonnull String[] excludedEnvironmentVariables,
                                              boolean debug,
                                              @Nonnegative int forkCount,
                                              boolean reuseForks,
                                              @Nonnull Platform pluginPlatform,
                                              @Nonnull ConsoleLogger log,
                                              @Nonnull ForkNodeFactory forkNodeFactory )
    {
        super( bootClasspath, tempDirectory, debugLine, workingDirectory, modelProperties, argLine,
            environmentVariables, excludedEnvironmentVariables, debug, forkCount, reuseForks, pluginPlatform, log,
            forkNodeFactory );
    }

    @Override
    protected void resolveClasspath( @Nonnull Commandline cli, @Nonnull String startClass,
                                     @Nonnull StartupConfiguration config, @Nonnull File dumpLogDirectory )
            throws SurefireBooterForkException
    {
        try
        {
            AbstractPathConfiguration pathConfig = config.getClasspathConfiguration();

            ModularClasspathConfiguration modularClasspathConfiguration =
                    pathConfig.toRealPath( ModularClasspathConfiguration.class );

            ModularClasspath modularClasspath = modularClasspathConfiguration.getModularClasspath();

            boolean isMainDescriptor = modularClasspath.isMainDescriptor();
            String moduleName = modularClasspath.getModuleNameFromDescriptor();
            List<String> modulePath = modularClasspath.getModulePath();
            Collection<String> packages = modularClasspath.getPackages();
            File patchFile = modularClasspath.getPatchFile();
            List<String> classpath = toCompleteClasspath( config );

            File argsFile = createArgsFile( moduleName, modulePath, classpath, packages, patchFile, startClass,
                isMainDescriptor, config.getJpmsArguments() );

            cli.createArg().setValue( "@" + escapeToPlatformPath( argsFile.getAbsolutePath() ) );
        }
        catch ( IOException e )
        {
            String error = "Error creating args file";
            InPluginProcessDumpSingleton.getSingleton()
                    .dumpException( e, error, dumpLogDirectory );
            throw new SurefireBooterForkException( error, e );
        }
    }

    @Nonnull
    File createArgsFile( @Nonnull String moduleName, @Nonnull List<String> modulePath,
                         @Nonnull List<String> classPath, @Nonnull Collection<String> packages,
                         File patchFile, @Nonnull String startClassName, boolean isMainDescriptor,
                         @Nonnull List<String[]> providerJpmsArguments )
            throws IOException
    {
        File surefireArgs = createTempFile( "surefireargs", "", getTempDirectory() );
        if ( isDebug() )
        {
            getLogger().debug( "Path to args file: " +  surefireArgs.getCanonicalPath() );
        }
        else
        {
            surefireArgs.deleteOnExit();
        }

        try ( FileWriter io = new FileWriter( surefireArgs ) )
        {
            StringBuilder args = new StringBuilder( 64 * 1024 );
            if ( !modulePath.isEmpty() )
            {
                // https://docs.oracle.com/en/java/javase/11/tools/java.html#GUID-4856361B-8BFD-4964-AE84-121F5F6CF111
                args.append( "--module-path" )
                        .append( NL )
                        .append( '"' );

                for ( Iterator<String> it = modulePath.iterator(); it.hasNext(); )
                {
                    args.append( replace( it.next(), "\\", "\\\\" ) );
                    if ( it.hasNext() )
                    {
                        args.append( pathSeparatorChar );
                    }
                }

                args.append( '"' )
                        .append( NL );
            }

            if ( !classPath.isEmpty() )
            {
                args.append( "--class-path" )
                        .append( NL )
                        .append( '"' );

                for ( Iterator<String> it = classPath.iterator(); it.hasNext(); )
                {
                    args.append( replace( it.next(), "\\", "\\\\" ) );
                    if ( it.hasNext() )
                    {
                        args.append( pathSeparatorChar );
                    }
                }

                args.append( '"' )
                        .append( NL );
            }

            if ( isMainDescriptor )
            {
                args.append( "--patch-module" )
                        .append( NL )
                        .append( moduleName )
                        .append( '=' )
                        .append( '"' )
                        .append( replace( patchFile.getPath(), "\\", "\\\\" ) )
                        .append( '"' )
                        .append( NL );

                for ( String pkg : packages )
                {
                    args.append( "--add-opens" )
                            .append( NL )
                            .append( moduleName )
                            .append( '/' )
                            .append( pkg )
                            .append( '=' )
                            .append( "ALL-UNNAMED" )
                            .append( NL );
                }

                args.append( "--add-modules" )
                        .append( NL )
                        .append( moduleName )
                        .append( NL );

                args.append( "--add-reads" )
                        .append( NL )
                        .append( moduleName )
                        .append( '=' )
                        .append( "ALL-UNNAMED" )
                        .append( NL );
            }
            else
            {
                args.append( "--add-modules" )
                    .append( NL )
                    .append( moduleName )
                    .append( NL );
            }

            for ( String[] entries : providerJpmsArguments )
            {
                for ( String entry : entries )
                {
                    args.append( entry )
                        .append( NL );
                }
            }

            args.append( startClassName );

            String argsFileContent = args.toString();

            if ( isDebug() )
            {
                getLogger().debug( "args file content:" + NL + argsFileContent );
            }

            io.write( argsFileContent );

            return surefireArgs;
        }
    }
}
