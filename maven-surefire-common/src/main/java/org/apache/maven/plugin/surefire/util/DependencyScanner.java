package org.apache.maven.plugin.surefire.util;

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
import static org.apache.maven.plugin.surefire.util.ScannerUtil.convertJarFileResourceToJavaClassName;
import static org.apache.maven.plugin.surefire.util.ScannerUtil.convertSlashToSystemFileSeparator;
import static org.apache.maven.plugin.surefire.util.ScannerUtil.processIncludesExcludes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.utils.io.MatchPatterns;
import org.apache.maven.surefire.util.DefaultScanResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Scans dependencies looking for tests.
 * 
 * @author Aslak Knutsen
 */
public class DependencyScanner
{

    private final List<File> dependenciesToScan;

    protected final List<String> includes;

    protected final @Nonnull List<String> excludes;

    protected final List<String> specificTests;

    public DependencyScanner( List<File> dependenciesToScan, List<String> includes, @Nonnull List<String> excludes, List<String> specificTests )
    {
        this.dependenciesToScan = dependenciesToScan;
        this.includes = includes;
        this.excludes = excludes;
        this.specificTests = specificTests;
    }

    public DefaultScanResult scan()
        throws MojoExecutionException
    {
        Matcher matcher = new Matcher( includes, excludes, specificTests );
        List<String> found = new ArrayList<String>();
        for ( File artifact : dependenciesToScan )
        {
            try
            {
                found.addAll( scanArtifact( artifact, matcher ) );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Could not scan dependency " + artifact.toString(), e );
            }
        }
        return new DefaultScanResult( found );
    }

    private List<String> scanArtifact( File artifact, Matcher matcher )
        throws IOException
    {
        List<String> found = new ArrayList<String>();

        if ( artifact != null )
        {
            if ( artifact.isFile() )
            {
                JarFile jar = null;
                try
                {
                    jar = new JarFile( artifact );
                    Enumeration<JarEntry> entries = jar.entries();
                    while ( entries.hasMoreElements() )
                    {
                        JarEntry entry = entries.nextElement();
                        if ( matcher.shouldInclude( entry.getName() ) )
                        {
                            found.add( convertJarFileResourceToJavaClassName( entry.getName() ) );
                        }
                    }
                }
                finally
                {
                    if ( jar != null )
                    {
                        jar.close();
                    }
                }
            }
        }
        return found;
    }

    public static List<File> filter( List<Artifact> artifacts, List<String> groupArtifactIds )
    {
        List<File> matches = new ArrayList<File>();
        if ( groupArtifactIds == null || artifacts == null )
        {
            return matches;
        }
        for ( Artifact artifact : artifacts )
        {
            for ( String groups : groupArtifactIds )
            {
                String[] groupArtifact = groups.split( ":" );
                if ( groupArtifact.length != 2 )
                {
                    throw new IllegalArgumentException(
                                                        "dependencyToScan argument should be in format 'groupid:artifactid': "
                                                            + groups );
                }
                if ( artifact.getGroupId().matches( groupArtifact[0] )
                    && artifact.getArtifactId().matches( groupArtifact[1] ) )
                {
                    matches.add( artifact.getFile() );
                }
            }
        }
        return matches;
    }

    private class Matcher
    {

        private MatchPatterns includes;

        private MatchPatterns excludes;

        private SpecificFileFilter specificTestFilter;

        public Matcher( @Nullable List<String> includes, @Nonnull List<String> excludes, @Nullable List<String> specificTests )
        {
            String[] specific = specificTests == null ? new String[0] : processIncludesExcludes( specificTests );
            specificTestFilter = new SpecificFileFilter( specific );

            if ( includes != null && includes.size() > 0 )
            {
                this.includes = MatchPatterns.from( processIncludesExcludes( includes ) );
            }
            else
            {
                this.includes = MatchPatterns.from( "**" );
            }
            this.excludes = MatchPatterns.from( processIncludesExcludes( excludes ) );
        }

        public boolean shouldInclude( String name )
        {
            if ( !name.endsWith( ".class" ) )
            {
                return false;
            }
            name = convertSlashToSystemFileSeparator( name );
            boolean isIncluded = includes.matches( name, false );
            boolean isExcluded = excludes.matches( name, false );

            return isIncluded && !isExcluded && specificTestFilter.accept( name );
        }
    }
}
