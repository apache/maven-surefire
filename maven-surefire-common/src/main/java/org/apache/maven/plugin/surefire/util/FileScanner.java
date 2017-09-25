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

import org.apache.maven.surefire.testset.TestFilter;

import java.io.File;
import java.util.Collection;

import static org.apache.maven.surefire.util.internal.StringUtils.isBlank;

final class FileScanner
{
    private final File basedir;

    private final String ext;

    FileScanner( File basedir, String ext )
    {
        this.basedir = basedir;
        ext = ext.trim();
        if ( isBlank( ext ) )
        {
            throw new IllegalArgumentException( "No file extension" );
        }
        this.ext = ext.startsWith( "." ) ? ext : "." + ext;
    }

    void scanTo( Collection<String> scannedJavaClassNames, TestFilter<String, String> filter )
    {
        scan( scannedJavaClassNames, filter, basedir );
    }

    private void scan( Collection<String> scannedJavaClassNames,
                       TestFilter<String, String> filter, File basedir, String... subDirectories )
    {
        File[] filesAndDirs = basedir.listFiles();
        if ( filesAndDirs != null )
        {
            final String pAckage = toJavaPackage( subDirectories );
            final String path = toPath( subDirectories );
            final String ext = this.ext;
            final boolean hasExtension = ext != null;
            final int extLength = hasExtension ? ext.length() : 0;
            for ( File fileOrDir : filesAndDirs )
            {
                String name = fileOrDir.getName();
                if ( !name.isEmpty() )
                {
                    if ( fileOrDir.isFile() )
                    {
                        final int clsLength = name.length() - extLength;
                        if ( clsLength > 0
                            && ( !hasExtension || name.regionMatches( true, clsLength, ext, 0, extLength ) ) )
                        {
                            String simpleClassName = hasExtension ? name.substring( 0, clsLength ) : name;
                            if ( filter.shouldRun( toFile( path, simpleClassName ), null ) )
                            {
                                String fullyQualifiedClassName =
                                    pAckage.isEmpty() ? simpleClassName : pAckage + '.' + simpleClassName;
                                scannedJavaClassNames.add( fullyQualifiedClassName );
                            }
                        }
                    }
                    else if ( fileOrDir.isDirectory() )
                    {
                        String[] paths = new String[subDirectories.length + 1];
                        System.arraycopy( subDirectories, 0, paths, 0, subDirectories.length );
                        paths[subDirectories.length] = name;
                        scan( scannedJavaClassNames, filter, fileOrDir, paths );
                    }
                }
            }
        }
    }

    private static String toJavaPackage( String... subDirectories )
    {
        StringBuilder pkg = new StringBuilder();
        for ( int i = 0; i < subDirectories.length; i++ )
        {
            if ( i > 0 && i < subDirectories.length )
            {
                pkg.append( '.' );
            }
            pkg.append( subDirectories[i] );
        }
        return pkg.toString();
    }

    private static String toPath( String... subDirectories )
    {
        StringBuilder pkg = new StringBuilder();
        for ( int i = 0; i < subDirectories.length; i++ )
        {
            if ( i > 0 && i < subDirectories.length )
            {
                pkg.append( '/' );
            }
            pkg.append( subDirectories[i] );
        }
        return pkg.toString();
    }

    private String toFile( String path, String fileNameWithoutExtension )
    {
        String pathWithoutExtension =
            path.isEmpty() ? fileNameWithoutExtension : path + '/' + fileNameWithoutExtension;
        return pathWithoutExtension + ext;
    }
}
