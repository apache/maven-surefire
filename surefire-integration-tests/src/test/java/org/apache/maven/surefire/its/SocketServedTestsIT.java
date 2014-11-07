package org.apache.maven.surefire.its;

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

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

/**
 * Basic test for test name server over socket
 *
 * @author Marek Piechut
 */
public class SocketServedTestsIT
                extends SurefireJUnit4IntegrationTestCase
{
    private static final int PORT_RANGE_START = 26001;

    private static final int PORT_SEARCH_TRIES = 5;

    private Thread serverThread;

    private Collection<String> tests = Arrays.asList( "testsOverSocket.Test1", "testsOverSocket.Test2" );

    @Test
    public void shouldExecuteOnlyTestsFromServer ()
    {

        int port = findSocketPort();
        String url = "socket://localhost:" + port;

        startServer( port, tests );
        try
        {
            final OutputValidator outputValidator =
                            unpack( "/testsOverSocket" )
                                            .redirectToFile( true )
                                            .setJUnitVersion( "4.7" )
                                            .useTestsFromExternalSource()
                                            .externalSourceUrl( url )
                                            .executeTest();
            outputValidator.getSurefireReportsFile( "testsOverSocket.Test1-output.txt" ).assertContainsText( "Test1" );
            outputValidator.getSurefireReportsFile( "testsOverSocket.Test2-output.txt" ).assertContainsText( "Test2" );
            outputValidator.getSurefireReportsFile( "testsOverSocket.Test3-output.txt" ).assertFileNotExists();
        }
        finally
        {
            stopServer();
        }
    }

    private void startServer ( final int port, final Collection<String> tests )
    {
        final Queue<String> serverTests = new LinkedList<String>( tests );
        serverThread = new Thread()
        {
            @Override
            public void run ()
            {
                ServerSocket socket = null;
                try
                {
                    System.out.println( "Starting test server on port " + port );
                    socket = new ServerSocket( port );
                    while ( true )
                    {
                        Socket client = socket.accept();
                        try
                        {
                            PrintWriter out = new PrintWriter( client.getOutputStream() );
                            String testName = serverTests.poll();
                            out.write( testName + '\n' );
                            out.flush();
                            out.close();
                            if ( testName == null )
                            {
                                break;
                            }
                        }
                        finally
                        {
                            client.close();
                        }
                    }

                }
                catch ( Exception e )
                {
                    throw new RuntimeException( "Error starting tests socket server", e );
                }
                finally
                {
                    try
                    {
                        System.out.println( "Closing test server" );
                        socket.close();
                    }
                    catch ( IOException e )
                    {
                        throw new RuntimeException( "Error closing test server socket", e );
                    }

                }

            }
        };

        serverThread.start();
    }

    private void stopServer ()
    {
        serverThread.interrupt();
    }

    private int findSocketPort ()
    {
        Random random = new Random();
        for ( int i = 1; i < PORT_SEARCH_TRIES; i++ )
        {
            int port = PORT_RANGE_START + random.nextInt( 1000 );
            ServerSocket socket = null;
            try
            {
                socket = new ServerSocket( port );
                return port;
            }
            catch ( IOException e )
            {
                //Socket busy. Let's try another one.
            }
            finally
            {
                if ( socket != null )
                {
                    try
                    {
                        socket.close();
                    }
                    catch ( IOException e )
                    {
                        throw new RuntimeException( "Error closing socket", e );
                    }
                }
            }
        }

        throw new RuntimeException( "Could not find free socket in " + PORT_SEARCH_TRIES + " tries." );
    }
}