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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.maven.plugin.surefire.AbstractSurefireMojo;
import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.OutputStreamFlushableCommandline;
import org.apache.maven.plugin.surefire.util.Relocator;
import org.apache.maven.shared.utils.StringUtils;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.ForkedBooter;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.SurefireBooterForkException;
import org.apache.maven.surefire.util.UrlUtils;

/**
 * Configuration for forking tests.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a>
 * @author <a href="mailto:krosenvold@apache.org">Kristian Rosenvold</a>
 */
public class ForkConfiguration
{
    public static final String FORK_ONCE = "once";

    public static final String FORK_ALWAYS = "always";

    public static final String FORK_NEVER = "never";

    public static final String FORK_PERTHREAD = "perthread";

    private final int forkCount;

    private final boolean reuseForks;

    private final Classpath bootClasspathConfiguration;

    private final String jvmExecutable;

    private Properties modelProperties;

    private final String argLine;

    private final Map<String, String> environmentVariables;

    private final File workingDirectory;

    private final File tempDirectory;

    private final boolean debug;

    private final String debugLine;

    public ForkConfiguration( Classpath bootClasspathConfiguration, File tmpDir, String debugLine, String jvmExecutable,
                              File workingDirectory, Properties modelProperties, String argLine, Map<String, String> environmentVariables,
                              boolean debugEnabled, int forkCount, boolean reuseForks )
    {
        this.bootClasspathConfiguration = bootClasspathConfiguration;
        this.tempDirectory = tmpDir;
        this.debugLine = debugLine;
        this.jvmExecutable = jvmExecutable;
        this.workingDirectory = workingDirectory;
        this.modelProperties = modelProperties;
        this.argLine = argLine;
        this.environmentVariables = environmentVariables;
        this.debug = debugEnabled;
        this.forkCount = forkCount;
        this.reuseForks = reuseForks;
    }

    public Classpath getBootClasspath()
    {
        return bootClasspathConfiguration;
    }

    public static String getEffectiveForkMode( String forkMode )
    {
        if ( "pertest".equalsIgnoreCase( forkMode ) )
        {
            return FORK_ALWAYS;
        }
        else if ( "none".equalsIgnoreCase( forkMode ) )
        {
            return FORK_NEVER;
        }
        else if ( forkMode.equals( FORK_NEVER ) || forkMode.equals( FORK_ONCE ) ||
            forkMode.equals( FORK_ALWAYS ) || forkMode.equals( FORK_PERTHREAD ) )
        {
            return forkMode;
        }
        else
        {
            throw new IllegalArgumentException( "Fork mode " + forkMode + " is not a legal value" );
        }
    }

    /**
     * @param classPath            cla the classpath arguments
     * @param startupConfiguration The startup configuration
     * @param threadNumber         the thread number, to be the replacement in the argLine   @return A commandline
     * @throws org.apache.maven.surefire.booter.SurefireBooterForkException
     *          when unable to perform the fork
     */
    public OutputStreamFlushableCommandline createCommandLine( List<String> classPath,
                                                               StartupConfiguration startupConfiguration,
                                                               int threadNumber )
        throws SurefireBooterForkException
    {
        return createCommandLine( classPath,
                                  startupConfiguration.getClassLoaderConfiguration().isManifestOnlyJarRequestedAndUsable(),
                                  startupConfiguration.isShadefire(), startupConfiguration.isProviderMainClass()
            ? startupConfiguration.getActualClassName()
            : ForkedBooter.class.getName(), threadNumber );
    }

