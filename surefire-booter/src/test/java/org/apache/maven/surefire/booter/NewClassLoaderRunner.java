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

import org.apache.maven.surefire.shared.utils.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.ExpectException;
import org.junit.internal.runners.statements.Fail;
import org.junit.internal.runners.statements.RunAfters;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static java.io.File.pathSeparator;

/**
 * JUnit runner testing methods in a separate class loader.
 *
 * @author Tibor Digana (tibor17)
 * @since 2.19
 */
public class NewClassLoaderRunner
    extends BlockJUnit4ClassRunner
{
    private static final String PROJECT_DIR = System.getProperty( "java.dir" );

    private Class<?> cls;

    public NewClassLoaderRunner( Class<?> clazz )
        throws InitializationError
    {
        super( clazz );
    }

    @Override
    protected void runChild( FrameworkMethod method, RunNotifier notifier )
    {
        ClassLoader backup = Thread.currentThread().getContextClassLoader();
        try
        {
            TestClassLoader loader = new TestClassLoader();
            Thread.currentThread().setContextClassLoader( loader );
            cls = getFromTestClassLoader( getTestClass().getName(), loader );
            method = new FrameworkMethod( cls.getMethod( method.getName() ) );
            super.runChild( method, notifier );
        }
        catch ( NoSuchMethodException e )
        {
            throw new IllegalStateException( e );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( backup );
        }
    }

    @Override
    protected Statement methodBlock( FrameworkMethod method )
    {
        try
        {
            Object test = new ReflectiveCallable()
            {
                @Override
                protected Object runReflectiveCall()
                    throws Throwable
                {
                    return createTest();
                }
            }.run();

            Statement statement = methodInvoker( method, test );
            statement = possiblyExpectingExceptions( method, test, statement );
            statement = withBefores( method, test, statement );
            statement = withAfters( method, test, statement );
            return statement;
        }
        catch ( Throwable e )
        {
            return new Fail( e );
        }
    }

    @Override
    @SuppressWarnings( "unchecked" )
    protected Statement possiblyExpectingExceptions( FrameworkMethod method, Object test, Statement next )
    {
        try
        {
            Class<? extends Annotation> t =
                (Class<? extends Annotation>) Thread.currentThread().getContextClassLoader().loadClass(
                    Test.class.getName() );
            Annotation annotation = method.getAnnotation( t );
            Class<? extends Throwable> exp =
                (Class<? extends Throwable>) t.getMethod( "expected" ).invoke( annotation );
            boolean isException = exp != null && !Test.None.class.getName().equals( exp.getName() );
            return isException ? new ExpectException( next, exp ) : next;
        }
        catch ( Exception e )
        {
            throw new IllegalStateException( e );
        }
    }

    @Override
    @SuppressWarnings( "unchecked" )
    protected Statement withBefores( FrameworkMethod method, Object target, Statement statement )
    {
        try
        {
            Class<? extends Annotation> before =
                (Class<? extends Annotation>) Thread.currentThread().getContextClassLoader().loadClass(
                    Before.class.getName() );
            List<FrameworkMethod> befores = new TestClass( target.getClass() ).getAnnotatedMethods( before );
            return befores.isEmpty() ? statement : new RunBefores( statement, befores, target );
        }
        catch ( ClassNotFoundException e )
        {
            throw new IllegalStateException( e );
        }
    }

    @Override
    @SuppressWarnings( "unchecked" )
    protected Statement withAfters( FrameworkMethod method, Object target, Statement statement )
    {
        try
        {
            Class<? extends Annotation> after =
                (Class<? extends Annotation>) Thread.currentThread().getContextClassLoader().loadClass(
                    After.class.getName() );
            List<FrameworkMethod> afters = new TestClass( target.getClass() ).getAnnotatedMethods( after );
            return afters.isEmpty() ? statement : new RunAfters( statement, afters, target );
        }
        catch ( ClassNotFoundException e )
        {
            throw new IllegalStateException( e );
        }
    }

    @Override
    protected Object createTest()
        throws Exception
    {
        return cls == null ? super.createTest() : cls.getConstructor().newInstance();
    }

    private static Class<?> getFromTestClassLoader( String clazz, TestClassLoader loader )
    {
        try
        {
            return Class.forName( clazz, true, loader );
        }
        catch ( ClassNotFoundException e )
        {
            throw new IllegalStateException( e );
        }
    }

    private static class TestClassLoader
        extends URLClassLoader
    {
        TestClassLoader()
        {
            super( toClassPath(), null );
        }

        /**
         * Compliant with Java 9 or prior version of JRE.
         *
         * @return classpath
         */
        private static URL[] toClassPath()
        {
            try
            {
                Collection<URL> cp = toPathList(); // if Maven run
                if ( cp.isEmpty() )
                {
                    // if IDE
                    cp = toPathList( System.getProperty( "java.class.path" ) );
                }
                return cp.toArray( new URL[cp.size()] );
            }
            catch ( IOException e )
            {
                e.printStackTrace();
                return new URL[0];
            }
        }

        private static Collection<URL> toPathList( String path ) throws MalformedURLException
        {
            Collection<URL> classPath = new HashSet<>();
            for ( String file : path.split( pathSeparator ) )
            {
                classPath.add( new File( file ).toURI().toURL() );
            }
            return classPath;
        }

        private static Collection<URL> toPathList()
        {
            Collection<URL> classPath = new HashSet<>();
            try
            {
                File classPathFile = new File( PROJECT_DIR, "target/test-classpath/cp.txt" );
                String[] files = FileUtils.fileRead( classPathFile, "UTF-8" ).split( pathSeparator );
                for ( String file : files )
                {
                    classPath.add( new File( file ).toURI().toURL() );
                }
                classPath.add( new File( PROJECT_DIR, "target/classes" ).toURI().toURL() );
                classPath.add( new File( PROJECT_DIR, "target/test-classes" ).toURI().toURL() );
            }
            catch ( IOException e )
            {
                e.printStackTrace();
                // turn to java.class.path
                classPath.clear();
            }
            return classPath;
        }
    }
}
