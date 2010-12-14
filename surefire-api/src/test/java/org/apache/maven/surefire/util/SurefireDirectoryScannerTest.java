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

import junit.framework.TestCase;
import org.apache.maven.surefire.testset.TestSetFailedException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test of the directory scanner.
 */
public class SurefireDirectoryScannerTest
    extends TestCase
{
    public void testLocateTestClasses()
        throws IOException, TestSetFailedException
    {
        File baseDir = new File( new File( "." ).getCanonicalPath() );
        List include = new ArrayList();
        include.add( "**/*ZT*A.java" );
        List exclude = new ArrayList();

        DefaultDirectoryScanner surefireDirectoryScanner = new DefaultDirectoryScanner( baseDir, include, exclude,
                                                                                        "filesystem" );
        String[] classNames = surefireDirectoryScanner.collectTests();
        assertNotNull( classNames );
        assertEquals( 4, classNames.length );
    }
}
