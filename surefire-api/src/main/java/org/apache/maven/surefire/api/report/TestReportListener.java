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

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;

/**
 * This architecture has two sides (forked JVM, plugin JVM) implementing the same interface {@link TestReportListener}:
 * <pre>
 * 1. <b>publisher</b> - surefire fork JVM: {@link org.apache.maven.surefire.api.booter.ForkingRunListener}
 *    registered in {@link  org.apache.maven.surefire.api.provider.SurefireProvider}
 * 2. <b>consumer</b> - plugin JVM: {@code TestSetRunListener} registered in the {@code ForkClient}
 * </pre>
 * Both implementations of {@link TestReportListener}, i.e.
 * {@link org.apache.maven.surefire.api.booter.ForkingRunListener} and {@code TestSetRunListener}
 * are decorators. They are used as delegators in interface adapters, see the implementations of
 * JUnit's {@code RunListener RunListener-s}.
 * <br>
 * The serialization of data in {@link TestReportListener} ensures that the ReportEntries are transferred
 * from the fork to the plugin.
 * <br>
 * Note: The adapters in the module <i>surefire-junit47</i> are temporal and will be removed after we have fixed
 * the SUREFIRE-1860 and XML reporter in SUREFIRE-1643. The adapters are a workaround of a real fix in both Jira issues.
 *
 * @param <T> usually {@link TestOutputReportEntry} or {@link OutputReportEntry}
 */
public interface TestReportListener<T extends OutputReportEntry>
    extends RunListener, TestOutputReceiver<T>, ConsoleLogger
{
}
