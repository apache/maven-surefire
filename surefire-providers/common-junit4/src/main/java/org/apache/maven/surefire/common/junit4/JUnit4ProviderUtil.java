package org.apache.maven.surefire.common.junit4;

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

import org.apache.maven.surefire.util.TestsToRun;
import org.apache.maven.surefire.util.internal.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;

import static org.apache.maven.surefire.common.junit4.JUnit4RunListener.isFailureInsideJUnitItself;

/**
 *
 * Utility method used among all JUnit4 providers
 *
 * @author Qingzhou Luo
 *
 */
public class JUnit4ProviderUtil
{
    /**
     * Organize all the failures in previous run into a map between test classes and corresponding failing test methods
     *
     * @param allFailures all the failures in previous run
     * @param testsToRun  all the test classes
     * @return a map between failing test classes and their corresponding failing test methods
     */
    public static Map<Class<?>, Set<String>> generateFailingTests( List<Failure> allFailures, TestsToRun testsToRun )
    {
        Map<Class<?>, Set<String>> testClassMethods = new HashMap<Class<?>, Set<String>>();

        for ( Failure failure : allFailures )
        {
            if ( !isFailureInsideJUnitItself( failure ) )
            {
                // failure.getTestHeader() is in the format: method(class)
                String[] testMethodClass = StringUtils.split( failure.getTestHeader(), "(" );
                String testMethod = testMethodClass[0];
                String testClass = StringUtils.split( testMethodClass[1], ")" )[0];
                Class testClassObj = testsToRun.getClassByName( testClass );

                if ( testClassObj == null )
                {
                    continue;
                }

                Set<String> failingMethods = testClassMethods.get( testClassObj );
                if ( failingMethods == null )
                {
                    failingMethods = new HashSet<String>();
                    failingMethods.add( testMethod );
                    testClassMethods.put( testClassObj, failingMethods );
                }
                else
                {
                    failingMethods.add( testMethod );
                }
            }
        }
        return testClassMethods;
    }

    /**
     * Get the name of all test methods from a list of Failures
     *
     * @param allFailures the list of failures for a given test class
     * @return the list of test method names
     */
    public static Set<String> generateFailingTests( List<Failure> allFailures )
    {
        Set<String> failingMethods = new HashSet<String>();

        for ( Failure failure : allFailures )
        {
            if ( !isFailureInsideJUnitItself( failure ) )
            {
                // failure.getTestHeader() is in the format: method(class)
                String testMethod = StringUtils.split( failure.getTestHeader(), "(" )[0];
                failingMethods.add( testMethod );
            }
        }
        return failingMethods;
    }

    public static Description createSuiteDescription( Collection<Class<?>> classes )
    {
        JUnit4Reflector reflector = new JUnit4Reflector();
        return reflector.createRequest( classes.toArray( new Class[classes.size()] ) )
                .getRunner()
                .getDescription();
    }

}
