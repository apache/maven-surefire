package org.apache.maven.surefire.util;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.surefire.testset.TestSetFailedException;

import junit.framework.TestCase;

/**
 * Test of the directory scanner.
 */
public class DefaultDirectoryScannerTest
    extends TestCase
{
    public void testLocateTestClasses()
        throws IOException, TestSetFailedException
    {
        // use target as people can configure ide to compile in an other place than maven
        File baseDir = new File( new File( "target" ).getCanonicalPath() );
        List<String> include = new ArrayList<>();
        include.add( "**/*ZT*A.java" );
        List<String> exclude = new ArrayList<>();

        DefaultDirectoryScanner surefireDirectoryScanner =
            new DefaultDirectoryScanner( baseDir, include, exclude, new ArrayList<String>() );

        String[] classNames = surefireDirectoryScanner.collectTests();
        assertNotNull( classNames );
        System.out.println( "classNames " + Arrays.asList( classNames ) );
        assertEquals( 4, classNames.length );
    }
}
