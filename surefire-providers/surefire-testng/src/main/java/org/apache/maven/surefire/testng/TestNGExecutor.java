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
import org.testng.xml.XmlSuite;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Contains utility methods for executing TestNG.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class TestNGExecutor
{
    private TestNGExecutor()
    {
    }
    
    static void executeTestNG( SurefireTestSuite surefireSuite, String testSourceDirectory, XmlSuite suite,
                               ReporterManager reporterManager )
    {
        TestNG testNG = new TestNG( false );
        
        // turn off all TestNG output
        testNG.setVerbose( 0 );
        
        testNG.setXmlSuites( Collections.singletonList( suite ) );
        
        testNG.setListenerClasses( new ArrayList() );
        
        TestNGReporter reporter = new TestNGReporter( reporterManager, surefireSuite );
        testNG.addListener( (ITestListener) reporter );
        testNG.addListener( (ISuiteListener) reporter );
        
        // Set source path so testng can find javadoc annotations if not in 1.5 jvm
        if ( testSourceDirectory != null )
        {
            testNG.setSourcePath( testSourceDirectory );
        }
        
        // TODO: Doesn't find testng.xml based suites when these are un-commented
        // TestNG ~also~ looks for the currentThread context classloader
        // ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        // Thread.currentThread().setContextClassLoader( suite.getClass().getClassLoader() );
        testNG.runSuitesLocally();
        //Thread.currentThread().setContextClassLoader( oldClassLoader );
    }
}
