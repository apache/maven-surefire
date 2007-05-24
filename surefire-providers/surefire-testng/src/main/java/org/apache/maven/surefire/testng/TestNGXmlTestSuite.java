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

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.surefire.report.ReporterManager;
import org.apache.maven.surefire.suite.SurefireTestSuite;
import org.apache.maven.surefire.testset.TestSetFailedException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Handles suite xml file definitions for TestNG.
 *
 * @author jkuhnert
 * @author <a href='mailto:the[dot]mindstorm[at]gmail[dot]com'>Alex Popescu</a>
 */
public class TestNGXmlTestSuite
    implements SurefireTestSuite
{
    private File[] suiteFiles;

    private List suiteFilePaths;

    private String testSourceDirectory;

    private ArtifactVersion version;

    private Map options = new HashMap();

    // Not really used
    private Map testSets;

    /**
     * Creates a testng testset to be configured by the specified
     * xml file.
     */
    public TestNGXmlTestSuite( File[] suiteFiles, String testSourceDirectory, String artifactVersion )
    {
        this.suiteFiles = suiteFiles;

        this.version = new DefaultArtifactVersion( artifactVersion );

        this.testSourceDirectory = testSourceDirectory;
    }

    public void execute( ReporterManager reporterManager, ClassLoader classLoader )
    {
        if ( testSets == null )
        {
            throw new IllegalStateException( "You must call locateTestSets before calling execute" );
        }

        TestNGExecutor.run( this.suiteFilePaths, this.testSourceDirectory, this.options, this.version, reporterManager,
                            this );
    }

    public void execute( String testSetName, ReporterManager reporterManager, ClassLoader classLoader )
        throws TestSetFailedException
    {
        throw new TestSetFailedException( "Cannot run individual test when suite files are specified" );
    }

    public int getNumTests()
    {
        // TODO: this is not correct
        return suiteFiles.length;
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

        this.testSets = new HashMap();
        this.suiteFilePaths = new ArrayList();

        for ( Iterator i = Arrays.asList( suiteFiles ).iterator(); i.hasNext(); )
        {
            File file = (File) i.next();
            if ( !file.exists() || !file.isFile() )
            {
                throw new TestSetFailedException( "Suite file " + file + " is not a valid file" );
            }
            this.testSets.put( file, file.getAbsolutePath() );
            this.suiteFilePaths.add( file.getAbsolutePath() );
        }

        return this.testSets;
    }
}
