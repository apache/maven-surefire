package org.apache.maven.surefire.api.report;

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
 * Minimum data requirement for report entry.
 * <br>
 * Additionally, we should distinguish between two events ({@link OutputReportEntry}, {@link TestOutputReportEntry}).
 * The interface {@link TestReportListener} handles these two events via generics depending on the situation.
 * <br>
 * The first situation happens when provider's listeners handles the event {@link OutputReportEntry} from
 * <i>System.out</i> and <i>System.err</i> via the {@link ConsoleOutputCapture}. The {@link ConsoleOutputCapture} does
 * not have any notion about {@link RunMode} and <code>testRunId</code>, and therefore the only provider's listener
 * would add {@link RunMode} and <code>testRunId</code> to a recreated entry which would be finally propagated to the
 * <code>ForkingRunListener</code> and <code>TestSetRunListener</code>. The {@link RunMode} and <code>testRunId</code>
 * are determined upon the events test-started, test-finished and Thread local.
 * <br>
 * The second situation happens when <code>ForkingRunListener</code> and <code>TestSetRunListener</code> handles
 * {@link TestOutputReportEntry} which contains {@link RunMode} and <code>testRunId</code>.
 */
public interface OutputReportEntry
{
    String getLog();

    boolean isStdOut();

    boolean isNewLine();
}
