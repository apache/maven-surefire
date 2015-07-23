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

import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;

import org.apache.maven.surefire.util.ReflectionUtils;
import org.apache.maven.surefire.util.TestsToRun;

/**
 * A variant of TestsToRun that is provided with test class names
 * from an {@code System.in}.
 * The method {@link #iterator()} returns an Iterator that blocks on calls to
 * {@link Iterator#hasNext()} or {@link Iterator#next()} until new classes are available, or no more
 * classes will be available or the internal stream is closed.
 *
 * @author Andreas Gudian
 * @author Tibor Digana
 */
final class LazyTestsToRun
    extends TestsToRun
{
    private final PrintStream originalOutStream;

    /**
     * C'tor
     *
     * @param originalOutStream the output stream to use when requesting new new tests
     */
    LazyTestsToRun( PrintStream originalOutStream )
    {
        super( Collections.<Class<?>>emptySet() );

        this.originalOutStream = originalOutStream;
    }

    private final class BlockingIterator
        implements Iterator<Class<?>>
    {
        private final Iterator<String> it =
            MasterProcessReader.getReader().getIterableClasses( originalOutStream ).iterator();

        public boolean hasNext()
        {
            return it.hasNext();
        }

        public Class<?> next()
        {
            String clazz = it.next();
            return ReflectionUtils.loadClass( Thread.currentThread().getContextClassLoader(), clazz );
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }

    }

    /* (non-Javadoc)
      * @see org.apache.maven.surefire.util.TestsToRun#iterator()
      */
    public Iterator<Class<?>> iterator()
    {
        return new BlockingIterator();
    }

    /* (non-Javadoc)
      * @see org.apache.maven.surefire.util.TestsToRun#toString()
      */
    public String toString()
    {
        return "LazyTestsToRun";
    }

    /* (non-Javadoc)
     * @see org.apache.maven.surefire.util.TestsToRun#allowEagerReading()
     */
    public boolean allowEagerReading()
    {
        return false;
    }
}
