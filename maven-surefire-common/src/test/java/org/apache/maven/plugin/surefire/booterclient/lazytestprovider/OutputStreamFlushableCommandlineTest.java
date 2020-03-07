package org.apache.maven.plugin.surefire.booterclient.lazytestprovider;

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

import org.apache.maven.surefire.shared.utils.cli.CommandLineException;
import org.fest.assertions.Condition;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.verifyZeroInteractions;

/**
 *
 */
public class OutputStreamFlushableCommandlineTest
{

    @Test
    public void shouldGetEnvironmentVariables()
    {
        OutputStreamFlushableCommandline cli = new OutputStreamFlushableCommandline();
        String[] env = cli.getEnvironmentVariables();

        assertThat( env )
                .doesNotHaveDuplicates()
                .satisfies( new ContainsAnyStartsWith( "JAVA_HOME=" ) );

        String[] excluded = { "JAVA_HOME" };
        cli = new OutputStreamFlushableCommandline( excluded );
        env = cli.getEnvironmentVariables();

        assertThat( env )
                .doesNotHaveDuplicates()
                .satisfies( new NotContainsAnyStartsWith( "JAVA_HOME=" ) );
    }

    @Test
    public void shouldExecute() throws CommandLineException
    {
        OutputStreamFlushableCommandline cli = new OutputStreamFlushableCommandline();
        cli.getShell().setWorkingDirectory( System.getProperty( "user.dir" ) );
        cli.getShell().setExecutable( IS_OS_WINDOWS ? "dir" : "ls" );
        assertThat( cli.getFlushReceiver() ).isNull();
        cli.execute();
        assertThat( cli.getFlushReceiver() ).isNotNull();
    }

    @Test
    public void shouldGetFlushReceiver()
    {
        OutputStreamFlushableCommandline cli = new OutputStreamFlushableCommandline();
        assertThat( cli.getFlushReceiver() ).isNull();
    }

    @Test
    public void shouldFlush() throws IOException
    {
        ByteArrayOutputStream os = mock( ByteArrayOutputStream.class );
        OutputStreamFlushReceiver flushReceiver = new OutputStreamFlushReceiver( os );
        verifyZeroInteractions( os );
        flushReceiver.flush();
        verify( os, times( 1 ) ).flush();
    }

    private static final class ContainsAnyStartsWith extends Condition<Object[]>
    {
        private final String expected;

        ContainsAnyStartsWith( String expected )
        {
            this.expected = expected;
        }

        @Override
        public boolean matches( Object[] values )
        {
            boolean matches = false;
            for ( Object value : values )
            {
                assertThat( value ).isInstanceOf( String.class );
                matches |= ( (String) value ).startsWith( expected );
            }
            return matches;
        }
    }

    private static final class NotContainsAnyStartsWith extends Condition<Object[]>
    {
        private final String expected;

        NotContainsAnyStartsWith( String expected )
        {
            this.expected = expected;
        }

        @Override
        public boolean matches( Object[] values )
        {
            boolean matches = false;
            for ( Object value : values )
            {
                assertThat( value ).isInstanceOf( String.class );
                matches |= ( (String) value ).startsWith( expected );
            }
            return !matches;
        }
    }
}
