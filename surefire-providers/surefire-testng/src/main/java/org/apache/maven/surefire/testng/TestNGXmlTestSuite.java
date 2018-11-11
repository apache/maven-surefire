package org.apache.maven.surefire.testng;

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
import java.util.Map;

import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.testset.TestSetFailedException;

import static org.apache.maven.surefire.testng.TestNGExecutor.run;

/**
 * Handles suite xml file definitions for TestNG.
 *
 * @author jkuhnert
 * @author <a href='mailto:the[dot]mindstorm[at]gmail[dot]com'>Alex Popescu</a>
 */
final class TestNGXmlTestSuite
        extends TestSuite
{
    private final List<File> suiteFiles;

    private List<String> suiteFilePaths;

    private final String testSourceDirectory;

    private final Map<String, String> options;

    private final File reportsDirectory;

    private final int skipAfterFailureCount;

    /**
     * Creates a testng testset to be configured by the specified
     * xml file(s). The XML files are suite definitions files according to TestNG DTD.
     */
    TestNGXmlTestSuite( List<File> suiteFiles, String testSourceDirectory, Map<String, String> confOptions,
                        File reportsDirectory, int skipAfterFailureCount )
    {
        this.suiteFiles = suiteFiles;
        this.options = confOptions;
        this.testSourceDirectory = testSourceDirectory;
        this.reportsDirectory = reportsDirectory;
        this.skipAfterFailureCount = skipAfterFailureCount;
    }

    void execute( RunListener reporter )
        throws TestSetFailedException
    {
        if ( suiteFilePaths == null )
        {
            throw new IllegalStateException( "You must call locateTestSets before calling execute" );
        }
        startTestSuite( reporter );
        run( suiteFilePaths, testSourceDirectory, options, reporter, reportsDirectory, skipAfterFailureCount );
        finishTestSuite( reporter );
    }

    Iterable locateTestSets()
        throws TestSetFailedException
    {
        if ( suiteFilePaths != null )
        {
            throw new IllegalStateException( "You can't call locateTestSets twice" );
        }

        if ( suiteFiles.isEmpty() )
        {
            throw new IllegalStateException( "No suite files were specified" );
        }

        suiteFilePaths = new ArrayList<>( suiteFiles.size() );
        ArrayList<File> testSets = new ArrayList<>( suiteFiles.size() );

        for ( File suiteFile : suiteFiles )
        {
            if ( !suiteFile.isFile() )
            {
                throw new TestSetFailedException( "Suite file " + suiteFile + " is not a valid file" );
            }
            testSets.add( suiteFile );
            suiteFilePaths.add( suiteFile.getAbsolutePath() );
        }
        return testSets;
    }

    @Override
    Map<String, String> getOptions()
    {
        return options;
    }
}
