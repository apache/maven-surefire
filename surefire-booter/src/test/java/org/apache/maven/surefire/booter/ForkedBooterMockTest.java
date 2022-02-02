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

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.api.booter.MasterProcessChannelDecoder;
import org.apache.maven.surefire.api.booter.MasterProcessChannelEncoder;
import org.apache.maven.surefire.api.fork.ForkNodeArguments;
import org.apache.maven.surefire.api.util.internal.WritableBufferedByteChannel;
import org.apache.maven.surefire.booter.spi.AbstractMasterProcessChannelProcessorFactory;
import org.apache.maven.surefire.booter.spi.CommandChannelDecoder;
import org.apache.maven.surefire.booter.spi.EventChannelEncoder;
import org.apache.maven.surefire.booter.spi.LegacyMasterProcessChannelProcessorFactory;
import org.apache.maven.surefire.booter.spi.SurefireMasterProcessChannelProcessorFactory;
import org.apache.maven.surefire.shared.utils.cli.ShutdownHookUtils;
import org.apache.maven.surefire.spi.MasterProcessChannelProcessorFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.maven.surefire.api.util.internal.Channels.newBufferedChannel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.doCallRealMethod;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.reflect.Whitebox.invokeMethod;
import static org.powermock.reflect.Whitebox.setInternalState;

/**
 * PowerMock tests for {@link ForkedBooter}.
 */
@RunWith( PowerMockRunner.class )
@PrepareForTest( {
                     PpidChecker.class,
                     ForkedBooter.class,
                     EventChannelEncoder.class,
                     ShutdownHookUtils.class
} )
@PowerMockIgnore( { "org.jacoco.agent.rt.*", "com.vladium.emma.rt.*" } )
public class ForkedBooterMockTest
{
    @Rule
    public final ErrorCollector errorCollector = new ErrorCollector();

    @Mock
    private PpidChecker pluginProcessChecker;

    @Mock
    private ForkedBooter booter;

    @Mock
    private MasterProcessChannelProcessorFactory channelProcessorFactory;

    @Mock
    private ConsoleLogger logger;

    @Captor
    private ArgumentCaptor<String[]> capturedArgs;

    @Captor
    private ArgumentCaptor<ForkedBooter> capturedBooter;

    @Test
    public void shouldCheckNewPingMechanism() throws Exception
    {
        boolean canUse = invokeMethod( ForkedBooter.class, "canUseNewPingMechanism", (PpidChecker) null );
        assertThat( canUse ).isFalse();

        when( pluginProcessChecker.canUse() ).thenReturn( false );
        canUse = invokeMethod( ForkedBooter.class, "canUseNewPingMechanism", pluginProcessChecker );
        assertThat( canUse ).isFalse();

        when( pluginProcessChecker.canUse() ).thenReturn( true );
        canUse = invokeMethod( ForkedBooter.class, "canUseNewPingMechanism", pluginProcessChecker );
        assertThat( canUse ).isTrue();
    }

    @Test
    public void testMain() throws Exception
    {
        mockStatic( ForkedBooter.class );

        doCallRealMethod()
                .when( ForkedBooter.class, "run", capturedBooter.capture(), capturedArgs.capture() );

        String[] args = new String[]{ "/", "dump", "surefire.properties", "surefire-effective.properties" };
        invokeMethod( ForkedBooter.class, "run", booter, args );

        assertThat( capturedBooter.getAllValues() )
                .hasSize( 1 )
                .contains( booter );

        assertThat( capturedArgs.getAllValues() )
                .hasSize( 1 );
        assertThat( capturedArgs.getAllValues().get( 0 )[0] )
                .isEqualTo( "/" );
        assertThat( capturedArgs.getAllValues().get( 0 )[1] )
                .isEqualTo( "dump" );
        assertThat( capturedArgs.getAllValues().get( 0 )[2] )
                .isEqualTo( "surefire.properties" );
        assertThat( capturedArgs.getAllValues().get( 0 )[3] )
                .isEqualTo( "surefire-effective.properties" );

        verifyPrivate( booter, times( 1 ) )
                .invoke( "setupBooter", same( args[0] ), same( args[1] ), same( args[2] ), same( args[3] ) );

        verifyPrivate( booter, times( 1 ) )
                .invoke( "execute" );

        verifyNoMoreInteractions( booter );
    }

    @Test
    public void testMainWithError() throws Exception
    {
        mockStatic( ForkedBooter.class );

        doCallRealMethod()
                .when( ForkedBooter.class, "run", any( ForkedBooter.class ), any( String[].class ) );

        doThrow( new RuntimeException( "dummy exception" ) )
                .when( booter, "execute" );

        doNothing()
                .when( booter, "setupBooter",
                        any( String.class ), any( String.class ), any( String.class ), any( String.class ) );

        setInternalState( booter, "logger", logger );

        String[] args = new String[]{ "/", "dump", "surefire.properties", "surefire-effective.properties" };
        invokeMethod( ForkedBooter.class, "run", booter, args );

        verifyPrivate( booter, times( 1 ) )
                .invoke( "setupBooter", same( args[0] ), same( args[1] ), same( args[2] ), same( args[3] ) );

        verifyPrivate( booter, times( 1 ) )
                .invoke( "execute" );

        verify( logger, times( 1 ) )
                .error( eq( "dummy exception" ), any( RuntimeException.class ) );

        verifyPrivate( booter, times( 1 ) )
                .invoke( "cancelPingScheduler" );

        verifyPrivate( booter, times( 1 ) )
                .invoke( "exit1" );

        verifyNoMoreInteractions( booter );
    }

