package org.apache.maven.surefire.testset;

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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Base test class.
 */
public abstract class AbstractTestSet
    implements SurefireTestSet
{
    private static final String TEST_METHOD_PREFIX = "test";

    protected List testMethods;

    private Class testClass;

    protected AbstractTestSet( Class testClass )
    {
        if ( testClass == null )
        {
            throw new NullPointerException( "testClass is null" );
        }

        this.testClass = testClass;
    }

    public String getName()
    {
        return testClass.getName();
    }

    public int getTestCount()
        throws TestSetFailedException
    {
        discoverTestMethods();

        return testMethods.size();
    }

    protected Class getTestClass()
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

                    // name must have 5 or more chars
                    if ( simpleName.length() > 4 )
                    {
                        String firstFour = simpleName.substring( 0, 4 );

                        // name must start with "test"
                        if ( firstFour.equals( TEST_METHOD_PREFIX ) )
                        {
                            testMethods.add( m );
                        }
                    }
                }
            }
        }
    }

    public static boolean isValidTestMethod( Method m )
    {
        boolean isInstanceMethod = !Modifier.isStatic( m.getModifiers() );

        boolean returnsVoid = m.getReturnType().equals( void.class );

        boolean hasNoParams = m.getParameterTypes().length == 0;

        return isInstanceMethod && returnsVoid && hasNoParams;
    }
}
