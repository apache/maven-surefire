package org.apache.maven.surefire.api.util;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.surefire.api.testset.TestSetFailedException;

import static java.lang.Math.max;

/**
 * Contains all the tests that have been found according to specified include/exclude
 * specification for a given surefire run.
 *
 * @author Kristian Rosenvold (junit core adaption)
 */
public class TestsToRun implements Iterable<Class<?>>
{
    private final List<Class<?>> locatedClasses;

    private volatile boolean finished;

    private int iteratedCount;

    /**
     * Constructor
     *
     * @param locatedClasses A set of java.lang.Class objects representing tests to run
     */
    public TestsToRun( Set<Class<?>> locatedClasses )
    {
        this.locatedClasses = new ArrayList<>( locatedClasses );
    }

    public static TestsToRun fromClass( Class<?> clazz )
        throws TestSetFailedException
    {
        return new TestsToRun( Collections.<Class<?>>singleton( clazz ) );
    }

    /**
     * @return test classes which have been retrieved by {@link TestsToRun#iterator()}.
     */
    public Iterator<Class<?>> iterated()
    {
        return newWeakIterator();
    }

    /**
     * Returns an iterator over the located java.lang.Class objects
     *
     * @return an unmodifiable iterator
     */
    @Override
    public Iterator<Class<?>> iterator()
    {
        return new ClassesIterator();
    }

    private final class ClassesIterator
        extends CloseableIterator<Class<?>>
    {
        private final Iterator<Class<?>> it = TestsToRun.this.locatedClasses.iterator();

        private int iteratedCount;

        @Override
        protected boolean isClosed()
        {
            return TestsToRun.this.isFinished();
        }

        @Override
        protected boolean doHasNext()
        {
            return it.hasNext();
        }

        @Override
        protected Class<?> doNext()
        {
            Class<?> nextTest = it.next();
            TestsToRun.this.iteratedCount = max( ++iteratedCount, TestsToRun.this.iteratedCount );
            return nextTest;
        }

        @Override
        protected void doRemove()
        {
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException( "unsupported remove" );
        }
    }

    public final void markTestSetFinished()
    {
        finished = true;
    }

    public final boolean isFinished()
    {
        return finished;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder( "TestsToRun: [" );
        for ( Class<?> clazz : this )
        {
            sb.append( ' ' )
                    .append( clazz.getName() );
        }

        sb.append( ']' );
        return sb.toString();
    }

    public boolean containsAtLeast( int atLeast )
    {
        return containsAtLeast( iterator(), atLeast );
    }

    private boolean containsAtLeast( Iterator<Class<?>> it, int atLeast )
    {
        for ( int i = 0; i < atLeast; i++ )
        {
            if ( !it.hasNext() )
            {
                return false;
            }

            it.next();
        }

        return true;
    }

    public boolean containsExactly( int items )
    {
        Iterator<Class<?>> it = iterator();
        return containsAtLeast( it, items ) && !it.hasNext();
    }

    /**
     * @return {@code true}, if the classes may be read eagerly. {@code false},
     *         if the classes must only be read lazy.
     */
    public boolean allowEagerReading()
    {
        return true;
    }

    public Class<?>[] getLocatedClasses()
    {
        if ( !allowEagerReading() )
        {
            throw new IllegalStateException( "Cannot eagerly read" );
        }
        Collection<Class<?>> result = new ArrayList<>();
        for ( Class<?> clazz : this )
        {
            result.add( clazz );
        }
        return result.toArray( new Class<?>[result.size()] );
    }

    /**
     * Get test class which matches className
     *
     * @param className string used to find the test class
     * @return Class object with the matching name, null if could not find a class with the matching name
     */
    public Class<?> getClassByName( String className )
    {
        for ( Class<?> clazz : this )
        {
            if ( clazz.getName().equals( className ) )
            {
                return clazz;
            }
        }
        return null;
    }

    /**
     * @return snapshot of tests upon constructs of internal iterator.
     * Therefore weakly consistent while {@link TestsToRun#iterator()} is being iterated.
     */
    private Iterator<Class<?>> newWeakIterator()
    {
        final Iterator<Class<?>> it = locatedClasses.subList( 0, iteratedCount ).iterator();
        return new CloseableIterator<Class<?>>()
        {
            @Override
            protected boolean isClosed()
            {
                return TestsToRun.this.isFinished();
            }

            @Override
            protected boolean doHasNext()
            {
                return it.hasNext();
            }

            @Override
            protected Class<?> doNext()
            {
                return it.next();
            }

            @Override
            protected void doRemove()
            {
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException( "unsupported remove" );
            }
        };
    }
}
