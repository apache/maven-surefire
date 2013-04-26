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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.surefire.util.ReflectionUtils;
import org.apache.maven.surefire.util.TestsToRun;

/**
 * A variant of TestsToRun that is provided with test class names
 * from an {@link InputStream} (e.g. {@code System.in}). The method
 * {@link #iterator()} returns an Iterator that blocks on calls to
 * {@link Iterator#hasNext()} until new classes are available, or no more
 * classes will be available.
 *
 * @author Andreas Gudian
 */
class LazyTestsToRun
    extends TestsToRun
{
    private final List<Class> workQueue = new ArrayList<Class>();

    private BufferedReader inputReader;

    private boolean streamClosed = false;

    private PrintStream originalOutStream;

    /**
     * C'tor
     *
     * @param testSource        source to read the tests from
     * @param originalOutStream the output stream to use when requesting new new tests
     */
    public LazyTestsToRun( InputStream testSource, PrintStream originalOutStream )
    {
        super( Collections.<Class>emptyList() );

        this.originalOutStream = originalOutStream;

        inputReader = new BufferedReader( new InputStreamReader( testSource ) );
    }

    protected void addWorkItem( String className )
    {
        synchronized ( workQueue )
        {
            workQueue.add( ReflectionUtils.loadClass( Thread.currentThread().getContextClassLoader(), className ) );
        }
    }

    protected void requestNextTest()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( (char) ForkingRunListener.BOOTERCODE_NEXT_TEST ).append( ",0,want more!\n" );
        originalOutStream.print( sb.toString() );
    }

    private class BlockingIterator
        implements Iterator<Class>
    {
        private int lastPos = -1;

        public boolean hasNext()
        {
            int nextPos = lastPos + 1;
            synchronized ( workQueue )
            {
                if ( workQueue.size() > nextPos )
                {
                    return true;
                }
                else
                {
                    if ( needsToWaitForInput( nextPos ) )
                    {
                        requestNextTest();

                        String nextClassName;
                        try
                        {
                            nextClassName = inputReader.readLine();
                        }
                        catch ( IOException e )
                        {
                            streamClosed = true;
                            return false;
                        }

                        if ( null == nextClassName )
                        {
                            streamClosed = true;
                        }
                        else
                        {
                            addWorkItem( nextClassName );
                        }
                    }

                    return ( workQueue.size() > nextPos );
                }
            }
        }

        private boolean needsToWaitForInput( int nextPos )
        {
            return workQueue.size() == nextPos && !streamClosed;
        }

        public Class next()
        {
            synchronized ( workQueue )
            {
                return workQueue.get( ++lastPos );
            }
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }

    }

    /* (non-Javadoc)
      * @see org.apache.maven.surefire.util.TestsToRun#iterator()
      */
    public Iterator<Class> iterator()
    {
        return new BlockingIterator();
    }

    /* (non-Javadoc)
      * @see org.apache.maven.surefire.util.TestsToRun#toString()
      */
    public String toString()
    {
        StringBuilder sb = new StringBuilder( "LazyTestsToRun " );
        synchronized ( workQueue )
        {
            sb.append( "(more items expected: " ).append( !streamClosed ).append( "): " );
            sb.append( workQueue );
        }

        return sb.toString();
    }

    /* (non-Javadoc)
     * @see org.apache.maven.surefire.util.TestsToRun#allowEagerReading()
     */
    public boolean allowEagerReading() {
        return false;
    }

}
