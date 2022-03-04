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

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Events sent back to the plugin process.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
@SuppressWarnings( "checkstyle:linelength" )
public enum ForkedProcessEventType
{
    /**
     * This is the opcode "sys-prop". The frame is composed of segments and the separator characters ':'
     * <pre>
     * :maven-surefire-event:sys-prop:RunMode:0x0000000100000000:5:UTF-8:0xFFFFFFFF:key:0xFFFFFFFF:value:
     * </pre>
     * The constructor with one argument:
     * <ul>
     *     <li>the opcode is "sys-prop"
     * </ul>
     */
    BOOTERCODE_SYSPROPS( "sys-prop"  ),

    /**
     * This is the opcode "testset-starting". The frame is composed of segments and the separator characters ':'
     * <pre>
     * :maven-surefire-event:testset-starting:RunMode:0x0000000100000000:5:UTF-8:0xFFFFFFFF:SourceName:0xFFFFFFFF:SourceText:0xFFFFFFFF:Name:0xFFFFFFFF:NameText:0xFFFFFFFF:Group:0xFFFFFFFF:Message:ElapsedTime (binary int):0xFFFFFFFF:LocalizedMessage:0xFFFFFFFF:SmartTrimmedStackTrace:0xFFFFFFFF:toStackTrace( stw, trimStackTraces ):
     * </pre>
     * The constructor with one argument:
     * <ul>
     *     <li>the opcode is "testset-starting"
     * </ul>
     */
    BOOTERCODE_TESTSET_STARTING( "testset-starting" ),

    /**
     * This is the opcode "testset-completed". The frame is composed of segments and the separator characters ':'
     * <pre>
     * :maven-surefire-event:testset-completed:RunMode:0x0000000100000000:5:UTF-8:0xFFFFFFFF:SourceName:0xFFFFFFFF:SourceText:0xFFFFFFFF:Name:0xFFFFFFFF:NameText:0xFFFFFFFF:Group:0xFFFFFFFF:Message:ElapsedTime (binary int):0xFFFFFFFF:LocalizedMessage:0xFFFFFFFF:SmartTrimmedStackTrace:0xFFFFFFFF:toStackTrace( stw, trimStackTraces ):
     * </pre>
     * The constructor with one argument:
     * <ul>
     *     <li>the opcode is "testset-completed"
     * </ul>
     */
    BOOTERCODE_TESTSET_COMPLETED( "testset-completed" ),

    /**
     * This is the opcode "test-starting". The frame is composed of segments and the separator characters ':'
     * <pre>
     * :maven-surefire-event:test-starting:RunMode:0x0000000100000001:5:UTF-8:0xFFFFFFFF:SourceName:0xFFFFFFFF:SourceText:0xFFFFFFFF:Name:0xFFFFFFFF:NameText:0xFFFFFFFF:Group:0xFFFFFFFF:Message:ElapsedTime (binary int):0xFFFFFFFF:LocalizedMessage:0xFFFFFFFF:SmartTrimmedStackTrace:0xFFFFFFFF:toStackTrace( stw, trimStackTraces ):
     * </pre>
     * The constructor with one argument:
     * <ul>
     *     <li>the opcode is "test-starting"
     * </ul>
     */
    BOOTERCODE_TEST_STARTING( "test-starting" ),

    /**
     * This is the opcode "test-succeeded". The frame is composed of segments and the separator characters ':'
     * <pre>
     * :maven-surefire-event:test-succeeded:RunMode:0x0000000100000001:5:UTF-8:0xFFFFFFFF:SourceName:0xFFFFFFFF:SourceText:0xFFFFFFFF:Name:0xFFFFFFFF:NameText:0xFFFFFFFF:Group:0xFFFFFFFF:Message:ElapsedTime (binary int):0xFFFFFFFF:LocalizedMessage:0xFFFFFFFF:SmartTrimmedStackTrace:0xFFFFFFFF:toStackTrace( stw, trimStackTraces ):
     * </pre>
     * The constructor with one argument:
     * <ul>
     *     <li>the opcode is "test-succeeded"
     * </ul>
     */
    BOOTERCODE_TEST_SUCCEEDED( "test-succeeded" ),

