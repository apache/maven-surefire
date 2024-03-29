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
package org.apache.maven.surefire.junit4;

import org.apache.maven.surefire.api.testset.TestListResolver;
import org.apache.maven.surefire.api.util.internal.ClassMethod;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import static org.apache.maven.surefire.api.testset.TestListResolver.toClassFileName;
import static org.apache.maven.surefire.common.junit4.JUnit4ProviderUtil.toClassMethod;

/**
 * Method filter used in {@link JUnit4Provider}.
 */
final class TestResolverFilter extends Filter {
    private final TestListResolver methodFilter;

    TestResolverFilter(TestListResolver methodFilter) {
        this.methodFilter = methodFilter;
    }

    @Override
    public boolean shouldRun(Description description) {
        // class: Java class name; method: 1. "testMethod" or 2. "testMethod[5+whatever]" in @Parameterized
        final ClassMethod cm = toClassMethod(description);
        final boolean isSuite = description.isSuite();
        final boolean isValidTest = description.isTest() && cm.isValidTest();
        final String clazz = cm.getClazz();
        final String method = cm.getMethod();
        return isSuite || isValidTest && methodFilter.shouldRun(toClassFileName(clazz), method);
    }

    @Override
    public String describe() {
        return methodFilter.toString();
    }
}
