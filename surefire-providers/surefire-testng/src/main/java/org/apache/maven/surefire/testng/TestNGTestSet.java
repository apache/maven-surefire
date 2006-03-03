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

import org.apache.maven.surefire.Surefire;
import org.apache.maven.surefire.report.ReporterManager;
import org.apache.maven.surefire.testset.AbstractTestSet;
import org.testng.TestNG;
import org.testng.internal.TestNGClassFinder;
import org.testng.internal.Utils;
import org.testng.internal.annotations.IAnnotationFinder;
import org.testng.internal.annotations.JDK14AnnotationFinder;
import org.testng.internal.annotations.JDK15AnnotationFinder;
import org.testng.xml.ClassSuite;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Main plugin point for running testng tests within the Surefire runtime
 * infrastructure.
 *
 * @author jkuhnert
 */
public class TestNGTestSet
    extends AbstractTestSet
{
    private String testSourceDirectory;

    private Class testClass;

    private static IAnnotationFinder annotationFinder;

    /**
     * Creates a new test testset that will process the class being
     * passed in to determine the testing configuration.
     */
    public TestNGTestSet( Class testClass )
    {
        this.testClass = testClass;

/*
        this.testSourceDirectory = testSourceDirectory;
*/
    }

    public Class getTestClass()
    {
        return testClass;
    }

    protected void discoverTestMethods()
    {
        if ( testMethods == null )
        {
            testMethods = new ArrayList();

            Method[] methods = testClass.getMethods();

            for ( int i = 0; i < methods.length; ++i )
            {
                Method m = methods[i];

                if ( isValidTestMethod( m ) )
                {
                    String simpleName = m.getName();

                    // TODO: WHY?
                    // name must have 5 or more chars
                    if ( simpleName.length() > 4 )
                    {
                        testMethods.add( m );
                    }
                }
            }
        }
    }

    public void execute( ReporterManager reportManager, ClassLoader loader )
    {
        // TODO: maybe don't execute this for every testset

        TestNG testNG = new TestNG();
        List classes = new ArrayList();
        classes.add( testClass );

        String groups = null;

        if ( !TestNGClassFinder.isTestNGClass( testClass, getAnnotationFinder() ) )
        {
//            testNG.setJUnit( Boolean.TRUE );
        }

        //configure testng parameters
        ClassSuite classSuite = new ClassSuite( groups != null ? groups : "TestNG Suite", Utils.classesToXmlClasses(
            (Class[]) classes.toArray( new Class[classes.size()] ) ) );
        testNG.setCommandLineSuite( classSuite );
        // TODO
//        testNG.setOutputDirectory( reportManager.getReportsDirectory() );
        Surefire surefire = new Surefire(); // TODO: blatently wrong
        TestNGReporter testngReporter = new TestNGReporter( reportManager, surefire );
/* TODO
        testNG.addListener( (ITestListener) testngReporter );
        testNG.addListener( (ISuiteListener) testngReporter );
*/

        // TODO: maybe this was running junit tests for us so that parallel would work
//        testNG.setThreadCount( threadCount );
//        testNG.setParallel( parallel );
//
//        if ( groups != null )
//        {
//            testNG.setGroups( groups );
//        }
//        if ( excludedGroups != null )
//        {
//            testNG.setExcludedGroups( excludedGroups );
//        }

        //set source path so testng can find javadoc
        //annotations if not in 1.5 jvm
        if ( /* TODO - necessary? !jvm15  && */ testSourceDirectory != null )
        {
            testNG.setSourcePath( testSourceDirectory );
        }

        //actually runs all the tests
        List result = testNG.runSuitesLocally();
//        nbTests += result.size(); TODO
    }

    private static IAnnotationFinder getAnnotationFinder()
    {
        // TODO: is this right? isn't it dependant on the version of the TestNG JAR being used?
        if ( annotationFinder == null )
        {
            if ( System.getProperty( "java.version" ).indexOf( "1.5" ) > -1 )
            {
                annotationFinder = new JDK15AnnotationFinder();
            }
            else
            {
                annotationFinder = new JDK14AnnotationFinder();
            }
        }
        return annotationFinder;
    }
}
