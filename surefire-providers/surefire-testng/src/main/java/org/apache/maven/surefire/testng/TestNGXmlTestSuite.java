package org.apache.maven.surefire.testng;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.surefire.report.ReporterManager;
import org.apache.maven.surefire.suite.SurefireTestSuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.TestNG;
import org.testng.xml.Parser;
import org.testng.xml.XmlSuite;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
    protected File suiteFile;

    protected TestNG testRunner;

    protected int m_testCount;

    protected String m_suiteName = "TestNG XML Suite";

    /**
     * Creates a testng testset to be configured by the specified
     * xml file.
     */
    public TestNGXmlTestSuite( File suiteFile )
    {
        this.suiteFile = suiteFile;
        parseSuite();
    }

    public void execute( ReporterManager reportManager, ClassLoader loader )
    {
        testRunner.runSuitesLocally();
    }

    public void execute( String testSetName, ReporterManager reporterManager, ClassLoader testsClassLoader )
    {
        // TODO: if this can't be implemented here, I should have the booter recognise that and prevent it from getting
        // the pertest treatment
    }

    public int getNumTests()
    {
        // TODO
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getNumTestSets()
    {
        // TODO
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getName()
    {
        return m_suiteName;
    }

    public List getSubBatteries()
    {
        return Collections.EMPTY_LIST;
    }

    public int getTestCount()
    {
        return m_testCount;
    }

    public void setOutputDirectory( String reportsDirectory )
    {
        testRunner.setOutputDirectory( reportsDirectory );
    }

    public void setReporter( TestNGReporter reporter )
    {
        testRunner.addListener( (ITestListener) reporter );
        testRunner.addListener( (ISuiteListener) reporter );
    }

    /**
     * Instantiates and partially configures testng suite
     */
    private void parseSuite()
    {
        testRunner = new TestNG();
        List suites = new ArrayList();

        try
        {
            XmlSuite s = new Parser( suiteFile.getAbsolutePath() ).parse();
            m_suiteName = s.getName();
            m_testCount += s.getTests().size();
            suites.add( s );
        }
        catch ( FileNotFoundException fne )
        {
            System.err.println( "File not found: " + suiteFile.getAbsolutePath() + " Ignoring." );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
        catch ( ParserConfigurationException e )
        {
            e.printStackTrace();
        }
        catch ( SAXException e )
        {
            e.printStackTrace();
        }

        testRunner.setXmlSuites( suites );
        // TODO: return when TestNG brings it back
//        testRunner.setReportResults(false);
    }

    public Map locateTestSets( ClassLoader classLoader )
    {
        // TODO
        return Collections.EMPTY_MAP;
    }

}
