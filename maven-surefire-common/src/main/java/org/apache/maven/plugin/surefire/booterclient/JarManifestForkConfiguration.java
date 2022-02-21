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

import org.apache.maven.surefire.shared.compress.archivers.zip.Zip64Mode;
import org.apache.maven.surefire.shared.compress.archivers.zip.ZipArchiveEntry;
import org.apache.maven.surefire.shared.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.Commandline;
import org.apache.maven.plugin.surefire.booterclient.output.InPluginProcessDumpSingleton;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.SurefireBooterForkException;
import org.apache.maven.surefire.extensions.ForkNodeFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Manifest;
import java.util.zip.Deflater;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.isDirectory;
import static org.apache.maven.plugin.surefire.SurefireHelper.escapeToPlatformPath;
import static org.apache.maven.plugin.surefire.booterclient.JarManifestForkConfiguration.ClasspathElementUri.absolute;
import static org.apache.maven.plugin.surefire.booterclient.JarManifestForkConfiguration.ClasspathElementUri.relative;
import static org.apache.maven.surefire.shared.utils.StringUtils.isNotBlank;
import static org.apache.maven.surefire.api.util.internal.StringUtils.NL;

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
                                         @Nonnull Map<String, String> environmentVariables,
                                         @Nonnull String[] excludedEnvironmentVariables,
                                         boolean debug,
                                         int forkCount, boolean reuseForks, @Nonnull Platform pluginPlatform,
                                         @Nonnull ConsoleLogger log,
                                         @Nonnull ForkNodeFactory forkNodeFactory )
    {
        super( bootClasspath, tempDirectory, debugLine, workingDirectory, modelProperties, argLine,
            environmentVariables, excludedEnvironmentVariables, debug, forkCount, reuseForks, pluginPlatform, log,
            forkNodeFactory );
    }

    @Override
    protected void resolveClasspath( @Nonnull Commandline cli,
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
        Path parent = file.getParentFile().toPath();
        OutputStream fos = new BufferedOutputStream( new FileOutputStream( file ), 64 * 1024 );

        try ( ZipArchiveOutputStream zos = new ZipArchiveOutputStream( fos ) )
        {
            zos.setUseZip64( Zip64Mode.Never );
            zos.setLevel( Deflater.NO_COMPRESSION );

            ZipArchiveEntry ze = new ZipArchiveEntry( "META-INF/MANIFEST.MF" );
            zos.putArchiveEntry( ze );

            Manifest man = new Manifest();

            boolean dumpError = true;

            // we can't use StringUtils.join here since we need to add a '/' to
            // the end of directory entries - otherwise the jvm will ignore them.
            StringBuilder cp = new StringBuilder();
            for ( Iterator<String> it = classPath.iterator(); it.hasNext(); )
            {
                Path classPathElement = Paths.get( it.next() );
                ClasspathElementUri classpathElementUri =
                        toClasspathElementUri( parent, classPathElement, dumpLogDirectory, dumpError );
                // too many errors in dump file with the same root cause may slow down the Boot Manifest-JAR startup
                dumpError &= !classpathElementUri.absolute;
                cp.append( classpathElementUri.uri );
                if ( isDirectory( classPathElement ) && !classpathElementUri.uri.endsWith( "/" ) )
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

            man.write( zos );

            zos.closeArchiveEntry();

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

    static ClasspathElementUri toClasspathElementUri( @Nonnull Path parent,
                                         @Nonnull Path classPathElement,
                                         @Nonnull File dumpLogDirectory,
                                         boolean dumpError )
    {
        try
        {
            String relativePath = relativize( parent, classPathElement );
            return relative( escapeUri( relativePath, UTF_8 ) );
        }
        catch ( IllegalArgumentException e )
        {
            if ( dumpError )
            {
                String error = "Boot Manifest-JAR contains absolute paths in classpath '"
                        + classPathElement
                        + "'"
                        + NL
                        + "Hint: <argLine>-Djdk.net.URLClassPath.disableClassPathURLCheck=true</argLine>";

                if ( isNotBlank( e.getLocalizedMessage() ) )
                {
                    error += NL;
                    error += e.getLocalizedMessage();
                }

                InPluginProcessDumpSingleton.getSingleton()
                        .dumpStreamText( error, dumpLogDirectory );
            }

            return absolute( toAbsoluteUri( classPathElement ) );
        }
    }

    static final class ClasspathElementUri
    {
        final String uri;
        final boolean absolute;

        private ClasspathElementUri( String uri, boolean absolute )
        {
            this.uri = uri;
            this.absolute = absolute;
        }

        static ClasspathElementUri absolute( String uri )
        {
            return new ClasspathElementUri( uri, true );
        }

        static ClasspathElementUri relative( String uri )
        {
            return new ClasspathElementUri( uri, false );
        }
    }

    static String escapeUri( String input, Charset encoding )
    {
        try
        {
            String uriFormEncoded = URLEncoder.encode( input, encoding.name() );

            String uriPathEncoded = uriFormEncoded.replaceAll( "\\+", "%20" );
            uriPathEncoded = uriPathEncoded.replaceAll( "%2F|%5C", "/" );

            return uriPathEncoded;
        }
        catch ( UnsupportedEncodingException e )
        {
            throw new IllegalStateException( "avoided by using Charset" );
        }
    }
}
