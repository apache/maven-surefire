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

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.unmodifiableMap;

/**
 * Events sent back to the plugin process.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
public enum ForkedProcessEventType
{
    BOOTERCODE_SYSPROPS( "sys-prop" ),

    BOOTERCODE_TESTSET_STARTING( "testset-starting" ),
    BOOTERCODE_TESTSET_COMPLETED( "testset-completed" ),
    BOOTERCODE_TEST_STARTING( "test-starting" ),
    BOOTERCODE_TEST_SUCCEEDED( "test-succeeded" ),
    BOOTERCODE_TEST_FAILED( "test-failed" ),
    BOOTERCODE_TEST_SKIPPED( "test-skipped" ),
    BOOTERCODE_TEST_ERROR( "test-error" ),
    BOOTERCODE_TEST_ASSUMPTIONFAILURE( "test-assumption-failure" ),

    BOOTERCODE_STDOUT( "std-out-stream" ),
    BOOTERCODE_STDOUT_NEW_LINE( "std-out-stream-new-line" ),
    BOOTERCODE_STDERR( "std-err-stream" ),
    BOOTERCODE_STDERR_NEW_LINE( "std-err-stream-new-line" ),

    BOOTERCODE_CONSOLE_INFO( "console-info-log" ),
    BOOTERCODE_CONSOLE_DEBUG( "console-debug-log" ),
    BOOTERCODE_CONSOLE_WARNING( "console-warning-log" ),
    BOOTERCODE_CONSOLE_ERROR( "console-error-log" ),

    BOOTERCODE_BYE( "bye" ),
    BOOTERCODE_STOP_ON_NEXT_TEST( "stop-on-next-test" ),
    BOOTERCODE_NEXT_TEST( "next-test" ),

    BOOTERCODE_JVM_EXIT_ERROR( "jvm-exit-error" );

    public static final String MAGIC_NUMBER = "maven-surefire-event";

    private static final Map<String, ForkedProcessEventType> EVENTS = events();

    private static Map<String, ForkedProcessEventType> events()
    {
        Map<String, ForkedProcessEventType> events = new ConcurrentHashMap<>();
        for ( ForkedProcessEventType event : values() )
        {
            events.put( event.getOpcode(), event );
        }
        return unmodifiableMap( events );
    }

    private final String opcode;

    ForkedProcessEventType( String opcode )
    {
        this.opcode = opcode;
    }

    public String getOpcode()
    {
        return opcode;
    }

    public boolean isSysPropCategory()
    {
        return this == BOOTERCODE_SYSPROPS;
    }

    public boolean isTestCategory()
    {
        return this == BOOTERCODE_TESTSET_STARTING
                || this == BOOTERCODE_TESTSET_COMPLETED
                || this == BOOTERCODE_TEST_STARTING
                || this == BOOTERCODE_TEST_SUCCEEDED
                || this == BOOTERCODE_TEST_FAILED
                || this == BOOTERCODE_TEST_SKIPPED
                || this == BOOTERCODE_TEST_ERROR
                || this == BOOTERCODE_TEST_ASSUMPTIONFAILURE;
    }

    public boolean isStandardStreamCategory()
    {
        return this == BOOTERCODE_STDOUT || this == BOOTERCODE_STDOUT_NEW_LINE
                || this == BOOTERCODE_STDERR || this == BOOTERCODE_STDERR_NEW_LINE;
    }

    public boolean isConsoleCategory()
    {
        return this == BOOTERCODE_CONSOLE_INFO
                || this == BOOTERCODE_CONSOLE_DEBUG
                || this == BOOTERCODE_CONSOLE_WARNING;
    }

    public boolean isConsoleErrorCategory()
    {
        return this == BOOTERCODE_CONSOLE_ERROR;
    }

    public boolean isControlCategory()
    {
        return this == BOOTERCODE_BYE || this == BOOTERCODE_STOP_ON_NEXT_TEST || this == BOOTERCODE_NEXT_TEST;
    }

    public boolean isJvmExitError()
    {
        return this == BOOTERCODE_JVM_EXIT_ERROR;
    }

    public static ForkedProcessEventType byOpcode( @Nonnull String opcode )
    {
        return EVENTS.get( opcode );
    }
}
