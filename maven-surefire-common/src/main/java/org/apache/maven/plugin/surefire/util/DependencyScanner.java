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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.artifact.filter.PatternIncludesArtifactFilter;
import org.apache.maven.surefire.testset.TestFilter;
import org.apache.maven.surefire.testset.TestListResolver;
import org.apache.maven.surefire.util.DefaultScanResult;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.apache.maven.plugin.surefire.util.ScannerUtil.convertJarFileResourceToJavaClassName;
import static org.apache.maven.plugin.surefire.util.ScannerUtil.isJavaClassFile;

/**
 * Scans dependencies looking for tests.
 *
 * @author Aslak Knutsen
 */
public class DependencyScanner
{
    private final List<File> dependenciesToScan;

    private final TestListResolver filter;

    public DependencyScanner( List<File> dependenciesToScan, TestListResolver filter )
    {
        this.dependenciesToScan = dependenciesToScan;
        this.filter = filter;
    }

    public DefaultScanResult scan()
        throws MojoExecutionException
    {
        Set<String> classes = new LinkedHashSet<>();
        for ( File artifact : dependenciesToScan )
        {
            if ( artifact != null && artifact.isFile() && artifact.getName().endsWith( ".jar" ) )
            {
                try
                {
                    scanArtifact( artifact, filter, classes );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Could not scan dependency " + artifact.toString(), e );
                }
            }
        }
        return new DefaultScanResult( new ArrayList<>( classes ) );
    }

    private static void scanArtifact( File artifact, TestFilter<String, String> filter, Set<String> classes )
        throws IOException
    {
        try ( JarFile jar = new JarFile( artifact ) )
        {
            for ( Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements(); )
            {
                JarEntry entry = entries.nextElement();
                String path = entry.getName();
                if ( !entry.isDirectory() && isJavaClassFile( path ) && filter.shouldRun( path, null ) )
                {
                    classes.add( convertJarFileResourceToJavaClassName( path ) );
                }
            }
        }
    }

    /**
     *
     * @param artifacts a list to filter
     * @param artifactPatterns a list of strings in the form
     *                         <pre>groupId[:artifactId[:type[:classifier][:version]]]</pre>
     * @return list of items from <code>artifacts</code> that match any of the filters in <code>groupArtifactIds</code>,
     * empty if none match
     */
    public static List<Artifact> filter( List<Artifact> artifacts, List<String> artifactPatterns )
    {
        if ( artifactPatterns == null || artifacts == null || artifacts.isEmpty() )
        {
            return Collections.emptyList();
        }

        PatternIncludesArtifactFilter artifactFilter = new PatternIncludesArtifactFilter( artifactPatterns );

        List<Artifact> matches = new ArrayList<>();
        for ( Artifact artifact : artifacts )
        {
            if ( artifactFilter.include( artifact ) )
            {
                matches.add( artifact );
            }
        }
        return matches;
    }
}
