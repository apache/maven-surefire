package org.apache.maven.surefire.common.junit48;

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

import org.apache.maven.shared.utils.io.SelectorUtils;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import java.util.Map;
import java.util.Set;

/**
 * Only run test methods in the given input map, indexed by test class
 */
final class FailingMethodFilter
    extends Filter
{
    // Map from Class -> List of method names. Are the method names hashed to include the signature?
    private final Map<Class<?>, Set<String>> failingClassMethodPatterns;

    public FailingMethodFilter( Map<Class<?>, Set<String>> failingClassMethodPatterns )
    {
        this.failingClassMethodPatterns = failingClassMethodPatterns;
    }

    @Override
    public boolean shouldRun( Description description )
    {
        return isDescriptionMatch( description );
    }

    private boolean isDescriptionMatch( Description description )
    {
        if ( description.getTestClass() == null || description.getMethodName() == null )
        {
            for ( Description childrenDescription : description.getChildren() )
            {
                if ( isDescriptionMatch( childrenDescription ) )
                {
                    return true;
                }
            }
            return false;
        }

        Set<String> testMethodPatterns = failingClassMethodPatterns.get( description.getTestClass() );
        String testMethod = description.getMethodName();
        return testMethodPatterns != null && matchMethod( testMethodPatterns, testMethod );
    }

    private static boolean matchMethod( Set<String> patterns, String methodName )
    {
        for ( String pattern : patterns )
        {
            if ( SelectorUtils.match( pattern, methodName ) )
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public String describe()
    {
        return "By failing class method";
    }
}