    @Test
    public void shouldNotCloseChannelProcessorFactory() throws Exception
    {
        setInternalState( booter, "channelProcessorFactory", (MasterProcessChannelProcessorFactory) null );

        doCallRealMethod()
            .when( booter, "closeForkChannel" );

        invokeMethod( booter, "closeForkChannel" );

        verifyZeroInteractions( channelProcessorFactory );
    }

    @Test
    public void shouldCloseChannelProcessorFactory() throws Exception
    {
        setInternalState( booter, "channelProcessorFactory", channelProcessorFactory );

        doCallRealMethod()
            .when( booter, "closeForkChannel" );

        invokeMethod( booter, "closeForkChannel" );

        verify( channelProcessorFactory, times( 1 ) )
            .close();
        verifyNoMoreInteractions( channelProcessorFactory );
    }

    @Test
    public void shouldFailOnCloseChannelProcessorFactory() throws Exception
    {
        setInternalState( booter, "channelProcessorFactory", channelProcessorFactory );

        doThrow( new IOException() )
            .when( channelProcessorFactory )
            .close();

        doCallRealMethod()
            .when( booter, "closeForkChannel" );

        invokeMethod( booter, "closeForkChannel" );

        verify( channelProcessorFactory, times( 1 ) )
            .close();
        verifyNoMoreInteractions( channelProcessorFactory );
    }

    @Test
    public void shouldLookupLegacyDecoderFactory() throws Exception
    {
        mockStatic( ForkedBooter.class );

        doCallRealMethod()
            .when( ForkedBooter.class, "lookupDecoderFactory", anyString() );

        try ( final MasterProcessChannelProcessorFactory factory =
                  invokeMethod( ForkedBooter.class, "lookupDecoderFactory", "pipe://3" ) )
        {
            assertThat( factory ).isInstanceOf( LegacyMasterProcessChannelProcessorFactory.class );

            assertThat( factory.canUse( "pipe://3" ) ).isTrue();

            assertThat( factory.canUse( "-- whatever --" ) ).isFalse();

            errorCollector.checkThrows( MalformedURLException.class, new ThrowingRunnable()
            {
                @Override
                public void run() throws Throwable
                {
                    factory.connect( "tcp://localhost:123" );
                    fail( "should not connect to the port 123" );
                }
            } );

            factory.connect( "pipe://3" );

            ForkNodeArguments args = new ForkedNodeArg( 1, false );
            MasterProcessChannelDecoder decoder = factory.createDecoder( args );
            assertThat( decoder ).isInstanceOf( CommandChannelDecoder.class );
            MasterProcessChannelEncoder encoder = factory.createEncoder( args );
            assertThat( encoder ).isInstanceOf( EventChannelEncoder.class );
        }
    }

    @Test
    @SuppressWarnings( "checkstyle:magicnumber" )
    public void shouldScheduleFlushes() throws Exception
    {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        class Factory extends AbstractMasterProcessChannelProcessorFactory
        {
            @Override
            public boolean canUse( String channelConfig )
            {
                return false;
            }

            @Override
            public void connect( String channelConfig )
            {
            }

            @Override
            public MasterProcessChannelDecoder createDecoder( @Nonnull  ForkNodeArguments args )
            {
                return null;
            }

            @Override
            public MasterProcessChannelEncoder createEncoder( @Nonnull ForkNodeArguments args )
            {
                return null;
            }

            public void runScheduler() throws InterruptedException
            {
                final WritableBufferedByteChannel channel = newBufferedChannel( out );
                schedulePeriodicFlusher( 100, channel );
                Thread t = new Thread()
                {
                    @Override
                    public void run()
                    {
                        for ( int i = 0; i < 10; i++ )
                        {
                            try
                            {
                                channel.write( ByteBuffer.wrap( new byte[] {1} ) );
                                Thread.sleep( 75 );
                            }
                            catch ( Exception e )
                            {
                                //
                            }
                        }
                    }
                };
                t.setDaemon( true );
                t.start();
                t.join( 5000L );
            }
        }

        Factory factory = new Factory();
        factory.runScheduler();
        factory.close();
        assertThat( out.size() ).isPositive();
        assertThat( out.size() ).isLessThanOrEqualTo( 10 );
    }

