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

import junit.framework.TestCase;
import org.apache.maven.surefire.testset.TestListResolver;
import org.apache.maven.surefire.util.ScanResult;

import java.io.File;
import java.util.*;

/**
 * @author Kristian Rosenvold
 */
public class DirectoryScannerTest
    extends TestCase
{
    public void testLocateTestClasses()
        throws Exception
    {
        // use target as people can configure ide to compile in an other place than maven
        File baseDir = new File( new File( "target/test-classes" ).getCanonicalPath() );
        List<String> include = new ArrayList<String>();
        include.add( "**/*ZT*A.java" );
        List<String> exclude = new ArrayList<String>();

        DirectoryScanner surefireDirectoryScanner =
            new DirectoryScanner( baseDir, new TestListResolver( include, exclude ), new TestListResolver( "" ) );

        ScanResult classNames = surefireDirectoryScanner.scan();
        assertNotNull( classNames );
        System.out.println( "classNames " + Collections.singletonList(classNames));
        assertEquals( 3, classNames.size() );

        Map<String, String> props = new HashMap<String, String>();
        classNames.writeTo( props );
        assertEquals( 3, props.size() );
    }
}
