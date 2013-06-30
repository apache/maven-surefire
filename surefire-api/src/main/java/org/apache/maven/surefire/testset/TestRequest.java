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
import java.util.ArrayList;
import java.util.List;

/**
 * Information about the requested test.
 *
 * @author Kristian Rosenvold
 */
public class TestRequest
{
    private final List<File> suiteXmlFiles;

    private final File testSourceDirectory;

    private final String requestedTest;

    /**
     * @since 2.7.3
     */
    private final String requestedTestMethod;

    public TestRequest( List suiteXmlFiles, File testSourceDirectory, String requestedTest )
    {
        this( suiteXmlFiles, testSourceDirectory, requestedTest, null );
    }

    /**
     * @since 2.7.3
     */
    public TestRequest( List suiteXmlFiles, File testSourceDirectory, String requestedTest, String requestedTestMethod )
    {
        this.suiteXmlFiles = createFiles( suiteXmlFiles );
        this.testSourceDirectory = testSourceDirectory;
        this.requestedTest = requestedTest;
        this.requestedTestMethod = requestedTestMethod;
    }

    /**
     * Represents suitexmlfiles that define the test-run request
     *
     * @return A list of java.io.File objects.
     */
    public List<File> getSuiteXmlFiles()
    {
        return suiteXmlFiles;
    }

    /**
     * Test source directory, normally ${project.build.testSourceDirectory}
     *
     * @return A file pointing to test sources
     */
    public File getTestSourceDirectory()
    {
        return testSourceDirectory;
    }

    /**
     * A specific test request issued with -Dtest= from the command line.
     *
     * @return The string specified at the command line
     */
    public String getRequestedTest()
    {
        return requestedTest;
    }

    /**
     * A specific test request method issued with -Dtest=class#method from the command line.
     *
     * @return The string specified at the command line
     * @since 2.7.3
     */
    public String getRequestedTestMethod()
    {
        return requestedTestMethod;
    }

    private static List<File> createFiles( List suiteXmlFiles )
    {
        if ( suiteXmlFiles != null )
        {
            List<File> files = new ArrayList<File>();
            Object element;
            for ( Object suiteXmlFile : suiteXmlFiles )
            {
                element = suiteXmlFile;
                files.add( element instanceof String ? new File( (String) element ) : (File) element );
            }
            return files;
        }
        return null;
    }

}