    @Test
    public void shouldLookupSurefireDecoderFactory() throws Exception
    {
        mockStatic( ForkedBooter.class );

        doCallRealMethod()
            .when( ForkedBooter.class, "lookupDecoderFactory", anyString() );

        try ( ServerSocketChannel server = ServerSocketChannel.open() )
        {
            server.bind( new InetSocketAddress( 0 ) );
            int serverPort = ( (InetSocketAddress) server.getLocalAddress() ).getPort();

            try ( MasterProcessChannelProcessorFactory factory =
                     invokeMethod( ForkedBooter.class, "lookupDecoderFactory", "tcp://localhost:" + serverPort ) )
            {
                assertThat( factory )
                    .isInstanceOf( SurefireMasterProcessChannelProcessorFactory.class );

                assertThat( factory.canUse( "tcp://localhost:" + serverPort ) )
                    .isTrue();

                assertThat( factory.canUse( "-- whatever --" ) )
                    .isFalse();

                errorCollector.checkThrows( MalformedURLException.class, new ThrowingRunnable()
                {
                    @Override
                    public void run() throws Throwable
                    {
                        factory.connect( "pipe://1" );
                        fail( "should not connect" );
                    }
                } );

                errorCollector.checkThrows( IOException.class, new ThrowingRunnable()
                {
                    @Override
                    public void run() throws Throwable
                    {
                        factory.connect( "tcp://localhost:123\u0000\u0000\u0000" );
                        fail( "should not connect to incorrect uri" );
                    }
                } );

                factory.connect( "tcp://localhost:" + serverPort );
                ForkNodeArguments args = new ForkedNodeArg( 1, false );
                MasterProcessChannelDecoder decoder = factory.createDecoder( args );
                assertThat( decoder )
                    .isInstanceOf( CommandChannelDecoder.class );
                MasterProcessChannelEncoder encoder = factory.createEncoder( args );
                assertThat( encoder )
                    .isInstanceOf( EventChannelEncoder.class );
            }
        }
    }

    @Test
    public void shouldAuthenticate() throws Exception
    {
        mockStatic( ForkedBooter.class );

        doCallRealMethod()
            .when( ForkedBooter.class, "lookupDecoderFactory", anyString() );

        try ( final ServerSocketChannel server = ServerSocketChannel.open() )
        {
            server.bind( new InetSocketAddress( 0 ) );
            int serverPort = ( (InetSocketAddress) server.getLocalAddress() ).getPort();
            final String uuid = UUID.randomUUID().toString();
            String url = "tcp://localhost:" + serverPort + "?sessionId=" + uuid;
            try ( final MasterProcessChannelProcessorFactory factory =
                      invokeMethod( ForkedBooter.class, "lookupDecoderFactory", url ) )
            {
                assertThat( factory )
                    .isInstanceOf( SurefireMasterProcessChannelProcessorFactory.class );

                FutureTask<Boolean> task = new FutureTask<>( new Callable<Boolean>()
                {
                    @Override
                    public Boolean call()
                    {
                        try
                        {
                            SocketChannel channel = server.accept();
                            ByteBuffer bb = ByteBuffer.allocate( uuid.length() );
                            int read = channel.read( bb );
                            assertThat( read )
                                .isEqualTo( uuid.length() );
                            ( (Buffer) bb ).flip();
                            assertThat( new String( bb.array(), US_ASCII ) )
                                .isEqualTo( uuid );
                            return true;
                        }
                        catch ( IOException e )
                        {
                            return false;
                        }
                    }
                } );

                Thread t = new Thread( task );
                t.setDaemon( true );
                t.start();

                factory.connect( url );

                try
                {
                    task.get( 10, SECONDS );
                }
                finally
                {
                    factory.close();
                }
            }
        }
    }

    @Test
    public void testFlushEventChannelOnExit() throws Exception
    {
        mockStatic( ShutdownHookUtils.class );

        final MasterProcessChannelEncoder eventChannel = mock( MasterProcessChannelEncoder.class );
        ForkedBooter booter = new ForkedBooter();
        setInternalState( booter, "eventChannel", eventChannel );

        doAnswer( new Answer<Object>()
        {
            @Override
            public Object answer( InvocationOnMock invocation )
            {
                Thread t = invocation.getArgument( 0 );
                assertThat( t.isDaemon() ).isTrue();
                t.run();
                verify( eventChannel, times( 1 ) ).onJvmExit();
                return null;
            }
        } ).when( ShutdownHookUtils.class, "addShutDownHook", any( Thread.class ) );
        invokeMethod( booter, "flushEventChannelOnExit" );
    }

    @Test
    public void shouldParseUUID() throws Exception
    {
        UUID uuid = UUID.randomUUID();
        URI uri = new URI( "tcp://localhost:12345?sessionId=" + uuid );
        String parsed = invokeMethod( SurefireMasterProcessChannelProcessorFactory.class, "extractSessionId", uri );
        assertThat( parsed )
            .isEqualTo( uuid.toString() );
    }

    @Test
    public void shouldNotParseUUID() throws Exception
    {
        UUID uuid = UUID.randomUUID();
        URI uri = new URI( "tcp://localhost:12345?xxx=" + uuid );
        String parsed = invokeMethod( SurefireMasterProcessChannelProcessorFactory.class, "extractSessionId", uri );
        assertThat( parsed )
            .isNull();
    }
}
