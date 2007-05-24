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
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterManager;
import org.apache.maven.surefire.suite.AbstractDirectoryTestSuite;
import org.apache.maven.surefire.testset.SurefireTestSet;
import org.apache.maven.surefire.testset.TestSetFailedException;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Test suite for TestNG based on a directory of Java test classes. Can also execute JUnit tests.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href='mailto:the[dot]mindstorm[at]gmail[dot]com'>Alex Popescu</a>
 */
public class TestNGDirectoryTestSuite
    extends AbstractDirectoryTestSuite
{
    private ArtifactVersion version;

    private Map options;

    private String testSourceDirectory;

    public TestNGDirectoryTestSuite( File basedir, List includes, List excludes, String testSourceDirectory,
                                     ArtifactVersion artifactVersion, Map confOptions )
    {
        super( basedir, includes, excludes );

        this.options = confOptions;
        this.testSourceDirectory = testSourceDirectory;
        this.version = artifactVersion;
    }

    protected SurefireTestSet createTestSet( Class testClass, ClassLoader classLoader )
    {
        return new TestNGTestSet( testClass );
    }

    // single class test
    public void execute( String testSetName, ReporterManager reporterManager, ClassLoader classLoader )
        throws ReporterException, TestSetFailedException
    {
        if ( testSets == null )
        {
            throw new IllegalStateException( "You must call locateTestSets before calling execute" );
        }
        SurefireTestSet testSet = (SurefireTestSet) testSets.get( testSetName );

        if ( testSet == null )
        {
            throw new TestSetFailedException( "Unable to find test set '" + testSetName + "' in suite" );
        }

        TestNGExecutor.run( new Class[]{testSet.getTestClass()}, this.testSourceDirectory, this.options, this.version,
                            reporterManager, this );
    }

    public void execute( ReporterManager reporterManager, ClassLoader classLoader )
        throws ReporterException, TestSetFailedException
    {
        if ( testSets == null )
        {
            throw new IllegalStateException( "You must call locateTestSets before calling execute" );
        }

        Class[] testClasses = new Class[testSets.size()];
        int i = 0;
        for ( Iterator it = testSets.values().iterator(); it.hasNext(); )
        {
            SurefireTestSet testSet = (SurefireTestSet) it.next();
            testClasses[i++] = testSet.getTestClass();
        }

        TestNGExecutor.run( testClasses, this.testSourceDirectory, this.options, this.version, reporterManager, this );
    }
}
