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
 * Information about the requested test.
 *
 * @author Kristian Rosenvold
 */
public class TestRequest
{
    private final File[] suiteXmlFiles;

    private final File testSourceDirectory;

    private final String requestedTest;

    public TestRequest( Object[] suiteXmlFiles, File testSourceDirectory, String requestedTest )
    {
        this.suiteXmlFiles = createFiles( suiteXmlFiles );
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

    public String getRequestedTest()
    {
        return requestedTest;
    }

    private static File[] createFiles( Object[] suiteXmlFiles )
    {
        if ( suiteXmlFiles != null )
        {
            File[] files = new File[suiteXmlFiles.length];
            Object element;
            for ( int i = 0; i < suiteXmlFiles.length; i++ )
            {
                element = suiteXmlFiles[i];
                files[i] = element instanceof String ? new File( (String) element ) : (File) element;
            }
            return files;
        }
        return null;
    }

}