    /**
     * This is the opcode "test-failed". The frame is composed of segments and the separator characters ':'
     * <pre>
     * :maven-surefire-event:test-failed:RunMode:0x0000000100000001:5:UTF-8:0xFFFFFFFF:SourceName:0xFFFFFFFF:SourceText:0xFFFFFFFF:Name:0xFFFFFFFF:NameText:0xFFFFFFFF:Group:0xFFFFFFFF:Message:ElapsedTime (binary int):0xFFFFFFFF:LocalizedMessage:0xFFFFFFFF:SmartTrimmedStackTrace:0xFFFFFFFF:toStackTrace( stw, trimStackTraces ):
     * </pre>
     * The constructor with one argument:
     * <ul>
     *     <li>the opcode is "test-failed"
     * </ul>
     */
    BOOTERCODE_TEST_FAILED( "test-failed" ),

    /**
     * This is the opcode "test-skipped". The frame is composed of segments and the separator characters ':'
     * <pre>
     * :maven-surefire-event:test-skipped:RunMode:0x0000000100000001:5:UTF-8:0xFFFFFFFF:SourceName:0xFFFFFFFF:SourceText:0xFFFFFFFF:Name:0xFFFFFFFF:NameText:0xFFFFFFFF:Group:0xFFFFFFFF:Message:ElapsedTime (binary int):0xFFFFFFFF:LocalizedMessage:0xFFFFFFFF:SmartTrimmedStackTrace:0xFFFFFFFF:toStackTrace( stw, trimStackTraces ):
     * </pre>
     * The constructor with one argument:
     * <ul>
     *     <li>the opcode is "test-skipped"
     * </ul>
     */
    BOOTERCODE_TEST_SKIPPED( "test-skipped" ),

    /**
     * This is the opcode "test-error". The frame is composed of segments and the separator characters ':'
     * <pre>
     * :maven-surefire-event:test-error:RunMode:0x0000000100000001:5:UTF-8:0xFFFFFFFF:SourceName:0xFFFFFFFF:SourceText:0xFFFFFFFF:Name:0xFFFFFFFF:NameText:0xFFFFFFFF:Group:0xFFFFFFFF:Message:ElapsedTime (binary int):0xFFFFFFFF:LocalizedMessage:0xFFFFFFFF:SmartTrimmedStackTrace:0xFFFFFFFF:toStackTrace( stw, trimStackTraces ):
     * </pre>
     * The constructor with one argument:
     * <ul>
     *     <li>the opcode is "test-error"
     * </ul>
     */
    BOOTERCODE_TEST_ERROR( "test-error" ),

    /**
     * This is the opcode "test-assumption-failure". The frame is composed of segments and the separator characters ':'
     * <pre>
     * :maven-surefire-event:test-assumption-failure:RunMode:0x0000000100000001:5:UTF-8:0xFFFFFFFF:SourceName:0xFFFFFFFF:SourceText:0xFFFFFFFF:Name:0xFFFFFFFF:NameText:0xFFFFFFFF:Group:0xFFFFFFFF:Message:ElapsedTime (binary int):0xFFFFFFFF:LocalizedMessage:0xFFFFFFFF:SmartTrimmedStackTrace:0xFFFFFFFF:toStackTrace( stw, trimStackTraces ):
     * </pre>
     * The constructor with one argument:
     * <ul>
     *     <li>the opcode is "test-assumption-failure"
     * </ul>
     */
    BOOTERCODE_TEST_ASSUMPTIONFAILURE( "test-assumption-failure" ),

    /**
     * This is the opcode "std-out-stream". The frame is composed of segments and the separator characters ':'
     * <pre>
     * :maven-surefire-event:std-out-stream:RunMode:0x0000000100000001:5:UTF-8:0xFFFFFFFF:line:
     * </pre>
     * The constructor with one argument:
     * <ul>
     *     <li>the opcode is "std-out-stream"
     * </ul>
     */
    BOOTERCODE_STDOUT( "std-out-stream" ),

    /**
     * This is the opcode "std-out-stream-new-line". The frame is composed of segments and the separator characters ':'
     * <pre>
     * :maven-surefire-event:std-out-stream-new-line:RunMode:0x0000000100000001:5:UTF-8:0xFFFFFFFF:line:
     * </pre>
     * The constructor with one argument:
     * <ul>
     *     <li>the opcode is "std-out-stream-new-line"
     * </ul>
     */
    BOOTERCODE_STDOUT_NEW_LINE( "std-out-stream-new-line" ),

