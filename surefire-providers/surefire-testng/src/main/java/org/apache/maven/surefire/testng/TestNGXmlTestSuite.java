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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.surefire.report.ConsoleOutputCapture;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.testset.TestSetFailedException;

/**
 * Handles suite xml file definitions for TestNG.
 *
 * @author jkuhnert
 * @author <a href='mailto:the[dot]mindstorm[at]gmail[dot]com'>Alex Popescu</a>
 */
public class TestNGXmlTestSuite
    implements TestNgTestSuite
{
    private final List suiteFiles;

    private List<String> suiteFilePaths;

    private final String testSourceDirectory;

    private final Map<String, String> options;

    private final File reportsDirectory;

    // Not really used
    private Map<File, String> testSets;

    /**
     * Creates a testng testset to be configured by the specified
     * xml file(s). The XML files are suite definitions files according to TestNG DTD.
     */
    public TestNGXmlTestSuite( List<File> suiteFiles, String testSourceDirectory, Map<String, String> confOptions,
                               File reportsDirectory )
    {
        this.suiteFiles = suiteFiles;

        this.options = confOptions;

        this.testSourceDirectory = testSourceDirectory;

        this.reportsDirectory = reportsDirectory;
    }

    public void execute( ReporterFactory reporterManagerFactory )
        throws TestSetFailedException
    {
        if ( testSets == null )
        {
            throw new IllegalStateException( "You must call locateTestSets before calling execute" );
        }
//        RunListener reporter = new SynchronizedReporterManager( reporterManagerFactory.createReporter() );
        RunListener reporter = reporterManagerFactory.createReporter();
        ConsoleOutputCapture.startCapture( (ConsoleOutputReceiver) reporter );

        TestNGDirectoryTestSuite.startTestSuite( reporter, this );
        TestNGExecutor.run( this.suiteFilePaths, this.testSourceDirectory, this.options, reporter, this,
                            reportsDirectory );
        TestNGDirectoryTestSuite.finishTestSuite( reporter, this );
    }

    public void execute( String testSetName, ReporterFactory reporterManagerFactory )
        throws TestSetFailedException
    {
        throw new TestSetFailedException( "Cannot run individual test when suite files are specified" );
    }

    public Map locateTestSets( ClassLoader classLoader )
        throws TestSetFailedException
    {
        if ( testSets != null )
        {
            throw new IllegalStateException( "You can't call locateTestSets twice" );
        }

        if ( this.suiteFiles == null )
        {
            throw new IllegalStateException( "No suite files were specified" );
        }

        this.testSets = new HashMap<File, String>();
        this.suiteFilePaths = new ArrayList<String>();

        for ( Object suiteFile : suiteFiles )
        {
            File file = (File) suiteFile;
            if ( !file.exists() || !file.isFile() )
            {
                throw new TestSetFailedException( "Suite file " + file + " is not a valid file" );
            }
            this.testSets.put( file, file.getAbsolutePath() );
            this.suiteFilePaths.add( file.getAbsolutePath() );
        }

        return this.testSets;
    }

    public String getSuiteName()
    {
        String result = options.get( "suitename" );
        return result == null ? "TestSuite" : result;
    }
}
