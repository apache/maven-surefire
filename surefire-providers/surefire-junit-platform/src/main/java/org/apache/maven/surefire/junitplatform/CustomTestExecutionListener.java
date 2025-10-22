/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.surefire.junitplatform;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

public class CustomTestExecutionListener implements TestExecutionListener {

    private RunListener runListener;

    public CustomTestExecutionListener(RunListener runListener) {
        this.runListener = runListener;
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        try {
            runListener.testFinished(Description.createSuiteDescription(runListener.getClass()));
        } catch (Exception e) {
            // TODO: I have no clue, what could happen here
            throw new RuntimeException(e);
        }
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        try {
            runListener.testRunStarted(Description.createSuiteDescription(runListener.getClass()));
        } catch (Exception e) {
            // TODO: I have no clue, what could happen here
            throw new RuntimeException(e);
        }
    }
}
