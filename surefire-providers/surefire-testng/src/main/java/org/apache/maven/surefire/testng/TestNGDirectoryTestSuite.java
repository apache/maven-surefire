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
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.surefire.cli.CommandLineOption;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.testset.TestListResolver;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.TestsToRun;

import static org.apache.maven.surefire.testng.TestNGExecutor.run;
import static org.apache.maven.surefire.util.internal.StringUtils.isBlank;

/**
 * Test suite for TestNG based on a directory of Java test classes. Can also execute JUnit tests.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href='mailto:the[dot]mindstorm[at]gmail[dot]com'>Alex Popescu</a>
 */
final class TestNGDirectoryTestSuite
    extends TestSuite
{
    private final Map<String, String> options;

    private final Map<String, String> junitOptions;

    private final String testSourceDirectory;

    private final File reportsDirectory;

    private final TestListResolver methodFilter;

    private final Class<?> junitTestClass;

    private final Class<? extends Annotation> junitRunWithAnnotation;

    private final Class<? extends Annotation> junitTestAnnotation;

    private final List<CommandLineOption> mainCliOptions;

    private final int skipAfterFailureCount;

    TestNGDirectoryTestSuite( String testSourceDirectory, Map<String, String> confOptions, File reportsDirectory,
                              TestListResolver methodFilter, List<CommandLineOption> mainCliOptions,
                              int skipAfterFailureCount )
    {
        this.options = confOptions;
        this.testSourceDirectory = testSourceDirectory;
        this.reportsDirectory = reportsDirectory;
        this.methodFilter = methodFilter;
        this.junitTestClass = findJUnitTestClass();
        this.junitRunWithAnnotation = findJUnitRunWithAnnotation();
        this.junitTestAnnotation = findJUnitTestAnnotation();
        this.junitOptions = createJUnitOptions();
        this.mainCliOptions = mainCliOptions;
        this.skipAfterFailureCount = skipAfterFailureCount;
    }

    void execute( TestsToRun testsToRun, RunListener reporterManager )
        throws TestSetFailedException
    {
        if ( !testsToRun.allowEagerReading() )
        {
            executeLazy( testsToRun, reporterManager );
        }
        else if ( testsToRun.containsAtLeast( 2 ) )
        {
            executeMulti( testsToRun, reporterManager );
        }
        else if ( testsToRun.containsAtLeast( 1 ) )
        {
            Class<?> testClass = testsToRun.iterator().next();
            executeSingleClass( reporterManager, testClass );
        }
    }

    private void executeSingleClass( RunListener reporter, Class<?> testClass )
        throws TestSetFailedException
    {
        options.put( "suitename", testClass.getName() );

        startTestSuite( reporter );

        Map<String, String> optionsToUse = isJUnitTest( testClass ) ? junitOptions : options;

        run( Collections.<Class<?>>singleton( testClass ), testSourceDirectory, optionsToUse, reporter,
                reportsDirectory, methodFilter, mainCliOptions, skipAfterFailureCount );

        finishTestSuite( reporter );
    }

    private void executeLazy( TestsToRun testsToRun, RunListener reporterManager )
        throws TestSetFailedException
    {
        for ( Class<?> testToRun : testsToRun )
        {
            executeSingleClass( reporterManager, testToRun );
        }
    }

    private static Class<?> findJUnitTestClass()
    {
        return lookupClass( "junit.framework.Test" );
    }

    private static Class<Annotation> findJUnitRunWithAnnotation()
    {
        return lookupAnnotation( "org.junit.runner.RunWith" );
    }

    private static Class<Annotation> findJUnitTestAnnotation()
    {
        return lookupAnnotation( "org.junit.Test" );
    }

    @SuppressWarnings( "unchecked" )
    private static Class<Annotation> lookupAnnotation( String className )
    {
        try
        {
            return (Class<Annotation>) Class.forName( className );
        }
        catch ( ClassNotFoundException e )
        {
            return null;
        }
    }

    private static Class<?> lookupClass( String className )
    {
        try
        {
            return Class.forName( className );
        }
        catch ( ClassNotFoundException e )
        {
            return null;
        }
    }

    private void executeMulti( TestsToRun testsToRun, RunListener reporterManager )
        throws TestSetFailedException
    {
        List<Class<?>> testNgTestClasses = new ArrayList<Class<?>>();
        List<Class<?>> junitTestClasses = new ArrayList<Class<?>>();
        for ( Class<?> testToRun : testsToRun )
        {
            if ( isJUnitTest( testToRun ) )
            {
                junitTestClasses.add( testToRun );
            }
            else
            {
                testNgTestClasses.add( testToRun );
            }
        }

        File testNgReportsDirectory = reportsDirectory, junitReportsDirectory = reportsDirectory;

        if ( !junitTestClasses.isEmpty() && !testNgTestClasses.isEmpty() )
        {
            testNgReportsDirectory = new File( reportsDirectory, "testng-native-results" );
            junitReportsDirectory = new File( reportsDirectory, "testng-junit-results" );
        }
        startTestSuite( reporterManager );

        run( testNgTestClasses, testSourceDirectory, options, reporterManager,
                testNgReportsDirectory, methodFilter, mainCliOptions, skipAfterFailureCount );

        if ( !junitTestClasses.isEmpty() )
        {
            run( junitTestClasses, testSourceDirectory, junitOptions, reporterManager,
                    junitReportsDirectory, methodFilter, mainCliOptions, skipAfterFailureCount );
        }

        finishTestSuite( reporterManager );
    }

    private boolean isJUnitTest( Class<?> c )
    {
        return isJunit3Test( c ) || isJunit4Test( c );
    }

    private boolean isJunit4Test( Class<?> c )
    {
        return hasJunit4RunWithAnnotation( c ) || hasJunit4TestAnnotation( c );
    }

    private boolean hasJunit4RunWithAnnotation( Class<?> c )
    {
        return junitRunWithAnnotation != null && c.getAnnotation( junitRunWithAnnotation ) != null;
    }

    private boolean hasJunit4TestAnnotation( Class<?> c )
    {
        if ( junitTestAnnotation != null )
        {
            for ( Method m : c.getMethods() )
            {
                if ( m.getAnnotation( junitTestAnnotation ) != null )
                {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isJunit3Test( Class<?> c )
    {
        return junitTestClass != null && junitTestClass.isAssignableFrom( c );
    }

    private Map<String, String> createJUnitOptions()
    {
        Map<String, String> junitOptions = new HashMap<>( options );
        String onlyJUnit = options.get( "junit" );
        if ( isBlank( onlyJUnit ) )
        {
            onlyJUnit = "true";
        }
        junitOptions.put( "junit", onlyJUnit );
        return junitOptions;
    }

    @Override
    Map<String, String> getOptions()
    {
        return options;
    }
}
