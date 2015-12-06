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

import static org.apache.maven.plugin.surefire.util.ScannerUtil.convertSlashToSystemFileSeparator;
import junit.framework.TestCase;

/**
 * @author Kristian Rosenvold
 */
@Deprecated
public class SpecificFileFilterTest
    extends TestCase
{
    public void testMatchSingleCharacterWildcard()
    {
        SpecificFileFilter filter = createFileFilter( "org/apache/maven/surefire/?pecificTestClassFilter.class" );
        assertTrue( filter.accept( getFile() ) );
    }

    public void testMatchSingleSegmentWordWildcard()
    {
        SpecificFileFilter filter = createFileFilter( "org/apache/maven/surefire/*TestClassFilter.class" );
        assertTrue( filter.accept( getFile() ) );
    }

    public void testMatchMultiSegmentWildcard()
    {
        SpecificFileFilter filter = createFileFilter( "org/**/SpecificTestClassFilter.class" );
        assertTrue( filter.accept( getFile() ) );
    }

    public void testMatchSingleSegmentWildcard()
    {
        SpecificFileFilter filter = createFileFilter( "org/*/maven/surefire/SpecificTestClassFilter.class" );
        assertTrue( filter.accept( getFile() ) );
    }

    private SpecificFileFilter createFileFilter( String s )
    {
        return new SpecificFileFilter( new String[]{ s } );
    }

    private String getFile()
    {
        return convertSlashToSystemFileSeparator( "org/apache/maven/surefire/SpecificTestClassFilter.class" );
    }
}
