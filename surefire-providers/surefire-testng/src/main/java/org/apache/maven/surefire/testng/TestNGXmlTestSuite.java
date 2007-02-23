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

import org.apache.maven.surefire.report.ReporterManager;
import org.apache.maven.surefire.suite.SurefireTestSuite;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.testng.xml.Parser;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles suite xml file definitions for TestNG.
 *
 * @author jkuhnert
 */
public class TestNGXmlTestSuite
    implements SurefireTestSuite
{
    private File suiteFile;

    private String testSourceDirectory;

    private XmlSuite suite;

    private Map testSets;

    /**
     * Creates a testng testset to be configured by the specified
     * xml file.
     */
    public TestNGXmlTestSuite( File suiteFile, String testSourceDirectory )
    {
        this.suiteFile = suiteFile;

        this.testSourceDirectory = testSourceDirectory;
    }

    public TestNGXmlTestSuite( File suiteFile )
    {
        this( suiteFile, null );
    }

    public void execute( ReporterManager reporterManager, ClassLoader classLoader )
    {
        if ( testSets == null )
        {
            throw new IllegalStateException( "You must call locateTestSets before calling execute" );
        }

        TestNGExecutor.executeTestNG( this, testSourceDirectory, suite, reporterManager );
    }

    public void execute( String testSetName, ReporterManager reporterManager, ClassLoader classLoader )
        throws TestSetFailedException
    {
        if ( testSets == null )
        {
            throw new IllegalStateException( "You must call locateTestSets before calling execute" );
        }
        XmlTest testSet = (XmlTest) testSets.get( testSetName );

        if ( testSet == null )
        {
            throw new TestSetFailedException( "Unable to find test set '" + testSetName + "' in suite" );
        }

        List originalTests = new ArrayList( suite.getTests() );
        for ( Iterator i = suite.getTests().iterator(); i.hasNext(); )
        {
            XmlTest test = (XmlTest) i.next();
            if ( !test.getName().equals( testSetName ) )
            {
                i.remove();
            }
        }
        TestNGExecutor.executeTestNG( this, testSourceDirectory, suite, reporterManager );

        suite.getTests().clear();
        suite.getTests().addAll( originalTests );
    }

    public int getNumTests()
    {
        // TODO: need to get this from TestNG somehow
        return 1;
    }

    public int getNumTestSets()
    {
        return suite.getTests().size();
    }

    public Map locateTestSets( ClassLoader classLoader )
        throws TestSetFailedException
    {
        if ( testSets != null )
        {
            throw new IllegalStateException( "You can't call locateTestSets twice" );
        }
        testSets = new LinkedHashMap();

        try
        {
            suite = new Parser( suiteFile.getAbsolutePath() ).parse();
        }
        catch ( IOException e )
        {
            throw new TestSetFailedException( "Error reading test suite", e );
        }
        catch ( ParserConfigurationException e )
        {
            throw new TestSetFailedException( "Error reading test suite", e );
        }
        catch ( SAXException e )
        {
            throw new TestSetFailedException( "Error reading test suite", e );
        }

        for ( Iterator i = suite.getTests().iterator(); i.hasNext(); )
        {
            XmlTest xmlTest = (XmlTest) i.next();

            if ( testSets.containsKey( xmlTest.getName() ) )
            {
                throw new TestSetFailedException( "Duplicate test set '" + xmlTest.getName() + "'" );
            }

            // We don't need to put real test sets in here, the key is the important part
            testSets.put( xmlTest.getName(), xmlTest );
        }
        return testSets;
    }
}
