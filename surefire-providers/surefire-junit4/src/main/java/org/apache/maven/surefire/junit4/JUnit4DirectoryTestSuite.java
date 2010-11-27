package org.apache.maven.surefire.junit4;

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

import org.apache.maven.surefire.suite.AbstractDirectoryTestSuite;
import org.apache.maven.surefire.testset.SurefireTestSet;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.DirectoryScanner;

import java.io.File;
import java.util.ArrayList;

/**
 * Test suite for JUnit4 based on a directory of Java test classes. This is
 * capable of running both JUnit3 and JUnit4 test classes (I think).
 *
 * @author Karl M. Davis
 */
@SuppressWarnings( { "UnusedDeclaration" } )
public class JUnit4DirectoryTestSuite
    extends AbstractDirectoryTestSuite
{
    // Remove when we no longer build with surefire 2.5
    public JUnit4DirectoryTestSuite( File basedir, ArrayList includes, ArrayList excludes )
    {
        super( basedir, includes, excludes );
    }

    public JUnit4DirectoryTestSuite( DirectoryScanner surefireDirectoryScanner )
    {
        super( surefireDirectoryScanner );
    }


    /**
     * This method will be called for each class to be run as a test. It returns
     * a surefire test set that will later be executed.
     *
     * @see org.apache.maven.surefire.suite.AbstractDirectoryTestSuite#createTestSet(java.lang.Class,
     *      java.lang.ClassLoader)
     */
    protected SurefireTestSet createTestSet( Class testClass, ClassLoader testsClassLoader )
        throws TestSetFailedException
    {
        JUnit4TestChecker jUnit4TestChecker = new JUnit4TestChecker( testsClassLoader );
        return jUnit4TestChecker.isValidJUnit4Test( testClass ) ? new JUnit4TestSet( testClass ) : null;
    }

}
