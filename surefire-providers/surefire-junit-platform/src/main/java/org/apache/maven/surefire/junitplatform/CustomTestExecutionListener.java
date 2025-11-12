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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

public class CustomTestExecutionListener implements TestExecutionListener {

    private List<Object> listeners;

    public CustomTestExecutionListener(List<Object> runListener) {
        this.listeners = runListener;
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {

        String mainTestClass = extractMainClassName(testPlan);
        listeners.forEach(runListener -> {
            try {
                Class<?> descriptionClass =
                        Thread.currentThread().getContextClassLoader().loadClass("org.junit.runner.Description");
                Method createSuiteDescription = descriptionClass.getMethod("createSuiteDescription", Class.class);
                if (mainTestClass != null) {
                    Class<?> classToRemove =
                            Thread.currentThread().getContextClassLoader().loadClass(mainTestClass);
                    Object invoke = createSuiteDescription.invoke(descriptionClass, classToRemove);
                    runListener.getClass()
                            .getMethod("testRunStarted", descriptionClass)
                            .invoke(runListener, invoke);
                    runListener.getClass()
                            .getMethod("testSuiteStarted", descriptionClass)
                            .invoke(runListener, invoke);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {

        String mainTestClass = extractMainClassName(testPlan);
        listeners.forEach(runListener -> {
            try {
                Class<?> resultClass =
                        Thread.currentThread().getContextClassLoader().loadClass("org.junit.runner.Result");
                Constructor<?> resultClassConstructor = resultClass.getConstructor();
                if (mainTestClass != null) {
                    Thread.currentThread().getContextClassLoader().loadClass(mainTestClass);
                    Object invoke = resultClassConstructor.newInstance();
                    runListener.getClass().getMethod("testRunFinished", resultClass).invoke(runListener, invoke);

                    Class<?> descriptionClass =
                            Thread.currentThread().getContextClassLoader().loadClass("org.junit.runner.Description");
                    Method createSuiteDescription = descriptionClass.getMethod("createSuiteDescription", Class.class);
                    Class<?> classToRemove =
                            Thread.currentThread().getContextClassLoader().loadClass(mainTestClass);
                    Object createSuiteDescInvoke = createSuiteDescription.invoke(descriptionClass, classToRemove);
                    runListener.getClass()
                            .getMethod("testSuiteFinished", descriptionClass)
                            .invoke(runListener, createSuiteDescInvoke);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String extractMainClassName(TestPlan testPlan) {
        for (Object identifier : testPlan.getRoots().toArray()) {
            for (TestIdentifier child : testPlan.getChildren((TestIdentifier) identifier)) {
                if (child.getType().isContainer()) {
                    if (child.getSource().isPresent() && child.getSource().get() instanceof ClassSource) {
                        return ((ClassSource) child.getSource().get()).getClassName();
                    }
                }
            }
        }
        return null;
    }
}
