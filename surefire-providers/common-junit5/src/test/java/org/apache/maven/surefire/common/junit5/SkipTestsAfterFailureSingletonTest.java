package org.apache.maven.surefire.common.junit5;

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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.reflect.Whitebox.getInternalState;

/**
 *
 */
public class SkipTestsAfterFailureSingletonTest
{
    @Test
    public void shouldBeActive()
    {
        boolean[] onThresholdReached = {false};
        SkipTestsAfterFailureSingleton singleton = SkipTestsAfterFailureSingleton.getSingleton();
        singleton.setDisableTests( false );
        singleton.init( 2, () -> onThresholdReached[0] = true );

        TestExecutionWatcherSPI spi = new TestExecutionWatcherSPI();

        spi.testFailed( null, null );
        assertFalse( singleton.isDisableTests() );
        assertFalse( spi.evaluateExecutionCondition( null ).isDisabled() );
        assertTrue( onThresholdReached[0] );

        spi.testFailed( null, null );
        assertTrue( singleton.isDisableTests() );
        assertTrue( spi.evaluateExecutionCondition( null ).isDisabled() );
        assertTrue( onThresholdReached[0] );
    }

    @Test
    public void shouldNotBeActive()
    {
        boolean[] onThresholdReached = {false};
        SkipTestsAfterFailureSingleton singleton = SkipTestsAfterFailureSingleton.getSingleton();
        singleton.setDisableTests( false );
        singleton.init( 0, () -> onThresholdReached[0] = true );

        TestExecutionWatcherSPI spi = new TestExecutionWatcherSPI();

        for ( int i = 0; i < 10; i++ )
        {
            spi.testFailed( null, null );
            assertFalse( singleton.isDisableTests() );
            assertFalse( spi.evaluateExecutionCondition( null ).isDisabled() );
            assertFalse( onThresholdReached[0] );
        }
    }

    @Test
    public void shouldNotBeActiveIfRerun()
    {
        boolean[] onThresholdReached = {false};
        SkipTestsAfterFailureSingleton singleton = SkipTestsAfterFailureSingleton.getSingleton();
        singleton.setDisableTests( false );
        singleton.init( 1, () -> onThresholdReached[0] = true );
        assertTrue( getInternalState( singleton, "active" ) );
        singleton.setReRunMode();
        assertFalse( getInternalState( singleton, "active" ) );
        assertFalse( singleton.isDisableTests() );
        assertFalse( onThresholdReached[0] );
        singleton.runIfFailureCountReachedThreshold();
        assertFalse( singleton.isDisableTests() );
        assertFalse( onThresholdReached[0] );
    }
}
