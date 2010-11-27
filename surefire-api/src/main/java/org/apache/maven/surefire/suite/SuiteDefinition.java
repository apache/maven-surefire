package org.apache.maven.surefire.suite;
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

import org.apache.maven.surefire.testset.TestSetFailedException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

// todo: Remove once we build with 2.7
public class SuiteDefinition
{
    private final String suiteClassName;

    private final Object[] params;

    public SuiteDefinition( String suiteClassName, Object[] params )
    {
        this.suiteClassName = suiteClassName;
        this.params = params;
    }


    public SurefireTestSuite newInstance( ClassLoader surefireClassLoader )
        throws TestSetFailedException
    {

        return instantiateSuite( suiteClassName, params, surefireClassLoader );
    }

    public List asBooterFormat()
    {
        return Collections.singletonList( new Object[]{ suiteClassName, params } );
    }

    public static SuiteDefinition fromBooterFormat( List testSuiteDefinitions )
    {
        if ( testSuiteDefinitions.size() != 1 )
        {
            throw new IllegalStateException( "No more than 1 SuiteDefinition supported" );
        }
        Object[] item = (Object[]) testSuiteDefinitions.get( 0 );
        String suiteClassName1 = (String) item[0];
        return new SuiteDefinition( suiteClassName1, (Object[]) item[1] );
    }

    // This reflection is not due to linkage issues, but only an attempt at being generic.
    private static Object instantiateObject( String className, Object[] params, ClassLoader classLoader )
        throws TestSetFailedException, ClassNotFoundException, NoSuchMethodException
    {
        Class clazz = classLoader.loadClass( className );

        Object object;
        try
        {
            if ( params != null )
            {
                Class[] paramTypes = new Class[params.length];

                for ( int j = 0; j < params.length; j++ )
                {
                    if ( params[j] == null )
                    {
                        paramTypes[j] = String.class;
                    }
                    else
                    {
                        paramTypes[j] = params[j].getClass();
                    }
                }

                Constructor constructor = clazz.getConstructor( paramTypes );

                object = constructor.newInstance( params );
            }
            else
            {
                object = clazz.newInstance();
            }
        }
        catch ( IllegalAccessException e )
        {
            throw new TestSetFailedException( "Unable to instantiate object: " + e.getMessage(), e );
        }
        catch ( InvocationTargetException e )
        {
            throw new TestSetFailedException( e.getTargetException().getMessage(), e.getTargetException() );
        }
        catch ( InstantiationException e )
        {
            throw new TestSetFailedException( "Unable to instantiate object: " + e.getMessage(), e );
        }
        return object;
    }

    // This reflection is not due to linkage issues, but only an attempt at being generic.
    private static SurefireTestSuite instantiateSuite( String suiteClass, Object[] params, ClassLoader classLoader )
        throws TestSetFailedException
    {
        try
        {
            return (SurefireTestSuite) instantiateObject( suiteClass, params, classLoader );
        }
        catch ( ClassNotFoundException e )
        {
            throw new TestSetFailedException( "Unable to find class to create suite '" + suiteClass + "'", e );
        }
        catch ( NoSuchMethodException e )
        {
            throw new TestSetFailedException(
                "Unable to find appropriate constructor to create suite: " + e.getMessage(), e );
        }
    }
}
