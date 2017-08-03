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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.unmodifiableMap;

/**
 * Events sent back to the plugin process.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
 */
public enum ForkedProcessEvent
{
    BOOTERCODE_SYSPROPS( "sysProp", 0 ),

    BOOTERCODE_TESTSET_STARTING( "testSetStarting", 1 ),
    BOOTERCODE_TESTSET_COMPLETED( "testSetCompleted", 1 ),
    BOOTERCODE_TEST_STARTING( "testStarting", 1 ),
    BOOTERCODE_TEST_SUCCEEDED( "testSucceeded", 1 ),
    BOOTERCODE_TEST_FAILED( "testFailed", 1 ),
    BOOTERCODE_TEST_SKIPPED( "testSkipped", 1 ),
    BOOTERCODE_TEST_ERROR( "testError", 1 ),
    BOOTERCODE_TEST_ASSUMPTIONFAILURE( "testAssumptionFailure", 1 ),

    BOOTERCODE_STDOUT( "stdOutStream", 2 ),
    BOOTERCODE_STDERR( "stdErrStream", 2 ),

    BOOTERCODE_CONSOLE_INFO( "console", 3 ),
    BOOTERCODE_CONSOLE_DEBUG( "debug", 3 ),
    BOOTERCODE_CONSOLE_WARNING( "warning", 3 ),
    BOOTERCODE_CONSOLE_ERROR( "error", 4 ),

    BOOTERCODE_BYE( "bye", 5 ),
    BOOTERCODE_STOP_ON_NEXT_TEST( "stopOnNextTest", 5 ),
    BOOTERCODE_NEXT_TEST( "nextTest", 5 ),

    BOOTERCODE_JVM_EXIT_ERROR( "jvmExitError", 6 );

    public static final String MAGIC_NUMBER = ":maven:surefire:std:out:";

    public static final Map<String, ForkedProcessEvent> EVENTS = events();

    private static Map<String, ForkedProcessEvent> events()
    {
        Map<String, ForkedProcessEvent> events = new ConcurrentHashMap<String, ForkedProcessEvent>();
        for ( ForkedProcessEvent event : values() )
        {
            events.put( event.getOpcode(), event );
        }
        return unmodifiableMap( events );
    }


    private final String opcode;
    private final int category;

    ForkedProcessEvent( String opcode, int category )
    {
        this.opcode = opcode;
        this.category = category;
    }

    public String getOpcode()
    {
        return opcode;
    }

    public boolean isSysPropCategory()
    {
        return category == 0;
    }

    public boolean isTestCategory()
    {
        return category == 1;
    }

    public boolean isStandardStreamCategory()
    {
        return category == 2;
    }

    public boolean isConsoleCategory()
    {
        return category == 3;
    }

    public boolean isConsoleErrorCategory()
    {
        return category == 4;
    }

    public boolean isControlCategory()
    {
        return category == 5;
    }

    public boolean isJvmExitError()
    {
        return category == 6;
    }
}
