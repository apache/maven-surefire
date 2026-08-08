<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Apache Maven Surefire

Surefire is the test framework behind Maven's `test` and `verify` phases. It runs your unit
and integration tests, collects the results, and turns them into reports — whether you use
JUnit or TestNG.

The project ships three Maven plugins:

- **[Maven Surefire Plugin](maven-surefire-plugin/)** — runs your unit tests during the `test` phase.
- **[Maven Failsafe Plugin](maven-failsafe-plugin/)** — runs your integration tests during the `integration-test` and `verify` phases.
- **[Maven Surefire Report Plugin](maven-surefire-report-plugin/)** — turns test results into readable HTML reports.

New here? Start with the [Surefire Plugin guide](maven-surefire-plugin/), or grab the latest
version from the [download page](download.html).

## What's New

### In development (3.6.0-M2-SNAPSHOT)

- Your tests can now run on [JUnit 6](https://github.com/apache/maven-surefire/pull/3370).
- Relative classpath entries are [resolved correctly](https://github.com/apache/maven-surefire/pull/3333)
  against the forked JVM's working directory.

### 3.6.0-M1

The big one: every test framework now runs through a single, unified
[JUnit Platform provider](maven-surefire-plugin/whats-new-3-6-0.html). JUnit 5 runs natively,
and JUnit 4 and TestNG run through it too — giving you more consistent behaviour and far less
to configure. JUnit 5 tests can also run in parallel, and the JUnit 5 XML reports are tidier.

If you're upgrading, note that 3.6.0-M1 raises the minimum versions (JUnit 4.12+, TestNG
6.14.3+) and drops the old per-framework providers. The
[What's New in 3.6.0](maven-surefire-plugin/whats-new-3-6-0.html) guide walks you through it,
and the [3.6.0-M1 release notes](https://github.com/apache/maven-surefire/releases/tag/surefire-3.6.0-M1)
list everything.

### 3.5.6

- Test reports can now flag flaky tests and include timestamps for test sets and test cases.
- Debugging Failsafe tests with `maven.surefire.debug` shows the
  *"Listening for transport dt_socket…"* message [on time again](https://github.com/apache/maven-surefire/issues/2613).

See the [3.5.6 release notes](https://github.com/apache/maven-surefire/releases/tag/surefire-3.5.6)
for the full list.
