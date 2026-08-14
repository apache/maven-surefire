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
package org.apache.maven.plugin.surefire.report;

import java.util.Objects;

/**
 * Identity used to aggregate the executions of one test across reruns. Providers
 * without a test run ID retain the legacy class-and-method-name grouping.
 */
final class TestMethodKey implements Comparable<TestMethodKey> {
    private final String testClassMethodName;

    private final Long testRunId;

    TestMethodKey(String testClassMethodName, Long testRunId) {
        this.testClassMethodName = testClassMethodName;
        this.testRunId = testRunId;
    }

    String getTestClassMethodName() {
        return testClassMethodName;
    }

    @Override
    public int compareTo(TestMethodKey other) {
        int byName = compare(testClassMethodName, other.testClassMethodName);
        return byName == 0 ? compare(testRunId, other.testRunId) : byName;
    }

    private static <T extends Comparable<T>> int compare(T left, T right) {
        if (left == null) {
            return right == null ? 0 : -1;
        }
        return right == null ? 1 : left.compareTo(right);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TestMethodKey)) {
            return false;
        }
        TestMethodKey that = (TestMethodKey) other;
        return Objects.equals(testClassMethodName, that.testClassMethodName)
                && Objects.equals(testRunId, that.testRunId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testClassMethodName, testRunId);
    }
}
