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

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.surefire.util.DefaultScanResult;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Scans directories looking for tests.
 *
 * @author Karl M. Davis
 * @author Kristian Rosenvold
 */
public class DirectoryScanner
{

    private static final String FS = System.getProperty( "file.separator" );

    private static final String JAVA_SOURCE_FILE_EXTENSION = ".java";

    private static final String JAVA_CLASS_FILE_EXTENSION = ".class";

    private final File basedir;

    private final List<String> includes;

    private final List<String> excludes;

    private final List<String> specificTests;

    public DirectoryScanner(File basedir, List<String> includes, List<String> excludes, List<String> specificTests)
    {
        this.basedir = basedir;
        this.includes = includes;
        this.excludes = excludes;
        this.specificTests = specificTests;
    }

    public DefaultScanResult scan()
    {
        String[] specific = specificTests == null ? new String[0] : processIncludesExcludes( specificTests );
        SpecificFileFilter specificTestFilter = new SpecificFileFilter( specific );

        List<String> result = new ArrayList<String>();
        if ( basedir.exists() )
        {
            org.codehaus.plexus.util.DirectoryScanner scanner = new org.codehaus.plexus.util.DirectoryScanner();

            scanner.setBasedir( basedir );

            if ( includes != null )
            {
                scanner.setIncludes( processIncludesExcludes( includes ) );
            }

            if ( excludes != null )
            {
                scanner.setExcludes( processIncludesExcludes( excludes ) );
            }

            scanner.scan();
            for (String test : scanner.getIncludedFiles() ){
                if (specificTestFilter.accept(stripBaseDir(basedir.getAbsolutePath(), test))){
                    result.add( convertToJavaClassName(test));
                }
            }
        }
        return new DefaultScanResult( result);
    }

    private String convertToJavaClassName( String test )
    {
        return StringUtils.removeEnd(test, ".class").replace( FS, "." );
    }

    private String stripBaseDir( String basedir, String test){
            return StringUtils.removeStart( test, basedir );
    }

    private static String[] processIncludesExcludes( List<String> list )
    {
        List<String> newList = new ArrayList<String>();
        for (Object aList : list) {
            String include = (String) aList;
            String[] includes = include.split(",");
            Collections.addAll(newList, includes);
        }

        String[] incs = new String[newList.size()];

        for ( int i = 0; i < incs.length; i++ )
        {
            String inc = newList.get( i );
            if ( inc.endsWith( JAVA_SOURCE_FILE_EXTENSION ) )
            {
                inc = StringUtils.removeEnd(inc, JAVA_SOURCE_FILE_EXTENSION) + JAVA_CLASS_FILE_EXTENSION;
            }
            incs[i] = inc;

        }
        return incs;
    }
}
