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

import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.OutputStreamFlushableCommandline;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.booter.AbstractPathConfiguration;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.ModularClasspath;
import org.apache.maven.surefire.booter.ModularClasspathConfiguration;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.SurefireBooterForkException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ModuleVisitor;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
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
import static org.objectweb.asm.Opcodes.ASM6;

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
                                              boolean debug,
                                              @Nonnegative int forkCount,
                                              boolean reuseForks,
                                              @Nonnull Platform pluginPlatform,
                                              @Nonnull ConsoleLogger log )
    {
        super( bootClasspath, tempDirectory, debugLine, workingDirectory, modelProperties, argLine,
                environmentVariables, debug, forkCount, reuseForks, pluginPlatform, log );
    }

    @Override
    protected void resolveClasspath( @Nonnull OutputStreamFlushableCommandline cli, @Nonnull String startClass,
                                     @Nonnull StartupConfiguration config )
            throws SurefireBooterForkException
    {
        try
        {
            AbstractPathConfiguration pathConfig = config.getClasspathConfiguration();

            ModularClasspathConfiguration modularClasspathConfiguration =
                    pathConfig.toRealPath( ModularClasspathConfiguration.class );

            ModularClasspath modularClasspath = modularClasspathConfiguration.getModularClasspath();

            File descriptor = modularClasspath.getModuleDescriptor();
            List<String> modulePath = modularClasspath.getModulePath();
            Collection<String> packages = modularClasspath.getPackages();
            File patchFile = modularClasspath.getPatchFile();
            List<String> classpath = toCompleteClasspath( config );

            File argsFile = createArgsFile( descriptor, modulePath, classpath, packages, patchFile, startClass );

            cli.createArg().setValue( "@" + escapeToPlatformPath( argsFile.getAbsolutePath() ) );
        }
        catch ( IOException e )
        {
            throw new SurefireBooterForkException( "Error creating args file", e );
        }
    }

    @Nonnull
    File createArgsFile( @Nonnull File moduleDescriptor, @Nonnull List<String> modulePath,
                         @Nonnull List<String> classPath, @Nonnull Collection<String> packages,
                         @Nonnull File patchFile, @Nonnull String startClassName )
            throws IOException
    {
        File surefireArgs = createTempFile( "surefireargs", "", getTempDirectory() );
        if ( !isDebug() )
        {
            surefireArgs.deleteOnExit();
        }

        BufferedWriter writer = null;
        try
        {
            writer = new BufferedWriter( new FileWriter( surefireArgs ) );

            if ( !modulePath.isEmpty() )
            {
                writer.write( "--module-path" );
                writer.newLine();

                for ( Iterator<String> it = modulePath.iterator(); it.hasNext(); )
                {
                    writer.append( it.next() );
                    if ( it.hasNext() )
                    {
                        writer.append( pathSeparatorChar );
                    }
                }

                writer.newLine();
            }

            if ( !classPath.isEmpty() )
            {
                writer.write( "--class-path" );
                writer.newLine();
                for ( Iterator<String> it = classPath.iterator(); it.hasNext(); )
                {
                    writer.append( it.next() );
                    if ( it.hasNext() )
                    {
                        writer.append( pathSeparatorChar );
                    }
                }

                writer.newLine();
            }

            final String moduleName = toModuleName( moduleDescriptor );

            writer.write( "--patch-module" );
            writer.newLine();
            writer.append( moduleName )
                    .append( '=' )
                    .append( patchFile.getPath() );

            writer.newLine();

            for ( String pkg : packages )
            {
                writer.write( "--add-exports" );
                writer.newLine();
                writer.append( moduleName )
                        .append( '/' )
                        .append( pkg )
                        .append( '=' )
                        .append( "ALL-UNNAMED" );

                writer.newLine();
            }

            writer.write( "--add-modules" );
            writer.newLine();
            writer.append( moduleName );

            writer.newLine();

            writer.write( "--add-reads" );
            writer.newLine();
            writer.append( moduleName )
                    .append( '=' )
                    .append( "ALL-UNNAMED" );

            writer.newLine();

            writer.write( startClassName );

            writer.newLine();
        }
        finally
        {
            if ( writer != null )
            {
                writer.close();
            }
        }

        return surefireArgs;
    }

    @Nonnull
    String toModuleName( @Nonnull File moduleDescriptor ) throws IOException
    {
        if ( !moduleDescriptor.isFile() )
        {
            throw new IOException( "No such Jigsaw module-descriptor exists " + moduleDescriptor.getAbsolutePath() );
        }

        final StringBuilder sb = new StringBuilder();
        new ClassReader( new FileInputStream( moduleDescriptor ) ).accept( new ClassVisitor( ASM6 )
        {
            @Override
            public ModuleVisitor visitModule( String name, int access, String version )
            {
                sb.setLength( 0 );
                sb.append( name );
                return super.visitModule( name, access, version );
            }
        }, 0 );

        return sb.toString();
    }
}
