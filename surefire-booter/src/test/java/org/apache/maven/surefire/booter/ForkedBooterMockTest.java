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

import org.apache.maven.surefire.report.StackTraceWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doCallRealMethod;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.reflect.Whitebox.invokeMethod;
import static org.powermock.reflect.Whitebox.setInternalState;
import static org.powermock.utils.JavaVersion.JAVA_RECENT;
import static org.powermock.utils.JavaVersion.JAVA_12;

/**
 * PowerMock tests for {@link ForkedBooter}.
 */
@RunWith( PowerMockRunner.class )
@PrepareForTest( { PpidChecker.class, ForkedBooter.class, ForkedChannelEncoder.class } )
@PowerMockIgnore( { "org.jacoco.agent.rt.*", "com.vladium.emma.rt.*" } )
public class ForkedBooterMockTest
{
    @Mock
    private PpidChecker pluginProcessChecker;

    @Mock
    private ForkedBooter booter;

    @Mock
    private ForkedChannelEncoder eventChannel;

    @Captor
    private ArgumentCaptor<StackTraceWriter> capturedStackTraceWriter;

    @Captor
    private ArgumentCaptor<Boolean> capturedBoolean;

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
        PowerMockito.mockStatic( ForkedBooter.class );

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
        //does not work in PowerMock
        //assumeFalse( JAVA_RECENT.atLeast( JAVA_12 ) );
        if ( JAVA_RECENT.atLeast( JAVA_12 ) )
        {
            return;
        }

        PowerMockito.mockStatic( ForkedBooter.class );

        doCallRealMethod()
                .when( ForkedBooter.class, "run", any( ForkedBooter.class ), any( String[].class ) );

        doThrow( new RuntimeException( "dummy exception" ) )
                .when( booter, "execute" );

        doNothing()
                .when( booter, "setupBooter",
                        any( String.class ), any( String.class ), any( String.class ), any( String.class ) );

        // broken in PowerMock JDK 12
        setInternalState( booter, "eventChannel", eventChannel );

        String[] args = new String[]{ "/", "dump", "surefire.properties", "surefire-effective.properties" };
        invokeMethod( ForkedBooter.class, "run", booter, args );

        verifyPrivate( booter, times( 1 ) )
                .invoke( "setupBooter", same( args[0] ), same( args[1] ), same( args[2] ), same( args[3] ) );

        verifyPrivate( booter, times( 1 ) )
                .invoke( "execute" );

        verify( eventChannel, times( 1 ) )
                .consoleErrorLog( capturedStackTraceWriter.capture(), capturedBoolean.capture() );
        assertThat( capturedStackTraceWriter.getValue() )
                .isNotNull();
        assertThat( capturedStackTraceWriter.getValue().smartTrimmedStackTrace() )
                .isEqualTo( "test subsystem#no method RuntimeException dummy exception" );
        assertThat( capturedStackTraceWriter.getValue().getThrowable().getTarget() )
                .isNotNull()
                .isInstanceOf( RuntimeException.class );
        assertThat( capturedStackTraceWriter.getValue().getThrowable().getTarget().getMessage() )
                .isEqualTo( "dummy exception" );

        verifyPrivate( booter, times( 1 ) )
                .invoke( "cancelPingScheduler" );

        verifyPrivate( booter, times( 1 ) )
                .invoke( "exit1" );

        verifyNoMoreInteractions( booter );
    }
}
