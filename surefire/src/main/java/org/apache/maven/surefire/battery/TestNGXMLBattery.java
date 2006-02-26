/**
 * 
 */
package org.apache.maven.surefire.battery;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.surefire.report.ReporterManager;
import org.apache.maven.surefire.report.TestNGReporter;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.TestNG;
import org.testng.xml.Parser;
import org.testng.xml.XmlSuite;
import org.xml.sax.SAXException;


/**
 * Handles suite xml file definitions for TestNG.
 * 
 * @author jkuhnert
 */
public class TestNGXMLBattery implements Battery {
    
    protected File suiteFile;
    protected TestNG testRunner;
    protected int m_testCount = 0;
    protected String m_suiteName = "TestNG XML Suite";
    
    /**
     * Creates a testng battery to be configured by the specified
     * xml file.
     */
    public TestNGXMLBattery(File suiteFile)
    {
        this.suiteFile = suiteFile;
        parseSuite();
    }
    
    public void discoverBatteryClassNames() throws Exception {
    }
    
    public void execute(ReporterManager reportManager) throws Exception {
        testRunner.runSuitesLocally();
    }
    
    public String getBatteryName() {
        return m_suiteName;
    }
    
    public List getSubBatteryClassNames()
    {
        return Collections.EMPTY_LIST;
    }
    
    public int getTestCount() {
        return m_testCount;
    }
    
    public void setOutputDirectory(String reportsDirectory)
    {
        testRunner.setOutputDirectory(reportsDirectory);
    }
    
    public void setReporter(TestNGReporter reporter)
    {
        testRunner.addListener((ITestListener)reporter);
        testRunner.addListener((ISuiteListener)reporter);
    }
    
    /**
     * Instantiates and partially configures testng suite
     */
    protected void parseSuite()
    {
        testRunner = new TestNG();
        List suites = new ArrayList();
        
        try {
            XmlSuite s = new Parser(suiteFile.getAbsolutePath()).parse();
            m_suiteName = s.getName();
            m_testCount += s.getTests().size();
            suites.add(s);
        }
        catch(FileNotFoundException fne) {
            System.err.println("File not found: " + suiteFile.getAbsolutePath() + " Ignoring.");
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        catch(ParserConfigurationException e) {
            e.printStackTrace();
        }
        catch(SAXException e) {
            e.printStackTrace();
        }
        
        testRunner.setXmlSuites(suites);
        // TODO: return when TestNG brings it back
//        testRunner.setReportResults(false);
    }
}
