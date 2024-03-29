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
package org.apache.maven.surefire.testng;

import java.util.Map;

import org.apache.maven.surefire.api.report.ReporterException;
import org.apache.maven.surefire.api.report.RunListener;
import org.apache.maven.surefire.api.report.SimpleReportEntry;

import static org.apache.maven.surefire.api.report.RunMode.NORMAL_RUN;
import static org.apache.maven.surefire.api.util.internal.ObjectUtils.systemProps;

/**
 * Abstract class which implements common functions.
 */
abstract class TestSuite {
    abstract Map<String, String> getOptions();

    private String getSuiteName() {
        String result = getOptions().get("suitename");
        return result == null ? "TestSuite" : result;
    }

    final void startTestSuite(RunListener reporterManager) {
        try {
            reporterManager.testSetStarting(new SimpleReportEntry(NORMAL_RUN, 0L, getSuiteName(), null, null, null));
        } catch (ReporterException e) {
            // TODO: remove this exception from the report manager
        }
    }

    final void finishTestSuite(RunListener reporterManager) {
        reporterManager.testSetCompleted(
                new SimpleReportEntry(NORMAL_RUN, 0L, getSuiteName(), null, null, null, systemProps()));
    }
}
