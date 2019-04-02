package org.apache.maven.plugin.surefire.log.api;

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

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.internal.matchers.CapturesArguments;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ConsoleLoggerDecorator}, {@link NullConsoleLogger} and {@link PrintStreamLogger}.
 */
public class LoggersTest
{
    @Test
    public void testPrintStreamLogger()
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream( outputStream );
        PrintStreamLogger logger = new PrintStreamLogger( printStream );


        assertThat( logger.isErrorEnabled() ).isTrue();
        assertThat( logger.isWarnEnabled() ).isTrue();
        assertThat( logger.isInfoEnabled() ).isTrue();
        assertThat( logger.isDebugEnabled() ).isTrue();

        logger.error( "error" );
        logger.debug( "debug" );
        logger.info( "info" );
        logger.warning( "warning" );

        String line = System.lineSeparator();
        assertThat( outputStream.toString() )
                .isEqualTo( "error" + line + "debug" + line + "info" + line + "warning" + line );

        Exception e = new Exception( "exception" );
        outputStream.reset();
        logger.error( e );
        assertThat( outputStream.toString() )
                .contains( "java.lang.Exception: exception" )
                .contains( "at " + getClass().getName() + ".testPrintStreamLogger(LoggersTest.java:63)" );
    }

    @Test( expected = NullPointerException.class )
    public void shouldThrowNPE()
    {
        new ConsoleLoggerDecorator( null );
    }

    @Test
    public void testDecorator()
    {
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ConsoleLoggerDecorator decorator = new ConsoleLoggerDecorator( logger );

        assertThat( decorator.isDebugEnabled() ).isFalse();
        when( logger.isDebugEnabled() ).thenReturn( true );
        assertThat( decorator.isDebugEnabled() ).isTrue();

        assertThat( decorator.isInfoEnabled() ).isFalse();
        when( logger.isInfoEnabled() ).thenReturn( true );
        assertThat( decorator.isInfoEnabled() ).isTrue();

        assertThat( decorator.isWarnEnabled() ).isFalse();
        when( logger.isWarnEnabled() ).thenReturn( true );
        assertThat( decorator.isWarnEnabled() ).isTrue();

        assertThat( decorator.isErrorEnabled() ).isFalse();
        when( logger.isErrorEnabled() ).thenReturn( true );
        assertThat( decorator.isErrorEnabled() ).isTrue();

        ArgumentCaptor<String> argumentMsg = ArgumentCaptor.forClass( String.class );
        decorator.debug( "debug" );
        verify( logger, times( 1 ) ).debug( argumentMsg.capture() );
        assertThat( argumentMsg.getAllValues() ).hasSize( 1 );
        assertThat( argumentMsg.getAllValues().get( 0 ) ).isEqualTo( "debug" );

        argumentMsg = ArgumentCaptor.forClass( String.class );
        decorator.info( "info" );
        verify( logger, times( 1 ) ).info( argumentMsg.capture() );
        assertThat( argumentMsg.getAllValues() ).hasSize( 1 );
        assertThat( argumentMsg.getAllValues().get( 0 ) ).isEqualTo( "info" );

        argumentMsg = ArgumentCaptor.forClass( String.class );
        decorator.warning( "warning" );
        verify( logger, times( 1 ) ).warning( argumentMsg.capture() );
        assertThat( argumentMsg.getAllValues() ).hasSize( 1 );
        assertThat( argumentMsg.getAllValues().get( 0 ) ).isEqualTo( "warning" );

        argumentMsg = ArgumentCaptor.forClass( String.class );
        decorator.error( "error" );
        verify( logger, times( 1 ) ).error( argumentMsg.capture() );
        assertThat( argumentMsg.getAllValues() ).hasSize( 1 );
        assertThat( argumentMsg.getAllValues().get( 0 ) ).isEqualTo( "error" );

        ArgumentCaptor<Throwable> argumentThrowable = ArgumentCaptor.forClass( Throwable.class );
        argumentMsg = ArgumentCaptor.forClass( String.class );
        Exception e = new Exception();
        decorator.error( "error", e );
        verify( logger, times( 1 ) ).error( argumentMsg.capture(), argumentThrowable.capture() );
        assertThat( argumentMsg.getAllValues() ).hasSize( 1 );
        assertThat( argumentMsg.getAllValues().get( 0 ) ).isEqualTo( "error" );
        assertThat( argumentThrowable.getAllValues() ).hasSize( 1 );
        assertThat( argumentThrowable.getAllValues().get( 0 ) ).isSameAs( e );

        argumentThrowable = ArgumentCaptor.forClass( Throwable.class );
        decorator.error( e );
        verify( logger, times( 1 ) ).error( argumentThrowable.capture() );
        assertThat( argumentThrowable.getAllValues() ).hasSize( 1 );
        assertThat( argumentThrowable.getAllValues().get( 0 ) ).isSameAs( e );
    }
}