    OutputStreamFlushableCommandline createCommandLine( List<String> classPath, boolean useJar, boolean shadefire,
                                                        String providerThatHasMainMethod, int threadNumber )
        throws SurefireBooterForkException
    {
        OutputStreamFlushableCommandline cli = new OutputStreamFlushableCommandline();

        cli.setExecutable( jvmExecutable );

        if ( argLine != null )
        {
            cli.createArg().setLine( replaceThreadNumberPlaceholder( stripNewLines( replacePropertyExpressions( argLine ) ), threadNumber ) );
        }

        if ( environmentVariables != null )
        {

            for ( String key : environmentVariables.keySet() )
            {
                String value = environmentVariables.get( key );

                cli.addEnvironment( key, value );
            }
        }

        if ( getDebugLine() != null && !"".equals( getDebugLine() ) )
        {
            cli.createArg().setLine( getDebugLine() );
        }

        if ( useJar )
        {
            File jarFile;
            try
            {
                jarFile = createJar( classPath, providerThatHasMainMethod );
            }
            catch ( IOException e )
            {
                throw new SurefireBooterForkException( "Error creating archive file", e );
            }

            cli.createArg().setValue( "-jar" );

            cli.createArg().setValue( jarFile.getAbsolutePath() );
        }
        else
        {
            cli.addEnvironment( "CLASSPATH", StringUtils.join( classPath.iterator(), File.pathSeparator ) );

            final String forkedBooter =
                providerThatHasMainMethod != null ? providerThatHasMainMethod : ForkedBooter.class.getName();

            cli.createArg().setValue( shadefire ? new Relocator().relocate( forkedBooter ) : forkedBooter );
        }

        cli.setWorkingDirectory( workingDirectory.getAbsolutePath() );

        return cli;
    }

    private String replaceThreadNumberPlaceholder( String argLine, int threadNumber )
    {
        return argLine.replace( AbstractSurefireMojo.THREAD_NUMBER_PLACEHOLDER,
                                String.valueOf( threadNumber ) ).replace( AbstractSurefireMojo.FORK_NUMBER_PLACEHOLDER,
                                                                          String.valueOf( threadNumber ) );
    }

    /**
     * Replaces expressions <pre>@{property-name}</pre> with the corresponding properties
     * from the model. This allows late evaluation of property values when the plugin is executed (as compared
     * to evaluation when the pom is parsed as is done with <pre>${property-name}</pre> expressions).
     *
     * This allows other plugins to modify or set properties with the changes getting picked up by surefire.
     */
    private String replacePropertyExpressions( String argLine )
    {
        if ( argLine == null ) {
            return null;
        }

        for ( Enumeration<?> e = modelProperties.propertyNames(); e.hasMoreElements(); ) {
            String key = e.nextElement().toString();
            String field = "@{" + key + "}";
            if ( argLine.contains(field) ) {
                argLine = argLine.replace(field, modelProperties.getProperty(key, ""));
            }
        }

        return argLine;
    }

    /**
     * Create a jar with just a manifest containing a Main-Class entry for BooterConfiguration and a Class-Path entry
     * for all classpath elements.
     *
     * @param classPath      List&lt;String> of all classpath elements.
     * @param startClassName  The classname to start (main-class)
     * @return The file pointint to the jar
     * @throws java.io.IOException When a file operation fails.
     */
    private File createJar( List<String> classPath, String startClassName )
        throws IOException
    {
        File file = File.createTempFile( "surefirebooter", ".jar", tempDirectory );
        if ( !debug )
        {
            file.deleteOnExit();
        }
        FileOutputStream fos = new FileOutputStream( file );
        JarOutputStream jos = new JarOutputStream( fos );
        jos.setLevel( JarOutputStream.STORED );
        JarEntry je = new JarEntry( "META-INF/MANIFEST.MF" );
        jos.putNextEntry( je );

        Manifest man = new Manifest();

        // we can't use StringUtils.join here since we need to add a '/' to
        // the end of directory entries - otherwise the jvm will ignore them.
        String cp = "";
        for ( String el : classPath )
        {
            // NOTE: if File points to a directory, this entry MUST end in '/'.
            cp += UrlUtils.getURL( new File( el ) ).toExternalForm() + " ";
        }

        man.getMainAttributes().putValue( "Manifest-Version", "1.0" );
        man.getMainAttributes().putValue( "Class-Path", cp.trim() );
        man.getMainAttributes().putValue( "Main-Class", startClassName );

        man.write( jos );
        jos.close();

        return file;
    }

    public boolean isDebug()
    {
        return debug;
    }

    public String stripNewLines( String argline )
    {
        return argline.replace( "\n", " " ).replace( "\r", " " );
    }

    public String getDebugLine()
    {
        return debugLine;
    }

    public File getTempDirectory()
    {
        return tempDirectory;
    }

    public int getForkCount()
    {
        return forkCount;
    }


    public boolean isReuseForks()
    {
        return reuseForks;
    }
}
