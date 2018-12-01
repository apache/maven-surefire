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
import org.apache.maven.plugin.surefire.booterclient.output.InPluginProcessDumpSingleton;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.SurefireBooterForkException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static java.nio.file.Files.isDirectory;
import static org.apache.maven.plugin.surefire.SurefireHelper.escapeToPlatformPath;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.21.0.Jigsaw
 */
public final class JarManifestForkConfiguration
    extends AbstractClasspathForkConfiguration
{
    @SuppressWarnings( "checkstyle:parameternumber" )
    public JarManifestForkConfiguration( @Nonnull Classpath bootClasspath, @Nonnull File tempDirectory,
                                         @Nullable String debugLine, @Nonnull File workingDirectory,
                                         @Nonnull Properties modelProperties, @Nullable String argLine,
                                         @Nonnull Map<String, String> environmentVariables, boolean debug,
                                         int forkCount, boolean reuseForks, @Nonnull Platform pluginPlatform,
                                         @Nonnull ConsoleLogger log )
    {
        super( bootClasspath, tempDirectory, debugLine, workingDirectory, modelProperties, argLine,
                environmentVariables, debug, forkCount, reuseForks, pluginPlatform, log );
    }

    @Override
    protected void resolveClasspath( @Nonnull OutputStreamFlushableCommandline cli,
                                     @Nonnull String booterThatHasMainMethod,
                                     @Nonnull StartupConfiguration config,
                                     @Nonnull File dumpLogDirectory )
            throws SurefireBooterForkException
    {
        try
        {
            File jar = createJar( toCompleteClasspath( config ), booterThatHasMainMethod, dumpLogDirectory );
            cli.createArg().setValue( "-jar" );
            cli.createArg().setValue( escapeToPlatformPath( jar.getAbsolutePath() ) );
        }
        catch ( IOException e )
        {
            throw new SurefireBooterForkException( "Error creating archive file", e );
        }
    }

    /**
     * Create a jar with just a manifest containing a Main-Class entry for BooterConfiguration and a Class-Path entry
     * for all classpath elements.
     *
     * @param classPath      List&lt;String&gt; of all classpath elements.
     * @param startClassName The class name to start (main-class)
     * @return file of the jar
     * @throws IOException When a file operation fails.
     */
    @Nonnull
    private File createJar( @Nonnull List<String> classPath, @Nonnull String startClassName,
                            @Nonnull File dumpLogDirectory )
            throws IOException
    {
        File file = File.createTempFile( "surefirebooter", ".jar", getTempDirectory() );
        if ( !isDebug() )
        {
            file.deleteOnExit();
        }
        Path parent = file.getParentFile().toPath().normalize();
        FileOutputStream fos = new FileOutputStream( file );
        try ( JarOutputStream jos = new JarOutputStream( fos ) )
        {
            jos.setLevel( JarOutputStream.STORED );
            JarEntry je = new JarEntry( "META-INF/MANIFEST.MF" );
            jos.putNextEntry( je );

            Manifest man = new Manifest();

            // we can't use StringUtils.join here since we need to add a '/' to
            // the end of directory entries - otherwise the jvm will ignore them.
            StringBuilder cp = new StringBuilder();
            for ( Iterator<String> it = classPath.iterator(); it.hasNext(); )
            {
                Path classPathElement = Paths.get( it.next() );
                String uri = toClasspathElementUri( parent, classPathElement, dumpLogDirectory );
                cp.append( uri );
                if ( isDirectory( classPathElement ) && !uri.endsWith( "/" ) )
                {
                    cp.append( '/' );
                }

                if ( it.hasNext() )
                {
                    cp.append( ' ' );
                }
            }

            man.getMainAttributes().putValue( "Manifest-Version", "1.0" );
            man.getMainAttributes().putValue( "Class-Path", cp.toString().trim() );
            man.getMainAttributes().putValue( "Main-Class", startClassName );

            man.write( jos );

            jos.closeEntry();
            jos.flush();

            return file;
        }
    }

    static String relativize( @Nonnull Path parent, @Nonnull Path child )
            throws IllegalArgumentException
    {
        return parent.relativize( child )
                .toString();
    }

    static String toAbsoluteUri( @Nonnull Path absolutePath )
    {
        return absolutePath.toUri()
                .toASCIIString();
    }

    static String toClasspathElementUri( @Nonnull Path parent,
                                         @Nonnull Path classPathElement,
                                         @Nonnull File dumpLogDirectory )
            throws IOException
    {
        try
        {
            String relativeUriPath = relativize( parent, classPathElement )
                    .replace( '\\', '/' );

            return new URI( null, relativeUriPath, null )
                    .toASCIIString();
        }
        catch ( IllegalArgumentException e )
        {
            String error = "Boot Manifest-JAR contains absolute paths in classpath " + classPathElement;
            InPluginProcessDumpSingleton.getSingleton()
                    .dumpException( e, error, dumpLogDirectory );

            return toAbsoluteUri( classPathElement );
        }
        catch ( URISyntaxException e )
        {
            // This is really unexpected, so fail
            throw new IOException( "Could not create a relative path "
                    + classPathElement
                    + " against "
                    + parent, e );
        }
    }
}
