package org.apache.maven.surefire.junit;

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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.maven.surefire.api.report.LegacyPojoStackTraceWriter;
import org.apache.maven.surefire.api.report.SimpleReportEntry;
import org.apache.maven.surefire.api.report.StackTraceWriter;
import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.apache.maven.surefire.report.ClassMethodIndexer;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.surefire.api.report.RunMode.NORMAL_RUN;
import static org.apache.maven.surefire.api.report.SimpleReportEntry.withException;

/**
 * Executes a JUnit3 test class
 *
 */
public class PojoTestSetExecutor
    implements SurefireTestSetExecutor
{
    private static final String TEST_METHOD_PREFIX = "test";

    private static final String SETUP_METHOD_NAME = "setUp";

    private static final String TEARDOWN_METHOD_NAME = "tearDown";

    private static final Object[] EMPTY_OBJECT_ARRAY = {};

    private final JUnit3Reporter reporter;

    public PojoTestSetExecutor( JUnit3Reporter reporter )
    {
        this.reporter = requireNonNull( reporter, "reportManager is null" );
    }

    @Override
    public void execute( Class<?> testClass, ClassLoader loader )
            throws TestSetFailedException
    {
        DiscoveredTestMethods discoveredTestMethods = discoverTestMethods( requireNonNull( testClass ) );

        for ( Method testMethod : discoveredTestMethods.testMethods )
        {
            ClassMethodIndexer indexer = reporter.getClassMethodIndexer();
            long testRunId = indexer.indexClassMethod( testClass.getName(), testMethod.getName() );
            boolean abort = executeTestMethod( testClass, testMethod, testRunId, discoveredTestMethods );
            if ( abort )
            {
                break;
            }
        }
    }

    private boolean executeTestMethod( Class<?> testClass, Method method, long testRunId,
                                       DiscoveredTestMethods methods )
            throws TestSetFailedException
    {
        final Object testObject;

        try
        {
            testObject = testClass.getDeclaredConstructor().newInstance();
        }
        catch ( ReflectiveOperationException e )
        {
            throw new TestSetFailedException( "Unable to instantiate POJO '" + testClass + "'.", e );
        }

        final String testClassName = testClass.getName();
        final String methodName = method.getName();
        final String userFriendlyMethodName = methodName + "()";
        final String testName = getTestName( testClassName, userFriendlyMethodName );

        reporter.testStarting( new SimpleReportEntry( NORMAL_RUN, testRunId, testClassName, null, testName, null ) );

        try
        {
            setUpFixture( testObject, methods );
        }
        catch ( Throwable e )
        {
            StackTraceWriter stackTraceWriter = new LegacyPojoStackTraceWriter( testClassName, methodName, e );
            reporter.testFailed(
                withException( NORMAL_RUN, testRunId, testClassName, null, testName, null, stackTraceWriter ) );

            // A return value of true indicates to this class's executeTestMethods
            // method that it should abort and not attempt to execute
            // any other test methods. The other caller of this method,
            // TestRerunner.rerun, ignores this return value, because it is
            // only running one test.
            return true;
        }

        // Make sure that tearDownFixture
        try
        {
            method.invoke( testObject, EMPTY_OBJECT_ARRAY );
            reporter.testSucceeded(
                new SimpleReportEntry( NORMAL_RUN, testRunId, testClassName, null, testName, null ) );
        }
        catch ( InvocationTargetException e )
        {
            Throwable t = e.getTargetException();
            StackTraceWriter stackTraceWriter = new LegacyPojoStackTraceWriter( testClassName, methodName, t );
            reporter.testFailed(
                withException( NORMAL_RUN, testRunId, testClassName, null, testName, null, stackTraceWriter ) );
            // Don't return  here, because tearDownFixture should be called even
            // if the test method throws an exception.
        }
        catch ( Throwable t )
        {
            StackTraceWriter stackTraceWriter = new LegacyPojoStackTraceWriter( testClassName, methodName, t );
            reporter.testFailed(
                withException( NORMAL_RUN, testRunId, testClassName, null, testName, null, stackTraceWriter ) );
            // Don't return  here, because tearDownFixture should be called even
            // if the test method throws an exception.
        }

        try
        {
            tearDownFixture( testObject, methods );
        }
        catch ( Throwable t )
        {
            StackTraceWriter stackTraceWriter = new LegacyPojoStackTraceWriter( testClassName, methodName, t );
            // Treat any exception from tearDownFixture as a failure of the test.
            reporter.testFailed(
                withException( NORMAL_RUN, testRunId, testClassName, null, testName, null, stackTraceWriter ) );

            // A return value of true indicates to this class's executeTestMethods
            // method that it should abort and not attempt to execute
            // any other test methods. The other caller of this method,
            // TestRerunner.rerun, ignores this return value, because it is
            // only running one test.
            return true;
        }

        // A return value of false indicates to this class's executeTestMethods
        // method that it should keep plowing ahead and invoke more test methods.
        // The other caller of this method,
        // TestRerunner.rerun, ignores this return value, because it is
        // only running one test.
        return false;
    }

    private String getTestName( String testClassName, String testMethodName )
    {
        return testClassName + "." + requireNonNull( testMethodName, "testMethodName is null" );
    }

    private void setUpFixture( Object testObject, DiscoveredTestMethods methods )
        throws Throwable
    {
        if ( methods.setUpMethod != null )
        {
            methods.setUpMethod.invoke( testObject );
        }
    }

    private void tearDownFixture( Object testObject, DiscoveredTestMethods methods )
        throws Throwable
    {
        if ( methods.tearDownMethod != null )
        {
            methods.tearDownMethod.invoke( testObject );
        }
    }

    private DiscoveredTestMethods discoverTestMethods( Class<?> testClass )
    {
        DiscoveredTestMethods methods = new DiscoveredTestMethods();
        for ( Method m : testClass.getMethods() )
        {
            if ( isNoArgsInstanceMethod( m ) )
            {
                if ( isValidTestMethod( m ) )
                {
                    methods.testMethods.add( m );
                }
                else if ( SETUP_METHOD_NAME.equals( m.getName() ) )
                {
                    methods.setUpMethod = m;
                }
                else if ( TEARDOWN_METHOD_NAME.equals( m.getName() ) )
                {
                    methods.tearDownMethod = m;
                }
            }
        }
        return methods;
    }

    private static boolean isNoArgsInstanceMethod( Method m )
    {
        boolean isInstanceMethod = !Modifier.isStatic( m.getModifiers() );
        boolean returnsVoid = m.getReturnType().equals( void.class );
        boolean hasNoParams = m.getParameterTypes().length == 0;
        return isInstanceMethod && returnsVoid && hasNoParams;
    }

    private static boolean isValidTestMethod( Method m )
    {
        return m.getName().startsWith( TEST_METHOD_PREFIX );
    }

    private static class DiscoveredTestMethods
    {
        final Collection<Method> testMethods = new ArrayList<>();
        Method setUpMethod;
        Method tearDownMethod;
    }
}
