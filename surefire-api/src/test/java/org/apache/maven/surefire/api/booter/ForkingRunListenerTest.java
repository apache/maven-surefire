package org.apache.maven.surefire.api.booter;

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

import junit.framework.TestCase;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author <a href="mailto:kristian.rosenvold@gmail.com">Kristian Rosenvold</a>
 */
public class ForkingRunListenerTest
    extends TestCase
{
    public void testInfo()
    {
        MasterProcessChannelEncoder encoder = mock( MasterProcessChannelEncoder.class );
        ArgumentCaptor<String> argument1 = ArgumentCaptor.forClass( String.class );
        doNothing().when( encoder ).consoleInfoLog( anyString() );
        ForkingRunListener forkingRunListener = new ForkingRunListener( encoder, true );
        forkingRunListener.info( new String( new byte[]{ (byte) 'A' } ) );
        forkingRunListener.info( new String( new byte[]{ } ) );
        verify( encoder, times( 2 ) ).consoleInfoLog( argument1.capture() );
        assertThat( argument1.getAllValues() )
            .hasSize( 2 )
            .containsSequence( "A", "" );
        verifyNoMoreInteractions( encoder );
    }
}
