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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.surefire.util.internal.ClassMethod;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.Failure;

import static org.apache.maven.surefire.util.internal.TestClassMethodNameUtils.extractClassName;
import static org.apache.maven.surefire.util.internal.TestClassMethodNameUtils.extractMethodName;
import static org.junit.runner.Description.TEST_MECHANISM;

/**
 *
 * Utility method used among all JUnit4 providers
 *
 * @author Qingzhou Luo
 *
 */
public final class JUnit4ProviderUtil
{
    private JUnit4ProviderUtil()
    {
        throw new IllegalStateException( "Cannot instantiate." );
    }

    /**
     * Get all descriptions from a list of Failures
     *
     * @param allFailures the list of failures for a given test class
     * @return the list of descriptions
     */
    public static Set<Description> generateFailingTestDescriptions( List<Failure> allFailures )
    {
        Set<Description> failingTestDescriptions = new HashSet<>();

        for ( Failure failure : allFailures )
        {
            Description description = failure.getDescription();
            if ( description.isTest() && !isFailureInsideJUnitItself( description ) )
            {
                failingTestDescriptions.add( description );
            }
        }
        return failingTestDescriptions;
    }

    public static boolean isFailureInsideJUnitItself( Description failure )
    {
        return TEST_MECHANISM.equals( failure );
    }

    /**
     * Java Patterns of regex is slower than cutting a substring.
     * @param description method(class) or method[#](class) or method[#whatever-literals](class)
     * @return method JUnit test method
     */
    public static ClassMethod toClassMethod( Description description )
    {
        String clazz = extractClassName( description.getDisplayName() );
        if ( clazz == null || isInsaneJunitNullString( clazz ) )
        {
            // This can happen upon early failures (class instantiation error etc)
            Iterator<Description> it = description.getChildren().iterator();
            if ( it.hasNext() )
            {
                description = it.next();
                clazz = extractClassName( description.getDisplayName() );
            }
            if ( clazz == null )
            {
                clazz = "Test Instantiation Error";
            }
        }
        String method = extractMethodName( description.getDisplayName() );
        return new ClassMethod( clazz, method );
    }

    private static boolean isInsaneJunitNullString( String value )
    {
        return "null".equals( value );
    }

    public static Filter createMatchAnyDescriptionFilter( Iterable<Description> descriptions )
    {
        return new MatchDescriptions( descriptions );
    }
}
