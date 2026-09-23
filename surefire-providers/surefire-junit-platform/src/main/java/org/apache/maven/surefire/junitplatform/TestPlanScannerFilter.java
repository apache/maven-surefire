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

import org.apache.maven.surefire.api.util.ScannerFilter;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.engine.Filter;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

/**
 * @since 2.22.0
 */
final class TestPlanScannerFilter implements ScannerFilter {

    private final LauncherAdapter launcher;

    private final Filter<?>[] includeAndExcludeFilters;

    TestPlanScannerFilter(LauncherAdapter launcher, Filter<?>[] includeAndExcludeFilters) {
        this.launcher = launcher;
        this.includeAndExcludeFilters = includeAndExcludeFilters;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean accept(Class testClass) {
        LauncherDiscoveryRequest discoveryRequest = request()
                .selectors(selectClass(testClass.getName()))
                .filters(includeAndExcludeFilters)
                .build();

        TestPlan testPlan;
        try {
            testPlan = launcher.discover(discoveryRequest);
        } catch (JUnitException e) {
            Throwable rootCause = e;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            if (rootCause == e || rootCause.getMessage() == null) {
                throw e;
            }
            String message = e.getMessage() == null
                    ? rootCause.getMessage()
                    : e.getMessage() + ": " + rootCause.getMessage();
            throw new JUnitException(message, e);
        }

        return testPlan.containsTests();
    }
}
