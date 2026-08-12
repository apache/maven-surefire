---
title: Frequently Asked Questions
---

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

<a id="top"></a>

# Frequently Asked Questions

1. [What is the difference between maven-failsafe-plugin and maven-surefire-plugin?](#surefire-v-failsafe)
2. [How can I reuse my test code in other modules?](#reuse-test-code)
3. [Surefire fails with the message &quot;The forked VM terminated without properly saying goodbye&quot;.](#vm-termination)
4. [Crashed Surefire or Failsafe plugin must indicate crashed tests](#crashed-forks)
5. [How can I run GWT tests?](#GWT)
6. [How do I use properties set by other plugins in `argLine`?](#late-property-evaluation)
7. [How maven-failsafe-plugin allows me to configure the jar file or classes to use?](#failsafe-jar)
8. [How to dump fatal errors and stack trace of plugin runtime if it fails?](#dumpfiles)
9. [Corrupted channel by directly writing to native stream in forked JVM](#corruptedstream)
10. [The files cannot be deleted when Jenkins CI killed Maven process and the tests still continue running.](#kill-jvm)

<a id="surefire-v-failsafe"></a>

### What is the difference between maven-failsafe-plugin and maven-surefire-plugin?

[maven-surefire-plugin](http://maven.apache.org/plugins/maven-surefire-plugin/) is designed for running unit
tests and if any of the tests fail then it will fail the build immediately.

[maven-failsafe-plugin](http://maven.apache.org/plugins/maven-failsafe-plugin/) is designed for running
integration tests, and decouples failing the build if there are test failures from actually running the tests.

<a id="reuse-test-code"></a>

### How can I reuse my test code in other modules?

Visit this link for your reference,
[Attaching tests](http://maven.apache.org/guides/mini/guide-attached-tests.html). Also see the examples for
[Inclusions and Exclusions of Tests](examples/inclusion-exclusion.html).

<a id="vm-termination"></a>

### Surefire fails with the message &quot;The forked VM terminated without properly saying goodbye&quot;.

Surefire does not support tests or any referenced libraries calling `System.exit()` at any time. If they do so,
they are incompatible with Surefire and you should probably file an issue with the library/vendor.

Alternatively the forked VM could also have crashed for a number of reasons. Look for the classical
`hs_err*` files indicating VM crashes or examine the Maven log output when the tests execute. Some
&quot;extraordinary&quot; output from crashing processes may be dumped to the console/log.

If this happens on a CI environment and only after it runs for some time, there is a fair chance your test
suite is leaking some kind of OS-level resource that makes things worse at every run. Regular OS-level
monitoring tools may give you some indication.

<a id="crashed-forks"></a>

### Crashed Surefire or Failsafe plugin must indicate crashed tests

After a forked JVM has crashed the console of forked JVM prints *Crashed tests:* and lists the last test which
has crashed. In the console log you can find the message *The forked VM terminated without properly saying
goodbye*.

<a id="GWT"></a>

### How can I run GWT tests?

Mojohaus publishes a [gwt-maven-plugin](https://gwt-maven-plugin.github.io/gwt-maven-plugin/), but if you want
to run with Surefire, you need the following settings:

```xml
<useSystemClassLoader>true</useSystemClassLoader>
<useManifestOnlyJar>false</useManifestOnlyJar>
<forkCount>1</forkCount>
```

Try `reuseForks=true` and if it doesn't work, fall back to `reuseForks=false`

<a id="late-property-evaluation"></a>

### How do I use properties set by other plugins in `argLine`?

Maven does property replacement for `${...}` values in pom.xml before any plugin is run. So Surefire would
never see the place-holders in its argLine property.

Using an alternate syntax for these properties, `@{...}` allows late replacement of properties when the plugin
is executed, so properties that have been modified by other plugins will be picked up correctly. In contrast to
the standard Maven property replacement the `@{...}` placeholder is replaced by the empty string if the
referenced property cannot be found. *This mechanism is only necessary for properties which are modified, i.e.
also available with a different value at initialization of the POM model. Otherwise standard Maven property
replacement kicks in properly directly before this goal is being executed (instead of when the POM model is
being resolved)*.

<a id="failsafe-jar"></a>

### How maven-failsafe-plugin allows me to configure the jar file or classes to use?

By default maven-failsafe-plugin uses project artifact file in test classpath if packaging is set to &quot;jar&quot; in
pom.xml. This can be modified and for instance set to main project classes if you use configuration parameter
&quot;classesDirectory&quot;. This would mean that you set value &quot;${project.build.outputDirectory}&quot; for the parameter
&quot;classesDirectory&quot; in the configuration of plugin.

<a id="dumpfiles"></a>

### How to dump fatal errors and stack trace of plugin runtime if it fails?

By default *maven-failsafe-plugin* and *maven-surefire-plugin* dumps fatal errors in dump files and these are
located in *target/failsafe-reports* and *target/surefire-reports*. Names of dump files are formatted as
follows:

```
[date]-jvmRun[N].dump
[date]-jvmRun[N].dumpstream
[date].dumpstream
[date].dump
```

Forked JVM process and plugin process communicate via std/out. If this channel is corrupted, for a whatever
reason, the dump of the corrupted stream appears in *\*.dumpstream*.

<a id="corruptedstream"></a>

### Corrupted channel by directly writing to native stream in forked JVM

If your tests use native library which prints to STDOUT this warning message appears because the library
corrupted the channel used by the plugin in order to transmit events with test status back to Maven process.
It would be even worse if you override the Java stream by *System.setOut* because the stream is also supposed
to be corrupted but the Maven will never see the tests finished and build may hang.

This warning message appears if you use *FileDescriptor.out* or JVM prints GC summary.

In that case the warning is printed *&quot;Corrupted channel by directly writing to native stream in forked JVM&quot;*,
and a dump file can be found in Reports directory.

If debug level is enabled then messages of corrupted stream appear in the console.

<a id="kill-jvm"></a>

### The files cannot be deleted when Jenkins CI killed Maven process and the tests still continue running.

Surefire and Failsafe plugin may kill forked Surefire JVM when the standard-input stream is closed. This works
when you stop Maven process by CTRL+C but it is not guaranteed on all platforms. By default, the plugins use
process pipes for interprocess communication. Since version 3.0.0-M5, TCP/IP sockets are also available and can
be selected with the forkNode configuration parameter. See the documentation of the configuration parameter
&quot;enableProcessChecker&quot; for mechanisms to detect and kill orphan forked JVMs. These mechanisms have some
drawbacks regarding your OS systems and GC, therefore see the documentation for the parameter
&quot;enableProcessChecker&quot;.
