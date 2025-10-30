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

import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public class CustomTestExecutionListener implements TestExecutionListener {

    private List<Object> listeners;

    private String mainTestClass = null;

    public CustomTestExecutionListener(List<Object> runListener) {
        this.listeners = runListener;
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {

        listeners.forEach(plan -> {
            try {
                Class<?> descriptionClass = null;
                descriptionClass =
                    Thread.currentThread().getContextClassLoader().loadClass("org.junit.runner.Description");
                Method createSuiteDescription = descriptionClass.getMethod("createSuiteDescription", Class.class);
                if (mainTestClass != null) {
                    Class<?> classToRemove =
                        Thread.currentThread().getContextClassLoader().loadClass(mainTestClass);
                    Object invoke = createSuiteDescription.invoke(descriptionClass, classToRemove);
                    plan.getClass()
                        .getMethod("testRunStarted", descriptionClass)
                        .invoke(plan, invoke);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        Optional<TestSource> testSource = testIdentifier.getSource();
        if (testIdentifier.isTest() && testSource.isPresent()) {
            mainTestClass = ((MethodSource) testSource.get()).getClassName();
        }
    }
}
