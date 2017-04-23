package org.apache.maven.surefire.report;

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

/**
 * Describes a single entry for a test report
 *
 */
public interface ReportEntry
{
    /**
     * The class name of the test
     *
     * @return A string with the class name
     */
    String getSourceName();

    /**
     * The name of the test case
     *
     * @return A string describing the test case
     */
    String getName();

    /**
     * The group/category of the testcase
     *
     * @return A string
     */
    String getGroup();

    /**
     * The group/category of the testcase
     *
     * @return A string
     */
    StackTraceWriter getStackTraceWriter();

    /**
     * Gets the runtime for the item. Optional parameter. If the value is not set, it will be determined within
     * the reporting subsystem. Some providers like to calculate this value themselves, and it gets the
     * most accurate value.
     * @return duration of a test in milli seconds
     */
    Integer getElapsed();


    /**
     * A message relating to a non-successful termination.
     * May be the "message" from an exception or the reason for a test being ignored
     *
     * @return A string that explains an anomaly
     */
    String getMessage();

    /**
     * A name of the test case together with the group or category (if any exists).
     *
     * @return A string with the test case name and group/category, or just the name.
     */
    String getNameWithGroup();
}
