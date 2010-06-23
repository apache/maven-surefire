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

import org.apache.maven.surefire.testset.TestSetFailedException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains all the tests that have been found according to specified include/exclude
 * specification for a given surefire run.
 *
 * @author Kristian Rosenvold (junit core adaption)
 */
class TestsToRun
{
    final Class[] locatedClasses;

    final int totalTests;

    Map<String, JUnitCoreTestSet> testSets;

    public TestsToRun( Class... locatedClasses )
        throws TestSetFailedException
    {
        this.locatedClasses = locatedClasses;
        testSets = new HashMap<String, JUnitCoreTestSet>();
        int testCount = 0;
        for ( Class testClass : locatedClasses )
        {
            JUnitCoreTestSet testSet = new JUnitCoreTestSet( testClass );

            if ( testSets.containsKey( testSet.getName() ) )
            {
                throw new TestSetFailedException( "Duplicate test set '" + testSet.getName() + "'" );
            }
            testSets.put( testSet.getName(), testSet );
            testCount++;
        }
        this.totalTests = testCount;
    }

    public Map<String, JUnitCoreTestSet> getTestSets()
    {
        return Collections.unmodifiableMap( testSets );
    }

    public int size()
    {
        return testSets.size();
    }

    public Class[] getLocatedClasses()
    {
        return locatedClasses;
    }

    public JUnitCoreTestSet getTestSet( String name )
    {
        return testSets.get( name );
    }

}
