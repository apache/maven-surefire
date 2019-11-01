package org.apache.maven.plugin.surefire.extensions;

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

import org.apache.maven.plugin.surefire.booterclient.output.ThreadedStreamConsumer;
import org.apache.maven.shared.utils.cli.StreamPumper;
import org.apache.maven.surefire.booter.Command;
import org.apache.maven.surefire.extensions.ForkedChannel;
import org.apache.maven.surefire.extensions.ForkedChannelServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author <a href="mailto:jon@jonbell.net">Jonathan Bell</a>
 * @since 3.0.0-M4
 */
public class TCPForkedChannelServer extends ForkedChannelServer
{

    private final int port;

    private final ServerSocket serverSocket;

    private Thread clientThread;
    private StreamPumper outputPumper;
    private Socket client;
    private IOException exceptionThrownInStart;
    private OutputStream outputStream;
    private final ForkedChannel encoder;

    public TCPForkedChannelServer( final ThreadedStreamConsumer threadedStreamConsumer,
                                   final ForkedChannel encoder ) throws IOException
    {
        super( "tcp://127.0.0.1/0" ); //TODO allow for configuring where we bind to?
        this.serverSocket = new ServerSocket( 0 );
        this.port = this.serverSocket.getLocalPort();
        //Create a new thread to manage this client connection
        this.clientThread = new Thread( new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    client = serverSocket.accept();
                    outputPumper = new StreamPumper( client.getInputStream(), threadedStreamConsumer );
                    outputPumper.start();
                    outputStream = client.getOutputStream();
                    synchronized ( TCPForkedChannelServer.this )
                    {
                        TCPForkedChannelServer.this.notify();
                    }
                }
                catch ( IOException e )
                {
                    //We will throw the exception when close gets called
                    exceptionThrownInStart = e;
                }
            }
        } );
        //If the fork never starts this thread should die with the JVM
        this.clientThread.setDaemon( true );
        this.clientThread.start();
        this.encoder = encoder;
    }

    @Override
    public String getChannelConfig()
    {
        return "tcp://127.0.0.1/" + port;
    }

    private void awaitClientConnection()
    {
        synchronized ( this )
        {
            while ( outputStream == null )
            {
                try
                {
                    this.wait();
                }
                catch ( InterruptedException e )
                {
                }
            }
        }
    }

    @Override
    public void send( Command command ) throws IOException
    {
        //Block until the socket is connected
        awaitClientConnection();
        byte[] ret = encoder.encode( command );
        if ( ret == null )
        {
            //EOF
            return;
        }
        else
        {
            this.outputStream.write( ret );
        }
        this.outputStream.flush();
    }

    @Override
    public void close() throws IOException
    {
        //Make sure streams are done
        try
        {
            outputPumper.waitUntilDone();
        }
        catch ( InterruptedException e )
        {
        }
        outputPumper.close();
        client.close();
        if ( exceptionThrownInStart != null )
        {
            throw exceptionThrownInStart;
        }
    }
}