    /**
     * This is the opcode "std-err-stream". The frame is composed of segments and the separator characters ':'
     * <pre>
     * :maven-surefire-event:std-err-stream:RunMode:0x0000000100000001:5:UTF-8:0xFFFFFFFF:line:
     * </pre>
     * The constructor with one argument:
     * <ul>
     *     <li>the opcode is "std-err-stream"
     * </ul>
     */
    BOOTERCODE_STDERR( "std-err-stream" ),

    /**
     * This is the opcode "std-err-stream-new-line". The frame is composed of segments and the separator characters ':'
     * <pre>
     * :maven-surefire-event:std-err-stream-new-line:RunMode:0x0000000100000001:5:UTF-8:0xFFFFFFFF:line:
     * </pre>
     * The constructor with one argument:
     * <ul>
     *     <li>the opcode is "std-err-stream-new-line"
     * </ul>
     */
    BOOTERCODE_STDERR_NEW_LINE( "std-err-stream-new-line" ),

    /**
     * This is the opcode "console-info-log". The frame is composed of segments and the separator characters ':'
     * <pre>
     * :maven-surefire-event:console-info-log:RunMode:5:UTF-8:0xFFFFFFFF:line:
     * </pre>
     * The constructor with one argument:
     * <ul>
     *     <li>the opcode is "console-info-log"
     * </ul>
     */
    BOOTERCODE_CONSOLE_INFO( "console-info-log" ),

    /**
     * This is the opcode "console-debug-log". The frame is composed of segments and the separator characters ':'
     * <pre>
     * :maven-surefire-event:console-debug-log:RunMode:5:UTF-8:0xFFFFFFFF:line:
     * </pre>
     * The constructor with one argument:
     * <ul>
     *     <li>the opcode is "console-debug-log"
     * </ul>
     */
    BOOTERCODE_CONSOLE_DEBUG( "console-debug-log" ),

    /**
     * This is the opcode "console-warning-log". The frame is composed of segments and the separator characters ':'
     * <pre>
     * :maven-surefire-event:console-warning-log:RunMode:5:UTF-8:0xFFFFFFFF:line:
     * </pre>
     * The constructor with one argument:
     * <ul>
     *     <li>the opcode is "console-warning-log"
     * </ul>
     */
    BOOTERCODE_CONSOLE_WARNING( "console-warning-log" ),

    /**
     * This is the opcode "console-error-log". The frame is composed of segments and the separator characters ':'
     * <pre>
     * :maven-surefire-event:console-error-log:RunMode:5:UTF-8:0xFFFFFFFF:line:
     * </pre>
     * The constructor with one argument:
     * <ul>
     *     <li>the opcode is "console-error-log"
     * </ul>
     */
    BOOTERCODE_CONSOLE_ERROR( "console-error-log" ),

    /**
     * This is the opcode "bye". The frame is composed of segments and the separator characters ':'
     * <pre>
     * :maven-surefire-event:bye:
     * </pre>
     * The constructor with one argument:
     * <ul>
     *     <li>the opcode is "bye"
     * </ul>
     */
    BOOTERCODE_BYE( "bye" ),

    /**
     * This is the opcode "stop-on-next-test". The frame is composed of segments and the separator characters ':'
     * <pre>
     * :maven-surefire-event:stop-on-next-test:
     * </pre>
     * The constructor with one argument:
     * <ul>
     *     <li>the opcode is "stop-on-next-test"
     * </ul>
     */
    BOOTERCODE_STOP_ON_NEXT_TEST( "stop-on-next-test" ),

    /**
     * This is the opcode "next-test". The frame is composed of segments and the separator characters ':'
     * <pre>
     * :maven-surefire-event:next-test:
     * </pre>
     * The constructor with one argument:
     * <ul>
     *     <li>the opcode is "next-test"
     * </ul>
     */
    BOOTERCODE_NEXT_TEST( "next-test" ),

    /**
     * This is the opcode "jvm-exit-error". The frame is composed of segments and the separator characters ':'
     * <pre>
     * :maven-surefire-event:jvm-exit-error:
     * </pre>
     * The constructor with one argument:
     * <ul>
     *     <li>the opcode is "jvm-exit-error"
     * </ul>
     */
    BOOTERCODE_JVM_EXIT_ERROR( "jvm-exit-error" );

    private final String opcode;
    private final byte[] opcodeBinary;

    ForkedProcessEventType( String opcode )
    {
        this.opcode = opcode;
        opcodeBinary = opcode.getBytes( US_ASCII );
    }

    public String getOpcode()
    {
        return opcode;
    }

    public byte[] getOpcodeBinary()
    {
        return opcodeBinary;
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
}
