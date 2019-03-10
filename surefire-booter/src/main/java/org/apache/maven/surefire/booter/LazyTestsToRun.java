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

import java.util.Collections;
import java.util.Iterator;

import org.apache.maven.surefire.util.CloseableIterator;
import org.apache.maven.surefire.util.TestsToRun;

import static org.apache.maven.surefire.booter.CommandReader.getReader;
import static org.apache.maven.surefire.util.ReflectionUtils.loadClass;

/**
 * A variant of TestsToRun that is provided with test class names
 * from an {@code System.in}.
 * The method {@link #iterator()} returns an Iterator that blocks on calls to
 * {@link Iterator#hasNext()} or {@link Iterator#next()} until new classes are available, or no more
 * classes will be available or the internal stream is closed.
 * The iterator can be used only in one Thread and it is the thread which executes
 * {@link org.apache.maven.surefire.providerapi.SurefireProvider provider implementation}.
 *
 * @author Andreas Gudian
 * @author Tibor Digana
 */
final class LazyTestsToRun
    extends TestsToRun
{
    private final ForkedChannelEncoder eventChannel;

    /**
     * C'tor
     *
     * @param eventChannel the output stream to use when requesting new new tests
     */
    LazyTestsToRun( ForkedChannelEncoder eventChannel )
    {
        super( Collections.<Class<?>>emptySet() );

        this.eventChannel = eventChannel;
    }

    private final class BlockingIterator
        implements Iterator<Class<?>>
    {
        private final Iterator<String> it = getReader().getIterableClasses( eventChannel ).iterator();

        @Override
        public boolean hasNext()
        {
            return it.hasNext();
        }

        @Override
        public Class<?> next()
        {
            return findClass( it.next() );
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * @return test classes which have been retrieved by {@link LazyTestsToRun#iterator()}.
     */
    @Override
    public Iterator<Class<?>> iterated()
    {
        return newWeakIterator();
    }

    /**
     * The iterator can be used only in one Thread.
     * {@inheritDoc}
     * @see org.apache.maven.surefire.util.TestsToRun#iterator()
     * */
    @Override
    public Iterator<Class<?>> iterator()
    {
        return new BlockingIterator();
    }

    /* (non-Javadoc)
     * {@inheritDoc}
      * @see org.apache.maven.surefire.util.TestsToRun#toString()
      */
    @Override
    public String toString()
    {
        return "LazyTestsToRun";
    }

    /* (non-Javadoc)
     * {@inheritDoc}
     * @see org.apache.maven.surefire.util.TestsToRun#allowEagerReading()
     */
    @Override
    public boolean allowEagerReading()
    {
        return false;
    }

    private static Class<?> findClass( String clazz )
    {
        return loadClass( Thread.currentThread().getContextClassLoader(), clazz );
    }

    /**
     * @return snapshot of tests upon constructs of {@link CommandReader#iterated() iterator}.
     * Therefore weakly consistent while {@link LazyTestsToRun#iterator()} is being iterated.
     */
    private Iterator<Class<?>> newWeakIterator()
    {
        final Iterator<String> it = getReader().iterated();
        return new CloseableIterator<Class<?>>()
        {
            @Override
            protected boolean isClosed()
            {
                return LazyTestsToRun.this.isFinished();
            }

            @Override
            protected boolean doHasNext()
            {
                return it.hasNext();
            }

            @Override
            protected Class<?> doNext()
            {
                return findClass( it.next() );
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
