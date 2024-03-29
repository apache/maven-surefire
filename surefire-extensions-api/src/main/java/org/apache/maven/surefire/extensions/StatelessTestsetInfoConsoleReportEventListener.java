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
package org.apache.maven.surefire.extensions;

import java.util.List;

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.api.report.TestSetReportEntry;

/**
 * Extension listener for stateless console reporter of test-set.
 * Signatures can be changed between major, minor versions or milestones.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 * @param <R> report entry type, see <em>WrappedReportEntry</em> from module the <em>maven-surefire-common</em>
 * @param <S> test-set statistics, see <em>TestSetStats</em> from module the <em>maven-surefire-common</em>
 */
public abstract class StatelessTestsetInfoConsoleReportEventListener<R extends TestSetReportEntry, S> {
    private final ConsoleLogger logger;

    public StatelessTestsetInfoConsoleReportEventListener(ConsoleLogger logger) {
        this.logger = logger;
    }

    public abstract void testSetStarting(TestSetReportEntry report);

    public abstract void testSetCompleted(R report, S testSetStats, List<String> testResults);

    public abstract void reset();

    public ConsoleLogger getConsoleLogger() {
        return logger;
    }
}
