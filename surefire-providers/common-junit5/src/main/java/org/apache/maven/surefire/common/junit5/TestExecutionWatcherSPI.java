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

import java.util.Optional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.TestWatcher;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;

/**
 * Supports the parameter <code>skipAfterFailureCount</code>.
 * <br>
 * Disables the execution if the number of failures has exceeded
 * the threshold defined in <code>skipAfterFailureCount</code>.
 *
 * @since 3.0.0-M5
 */
public class TestExecutionWatcherSPI
    implements ExecutionCondition, TestWatcher
{
    private static final SkipTestsAfterFailureSingleton FAILURES_COUNTER =
        SkipTestsAfterFailureSingleton.getSingleton();

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition( ExtensionContext context )
    {
        return FAILURES_COUNTER.isDisableTests() ? disabled( "" ) : enabled( "" );
    }

    @Override
    public void testDisabled( ExtensionContext extensionContext, Optional<String> optional )
    {

    }

    @Override
    public void testSuccessful( ExtensionContext extensionContext )
    {

    }

    @Override
    public void testAborted( ExtensionContext extensionContext, Throwable throwable )
    {

    }

    @Override
    public void testFailed( ExtensionContext extensionContext, Throwable throwable )
    {
        FAILURES_COUNTER.runIfFailureCountReachedThreshold();
    }
}
