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
import org.apache.maven.surefire.testset.AbstractTestSet;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Main plugin point for running testng tests within the Surefire runtime
 * infrastructure.
 *
 * @author jkuhnert
 */
public class TestNGTestSet
    extends AbstractTestSet
{
    /**
     * Creates a new test testset that will process the class being
     * passed in to determine the testing configuration.
     */
    public TestNGTestSet( Class testClass )
    {
        super( testClass );
    }

    protected void discoverTestMethods()
    {
        if ( testMethods == null )
        {
            testMethods = new ArrayList();

            Class testClass = getTestClass();
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
        throw new UnsupportedOperationException(
            "This should have been called directly from TestNGDirectoryTestSuite" );
    }
}
