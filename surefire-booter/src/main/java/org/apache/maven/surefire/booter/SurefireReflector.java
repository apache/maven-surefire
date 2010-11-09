package org.apache.maven.surefire.booter;
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

import org.apache.maven.surefire.Surefire;
import org.apache.maven.surefire.testset.TestSetFailedException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;

/**
 * Does reflection based invocation of the surefire methods.
 * <p/>
 * This is to avoid compilications with linkage issues
 *
 * @author Kristian Rosenvold
 */
public class SurefireReflector
{

    private final Class surefireClass;

    public SurefireReflector( ClassLoader surefirClassLoader )
        throws SurefireExecutionException
    {
        try
        {
            surefireClass = surefirClassLoader.loadClass( Surefire.class.getName() );
        }
        catch ( ClassNotFoundException e )
        {
            throw new SurefireExecutionException( "When loading class", e );
        }
    }


    public int run( List reportDefinitions, Object[] testSuiteDefinition, String testSetName,
                    ClassLoader surefireClassLoader, ClassLoader testsClassLoader, Properties results,
                    Boolean failIfNoTests )
        throws SurefireExecutionException
    {
        Object surefire = instantiateSurefire();
        Method run = getRunMethod(
            new Class[]{ List.class, Object[].class, String.class, ClassLoader.class, ClassLoader.class,
                Properties.class, Boolean.class } );

        return invokeRunMethod( surefire, run,
                                new Object[]{ reportDefinitions, testSuiteDefinition, testSetName, surefireClassLoader,
                                    testsClassLoader, results, failIfNoTests } );
    }

    public int run( List reportDefinitions, List testSuiteDefinitions, ClassLoader surefireClassLoader,
                    ClassLoader testsClassLoader, Boolean failIfNoTests )
        throws SurefireExecutionException
    {
        Object surefire = instantiateSurefire();

        Method run =
            getRunMethod( new Class[]{ List.class, List.class, ClassLoader.class, ClassLoader.class, Boolean.class } );

        Thread.currentThread().setContextClassLoader( testsClassLoader );

        return invokeRunMethod( surefire, run,
                                new Object[]{ reportDefinitions, testSuiteDefinitions, surefireClassLoader,
                                    testsClassLoader, failIfNoTests } );
    }

    public static Object instantiateObject( String className, Object[] params, ClassLoader classLoader )
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


    private int invokeRunMethod( Object surefire, Method run, Object[] args )
        throws SurefireExecutionException

    {
        try
        {
            final Integer invoke = (Integer) run.invoke( surefire, args );
            return invoke.intValue();
        }
        catch ( IllegalAccessException e )
        {
            throw new SurefireExecutionException( "When instantiating surefire", e );
        }
        catch ( InvocationTargetException e )
        {
            throw new SurefireExecutionException( e.getTargetException().getMessage(), e.getTargetException() );
        }

    }

    private Object instantiateSurefire()
        throws SurefireExecutionException
    {
        try
        {
            return surefireClass.newInstance();
        }
        catch ( InstantiationException e )
        {
            throw new SurefireExecutionException( "When instanitating surefire", e );
        }
        catch ( IllegalAccessException e )
        {
            throw new SurefireExecutionException( "When instanitating surefire", e );
        }
    }

    private Method getRunMethod( Class[] parameters )
        throws SurefireExecutionException
    {
        try
        {
            return surefireClass.getMethod( "run", parameters );
        }
        catch ( NoSuchMethodException e )
        {
            throw new SurefireExecutionException( "When finding run method", e );
        }
    }


}
