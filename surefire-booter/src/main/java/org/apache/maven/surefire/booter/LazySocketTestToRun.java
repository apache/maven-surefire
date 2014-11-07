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

import org.apache.maven.surefire.util.ReflectionUtils;
import org.apache.maven.surefire.util.TestsToRun;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * TestToRun implementation that lazily request tests from external service via socket
 *
 * @author Marek Piechut
 */
public class LazySocketTestToRun
                extends TestsToRun
{

    private static final String WANT_MORE = "" + ForkingRunListener.BOOTERCODE_NEXT_TEST + ",0,want more!\n";

    public static final int PAUSE_BETWEEN_RETRIES = 1000;

    private final List<Class> workQueue = new ArrayList<Class>();

    private boolean finished = false;

    private final URI url;

    private final int retries;

    public LazySocketTestToRun ( URI url )
    {
        this( url, 0 );
    }

    public LazySocketTestToRun ( URI url, int retries )
    {
        super( Collections.<Class>emptyList() );
        this.url = url;
        this.retries = retries;
    }

    private boolean hasNextTextClass ( int pos )
                    throws IOException
    {
        synchronized ( workQueue )
        {
            if ( !finished )
            {
                String nextTestName = tryToGetNextTestName();
                if ( !nothingMoreToProcess( nextTestName ) )
                {
                    Class testClass = loadTestClass( nextTestName );
                    workQueue.add( testClass );
                }
                else
                {
                    finished = true;
                }
            }
            return workQueue.size() > pos;
        }
    }

    private boolean nothingMoreToProcess ( String nextTestName )
    {
        return nextTestName == null || nextTestName.trim().length() == 0
                        || nextTestName.trim().equalsIgnoreCase( "null" );
    }

    private String tryToGetNextTestName ()
                    throws IOException
    {
        String nextTestName;
        int tries = 0;
        while ( true )
        {
            try
            {
                nextTestName = fetchNextTestName();
                break;
            }
            catch ( IOException e )
            {
                if ( tries < retries )
                {
                    tries++;
                    System.out.println( "Error connecting to external test source. Retry in 1 second." );
                    sleep( PAUSE_BETWEEN_RETRIES );
                }
                else
                {
                    throw e;
                }
            }
        }
        return nextTestName;
    }

    private void sleep ( int time )
    {
        try
        {
            Thread.sleep( time );
        }
        catch ( InterruptedException e1 )
        {
            throw new RuntimeException( e1 );
        }
    }

    private String fetchNextTestName ()
                    throws IOException
    {
        String testName = null;
        Socket socket = new Socket( url.getHost(), url.getPort() );
        BufferedWriter out = null;
        BufferedReader in = null;
        try
        {
            out = new BufferedWriter( new OutputStreamWriter( socket.getOutputStream() ) );
            out.write( WANT_MORE );
            out.flush();
            in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
            testName = in.readLine();
            socket.shutdownInput();
        }
        finally
        {
            try
            {
                if ( out != null )
                {
                    out.close();
                }

                if ( in != null )
                {
                    in.close();
                }
            }
            finally
            {
                socket.close();
            }
        }

        return testName;
    }

    private Class getItem ( int pos )
    {
        synchronized ( workQueue )
        {
            return workQueue.get( pos );
        }
    }

    private Class loadTestClass ( String name )
    {
        return ReflectionUtils.loadClass( Thread.currentThread().getContextClassLoader(), name );
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder( "LazySocketTestsToRun: " );
        sb.append( '(' ).append( url ).append( "):" );
        synchronized ( workQueue )
        {
            sb.append( workQueue );
        }

        return sb.toString();
    }

    @Override
    public Iterator<Class> iterator ()
    {
        return new NextExternalTestIterator();
    }

    @Override
    public boolean allowEagerReading ()
    {
        return false;
    }

    private class NextExternalTestIterator
                    implements Iterator<Class>
    {
        private int pos = -1;

        public boolean hasNext ()
        {
            try
            {
                return hasNextTextClass( pos + 1 );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( "Error receiving next test class from external source", e );
            }
        }

        public Class next ()
        {
            return getItem( ++pos );
        }

        public void remove ()
        {
            throw new UnsupportedOperationException();
        }
    }
}
