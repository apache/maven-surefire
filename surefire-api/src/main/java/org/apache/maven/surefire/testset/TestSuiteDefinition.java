package org.apache.maven.surefire.testset;

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

/**
 * @author Kristian Rosenvold
 */
public class TestSuiteDefinition
{
    private final File[] suiteXmlFiles;

    /**
     * The test that will be invoked through a fork; used only for forkmode=pertest, when the classpath
     * scanning happens on the plugin-side. When this is set, the forked process will run only that test
     * and not scan the classpath
     */
    private final String testForFork;

    private final File testSourceDirectory;

    private final String requestedTest;

    public TestSuiteDefinition( Object[] suiteXmlFiles, String testForFork, File testSourceDirectory,
                                String requestedTest )
    {
        this( createFiles( suiteXmlFiles ), testForFork, testSourceDirectory, requestedTest );
    }


    public TestSuiteDefinition( File[] suiteXmlFiles, File testSourceDirectory, String requestedTest )
    {
        this( suiteXmlFiles, null, testSourceDirectory, requestedTest );
    }


    public TestSuiteDefinition( File[] suiteXmlFiles, String testForFork, File testSourceDirectory,
                                String requestedTest )
    {
        this.suiteXmlFiles = suiteXmlFiles;
        this.testForFork = testForFork;
        this.testSourceDirectory = testSourceDirectory;
        this.requestedTest = requestedTest;
    }

    public File[] getSuiteXmlFiles()
    {
        return suiteXmlFiles;
    }

    public File getTestSourceDirectory()
    {
        return testSourceDirectory;
    }

    public String getTestForFork()
    {
        return testForFork;
    }

    public String getRequestedTest()
    {
        return requestedTest;
    }

    private static File[] createFiles( Object[] suiteXmlFiles )
    {
        if ( suiteXmlFiles != null )
        {
            File[] files = new File[suiteXmlFiles.length];
            for ( int i = 0; i < suiteXmlFiles.length; i++ )
            {
                files[i] = (File) suiteXmlFiles[i];
            }
            return files;
        }
        return null;
    }

}
