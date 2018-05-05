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
import static org.apache.maven.plugin.surefire.util.ScannerUtil.isJavaClassFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.surefire.testset.TestFilter;
import org.apache.maven.surefire.testset.TestListResolver;
import org.apache.maven.surefire.util.DefaultScanResult;

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
        Set<String> classes = new LinkedHashSet<String>();
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
        return new DefaultScanResult( new ArrayList<String>( classes ) );
    }

    private static void scanArtifact( File artifact, TestFilter<String, String> filter, Set<String> classes )
        throws IOException
    {
        JarFile jar = null;
        try
        {
            jar = new JarFile( artifact );
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
        finally
        {
            if ( jar != null )
            {
                jar.close();
            }
        }
    }

    public static List<Artifact> filter( List<Artifact> artifacts, List<String> groupArtifactIds )
    {
        List<Artifact> matches = new ArrayList<Artifact>();
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
                    throw new IllegalArgumentException( "dependencyToScan argument should be in format"
                        + " 'groupid:artifactid': " + groups );
                }
                if ( artifact.getGroupId().matches( groupArtifact[0] )
                    && artifact.getArtifactId().matches( groupArtifact[1] ) )
                {
                    matches.add( artifact );
                }
            }
        }
        return matches;
    }
}
