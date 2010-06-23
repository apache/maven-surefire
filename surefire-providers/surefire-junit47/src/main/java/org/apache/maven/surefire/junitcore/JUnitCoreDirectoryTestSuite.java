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

package org.apache.maven.surefire.junitcore;

import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterManagerFactory;
import org.apache.maven.surefire.suite.SurefireTestSuite;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.SurefireDirectoryScanner;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

/**
 * Test suite for JUnitCore based on a directory of Java test classes.
 *
 * @author Karl M. Davis
 * @author Kristian Rosenvold (junit core adaption)
 */
@SuppressWarnings( { "UnusedDeclaration" } )
public class JUnitCoreDirectoryTestSuite
    implements SurefireTestSuite
{
    private final SurefireDirectoryScanner directoryScanner;

    private TestsToRun testsToRun;

    protected Map testSets;

    private final JUnitCoreParameters jUnitCoreParameters;


    public JUnitCoreDirectoryTestSuite( File basedir, ArrayList includes, ArrayList excludes, Properties properties )
    {
        directoryScanner = new SurefireDirectoryScanner( basedir, includes, excludes );
        this.jUnitCoreParameters = new JUnitCoreParameters( properties );
    }


    public void execute( ReporterManagerFactory reporterManagerFactory, ClassLoader classLoader )
        throws ReporterException, TestSetFailedException
    {
        if ( testsToRun == null )
        {
            throw new IllegalStateException( "You must call locateTestSets before calling execute" );
        }

        JUnitCoreTestSet.execute( testsToRun.getLocatedClasses(), reporterManagerFactory, jUnitCoreParameters );
    }

    public void execute( String testSetName, ReporterManagerFactory reporterManagerFactory, ClassLoader classLoader )
        throws ReporterException, TestSetFailedException
    {
        if ( testsToRun == null )
        {
            throw new IllegalStateException( "You must call locateTestSets before calling execute" );
        }
        JUnitCoreTestSet testSet = testsToRun.getTestSet( testSetName );


        if ( testSet == null )
        {
            throw new TestSetFailedException( "Unable to find test set '" + testSetName + "' in suite" );
        }
        testSet.execute( reporterManagerFactory, jUnitCoreParameters );
    }

    public Map locateTestSets( ClassLoader classLoader )
        throws TestSetFailedException
    {
        if ( testSets != null )
        {
            throw new IllegalStateException( "You can't call locateTestSets twice" );
        }

        Class[] locatedClasses = directoryScanner.locateTestClasses( classLoader );
        testsToRun = new TestsToRun( locatedClasses );
        return testsToRun.getTestSets();
    }

    public int getNumTests()
    {
        if ( testsToRun == null )
        {
            throw new IllegalStateException( "You must call locateTestSets before calling getNumTests" );
        }
        return testsToRun.size();
    }
}
